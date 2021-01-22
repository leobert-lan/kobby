package io.kobby.model

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyNode internal constructor(
    val schema: KobbySchema,

    val name: String,
    val kind: KobbyNodeKind,
    private val _implements: List<String>,
    val comments: List<String>,
    val enumValues: Map<String, KobbyEnumValue>,
    val fields: Map<String, KobbyField>
) {
    val implements: Map<String, KobbyNode> by lazy {
        _implements.asSequence()
            .map { schema.interfaces[it] }
            .filterNotNull()
            .map { it.name to it }
            .toMap()
    }

    fun implements(action: (KobbyNode) -> Unit) = implements.values.forEach(action)
    fun comments(action: (String) -> Unit) = comments.forEach(action)
    fun enumValues(action: (KobbyEnumValue) -> Unit) = enumValues.values.forEach(action)
    fun fields(action: (KobbyField) -> Unit) = fields.values.forEach(action)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as KobbyNode
        return schema == other.schema && name == other.name
    }

    override fun hashCode(): Int {
        var result = schema.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "${kind.name.toLowerCase()} $name"
    }
}

enum class KobbyNodeKind {
    SCALAR,
    QUERY,
    MUTATION,
    OBJECT,
    INTERFACE,
    UNION,
    ENUM,
    INPUT
}

@KobbyScope
class KobbyNodeScope internal constructor(
    val schema: KobbySchema,
    name: String,
    kind: KobbyNodeKind
) {
    private val _implements = mutableListOf<String>()
    private val comments = mutableListOf<String>()
    private val enumValues = mutableMapOf<String, KobbyEnumValue>()
    private val fields = mutableMapOf<String, KobbyField>()
    private val node = KobbyNode(schema, name, kind, _implements, comments, enumValues, fields)

    fun addImplements(interfaceName: String) {
        _implements += interfaceName
    }

    fun addComment(comment: String) {
        comments += comment
    }

    fun addEnumValue(
        name: String,
        block: KobbyEnumValueScope.() -> Unit
    ) = KobbyEnumValueScope(schema, node, name).apply(block).build().also {
        enumValues[it.name] = it
    }

    fun addField(
        name: String,
        type: KobbyType,
        required: Boolean,
        default: Boolean,
        block: KobbyFieldScope.() -> Unit
    ) = KobbyFieldScope(schema, node, name, type, required, default).apply(block).build().also {
        fields[it.name] = it
    }

    fun build(): KobbyNode = node
}