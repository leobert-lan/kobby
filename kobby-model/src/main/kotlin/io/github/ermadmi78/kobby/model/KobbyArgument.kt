package io.github.ermadmi78.kobby.model

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyArgument internal constructor(
    val schema: KobbySchema,
    val node: KobbyNode,
    val field: KobbyField,

    val name: String,
    val type: KobbyType,
    val hasDefaultValue: Boolean,
    private val _comments: List<String>
) {
    val comments: List<String> by lazy {
        if (_comments.isNotEmpty()) {
            _comments
        } else {
            field.overriddenField?.arguments?.get(name)?.comments ?: emptyList()
        }
    }

    fun comments(action: (String) -> Unit) = comments.forEach(action)

    val isInitialized: Boolean get() = type.nullable || hasDefaultValue

    val isSelection: Boolean get() = this.field.isSelection && isInitialized

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as KobbyArgument
        return field == other.field && name == other.name
    }

    override fun hashCode(): Int {
        var result = field.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "$name: $type"
    }
}

@KobbyScope
class KobbyArgumentScope internal constructor(
    schema: KobbySchema,
    node: KobbyNode,
    field: KobbyField,
    name: String,
    type: KobbyType,
    hasDefaultValue: Boolean
) {
    private val comments = mutableListOf<String>()
    private val argument = KobbyArgument(schema, node, field, name, type, hasDefaultValue, comments)

    fun addComment(comment: String) {
        comments += comment
    }

    fun build(): KobbyArgument = argument
}