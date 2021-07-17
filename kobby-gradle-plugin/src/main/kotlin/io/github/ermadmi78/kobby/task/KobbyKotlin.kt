package io.github.ermadmi78.kobby.task

import io.github.ermadmi78.kobby.generator.kotlin.*
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.PREDEFINED_SCALARS
import io.github.ermadmi78.kobby.model.Decoration
import io.github.ermadmi78.kobby.model.KobbyDirective
import io.github.ermadmi78.kobby.model.PluginUtils.contextName
import io.github.ermadmi78.kobby.model.PluginUtils.extractCommonPrefix
import io.github.ermadmi78.kobby.model.PluginUtils.forEachPackage
import io.github.ermadmi78.kobby.model.PluginUtils.pathIterator
import io.github.ermadmi78.kobby.model.PluginUtils.removePrefixOrEmpty
import io.github.ermadmi78.kobby.model.PluginUtils.toPackageName
import io.github.ermadmi78.kobby.model.parseSchema
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import java.io.File
import java.io.FileReader

/**
 * Created on 02.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
open class KobbyKotlin : DefaultTask() {
    companion object {
        const val TASK_NAME = "kobbyKotlin"
    }

    @InputFiles
    val schemaFiles: ListProperty<RegularFile> = project.objects.listProperty(RegularFile::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaScanDir",
        description = "path to directory relative to project basedir, " +
                "where to look for schema files (default \"src/main/resources\")"
    )
    val schemaScanDir: Property<String> = project.objects.property(String::class.java)

    /**
     * ANT style include patterns to look for schema files (default `**`/`*`.graphqls)
     */
    @Input
    @Optional
    val schemaScanIncludes: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * ANT style exclude patterns to look for schema files (default empty)
     */
    @Input
    @Optional
    val schemaScanExcludes: ListProperty<String> = project.objects.listProperty(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectivePrimaryKey",
        description = "name of directive \"primaryKey\" (default \"primaryKey\")"
    )
    val schemaDirectivePrimaryKey: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveRequired",
        description = "name of directive \"required\" (default \"required\")"
    )
    val schemaDirectiveRequired: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveDefault",
        description = "name of directive \"default\" (default \"default\")"
    )
    val schemaDirectiveDefault: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveSelection",
        description = "name of directive \"selection\" (default \"selection\")"
    )
    val schemaDirectiveSelection: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveResolve",
        description = "name of directive \"resolve\" (default \"resolve\")"
    )
    val schemaDirectiveResolve: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    val scalars: MapProperty<String, KotlinType> =
        project.objects.mapProperty(String::class.java, KotlinType::class.java)


    @Input
    @Optional
    @Option(
        option = "relativePackage",
        description = "generate root package name relative to schema package name (default true)"
    )
    val relativePackage: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "rootPackageName",
        description = "root package name relative to schema package name (if relativePackage option is true) " +
                "for generated classes (default \"kobby.kotlin\")"
    )
    val rootPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextPackageName",
        description = "package name relative to root package name for generated context classes (default null)"
    )
    val contextPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextName",
        description = "name of context (default \"<GraphQL schema name>\")"
    )
    val contextName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextPrefix",
        description = "prefix for generated context classes (default \"<Context name>\")"
    )
    val contextPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextPostfix",
        description = "postfix for generated context classes (default null)"
    )
    val contextPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextQuery",
        description = "name of context query function (default \"query\")"
    )
    val contextQuery: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextMutation",
        description = "name of context mutation function (default \"mutation\")"
    )
    val contextMutation: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextSubscription",
        description = "name of context subscription function (default \"subscription\")"
    )
    val contextSubscription: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoPackageName",
        description = "package name relative to root package name for generated DTO classes (default \"dto\")"
    )
    val dtoPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoPrefix",
        description = "prefix for generated DTO classes (default null)"
    )
    val dtoPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoPostfix",
        description = "postfix for generated DTO classes (default \"Dto\")"
    )
    val dtoPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoEnumPrefix",
        description = "prefix for generated DTO enums (default null)"
    )
    val dtoEnumPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoEnumPostfix",
        description = "postfix for generated DTO enums (default null)"
    )
    val dtoEnumPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoInputPrefix",
        description = "prefix for generated DTO input objects (default null)"
    )
    val dtoInputPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoInputPostfix",
        description = "postfix for generated DTO input objects (default null)"
    )
    val dtoInputPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoApplyPrimaryKeys",
        description = "Generate equals and hashCode for DTO classes by @primaryKey directive (default false)"
    )
    val dtoApplyPrimaryKeys: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoJacksonEnabled",
        description = "add Jackson annotations for generated DTO classes (default true)"
    )
    val dtoJacksonEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderEnabled",
        description = "generate DTO builders is enabled (default true)"
    )
    val dtoBuilderEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderPrefix",
        description = "prefix for generated DTO Builder classes (default null)"
    )
    val dtoBuilderPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderPostfix",
        description = "postfix for generated DTO Builder classes (default \"Dto\")"
    )
    val dtoBuilderPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderCopyFun",
        description = "name of copy function for DTO classes (default \"copy\")"
    )
    val dtoBuilderCopyFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLEnabled",
        description = "generate GraphQL DTO classes (default true)"
    )
    val dtoGraphQLEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLPackageName",
        description = "package name for GraphQL DTO classes relative to DTO package name (default \"graphql\")"
    )
    val dtoGraphQLPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLPrefix",
        description = "prefix for generated GraphQL DTO classes (default \"<Context name>\")"
    )
    val dtoGraphQLPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLPostfix",
        description = "postfix for generated GraphQL DTO classes (default null)"
    )
    val dtoGraphQLPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityEnabled",
        description = "generate Entity classes (default true)"
    )
    val entityEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "entityPackageName",
        description = "package name relative to root package name for generated Entity classes (default \"entity\")"
    )
    val entityPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityPrefix",
        description = "prefix for generated entity classes (default null)"
    )
    val entityPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityPostfix",
        description = "postfix for generated entity classes (default null)"
    )
    val entityPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithCurrentProjectionFun",
        description = "name of entity 'withCurrentProjection' function (default \"__withCurrentProjection\")"
    )
    val entityWithCurrentProjectionFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityProjectionPrefix",
        description = "prefix for generated projection classes (default null)"
    )
    val entityProjectionPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityProjectionPostfix",
        description = "postfix for generated projection classes (default \"Projection\")"
    )
    val entityProjectionPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityProjectionArgument",
        description = "name of projection lambda argument (default \"__projection\")"
    )
    val entityProjectionArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithPrefix",
        description = "prefix of projection 'with' method (default null)"
    )
    val entityWithPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithPostfix",
        description = "postfix of projection 'with' method (default null)"
    )
    val entityWithPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithoutPrefix",
        description = "prefix of projection 'with' method (default \"__without\")"
    )
    val entityWithoutPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithoutPostfix",
        description = "postfix of projection 'with' method (default null)"
    )
    val entityWithoutPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityMinimizeFun",
        description = "name of projection 'minimize' function (default \"__minimize\")"
    )
    val entityMinimizeFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualificationPrefix",
        description = "prefix for generated qualification classes (default null)"
    )
    val entityQualificationPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualificationPostfix",
        description = "postfix for generated qualification classes (default \"Qualification\")"
    )
    val entityQualificationPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualifiedProjectionPrefix",
        description = "prefix for generated qualification classes (default null)"
    )
    val entityQualifiedProjectionPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualifiedProjectionPostfix",
        description = "postfix for generated qualification classes (default \"QualifiedProjection\")"
    )
    val entityQualifiedProjectionPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityOnPrefix",
        description = "prefix of qualification 'on' method (default \"__on\")"
    )
    val entityOnPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityOnPostfix",
        description = "postfix of qualification 'on' method (default null)"
    )
    val entityOnPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entitySelectionPrefix",
        description = "prefix for generated selection classes (default null)"
    )
    val entitySelectionPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entitySelectionPostfix",
        description = "postfix for generated selection classes (default \"Selection\")"
    )
    val entitySelectionPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entitySelectionArgument",
        description = "name of selection lambda argument (default \"__selection\")"
    )
    val entitySelectionArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQueryPrefix",
        description = "prefix for generated query classes (default null)"
    )
    val entityQueryPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQueryPostfix",
        description = "postfix for generated query classes (default \"Query\")"
    )
    val entityQueryPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQueryArgument",
        description = "name of query lambda argument (default \"__query\")"
    )
    val entityQueryArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPackageName",
        description = "package name relative to root package name " +
                "for generated implementation classes (default \"impl\")"
    )
    val implPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPrefix",
        description = "prefix for generated implementation classes (default null)"
    )
    val implPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPostfix",
        description = "postfix for generated implementation classes (default \"Impl\")"
    )
    val implPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implInternal",
        description = "make generated implementation classes internal (default true)"
    )
    val implInternal: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "implInnerPrefix",
        description = "prefix for generated implementation service properties (default \"__inner\")"
    )
    val implInnerPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implInnerPostfix",
        description = "postfix for generated implementation service properties (default \"Impl\")"
    )
    val implInnerPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorSimpleEnabled",
        description = "generate default SimpleKtorAdapter (default false)"
    )
    val adapterKtorSimpleEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorCompositeEnabled",
        description = "generate default CompositeKtorAdapter (default false)"
    )
    val adapterKtorCompositeEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorPackageName",
        description = "package name relative to root package name " +
                "for generated Ktor adapters (default \"adapter.ktor\")"
    )
    val adapterKtorPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorPrefix",
        description = "prefix for generated Ktor adapters (default \"<Context name>\")"
    )
    val adapterKtorPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "adapterKtorPostfix",
        description = "postfix for generated Ktor adapters (default \"KtorAdapter\")"
    )
    val adapterKtorPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "resolverEnabled",
        description = "generate graphql-java-kickstart resolvers (default false)"
    )
    val resolverEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "resolverPublisherEnabled",
        description = "generate publishers for subscription resolvers (default false)"
    )
    val resolverPublisherEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "resolverPackageName",
        description = "package name relative to root package name " +
                "for generated graphql-java-kickstart resolvers (default \"resolver\")"
    )
    val resolverPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "resolverPrefix",
        description = "prefix for generated graphql-java-kickstart resolvers (default \"<Context name>\")"
    )
    val resolverPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "resolverPostfix",
        description = "postfix for generated graphql-java-kickstart resolvers (default \"Resolver\")"
    )
    val resolverPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "resolverArgument",
        description = "name of resolver bean argument. null - generate argument name from bean name (default null)"
    )
    val resolverArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "resolverToDoMessage",
        description = "error message for generated graphql-java-kickstart resolvers (default null)"
    )
    val resolverToDoMessage: Property<String> = project.objects.property(String::class.java)

    @OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    init {
        group = "kobby"
        description = "Generate Kotlin DSL client by GraphQL schema"

        schemaFiles.convention(project.provider<Iterable<RegularFile>> {
            project.fileTree(schemaScanDir.get()) {
                it.include(schemaScanIncludes.get())
                it.exclude(schemaScanExcludes.get())
            }.filter { it.isFile }.files.map {
                project.layout.file(project.provider { it }).get()
            }
        })

        schemaScanDir.convention("src/main/resources")
        schemaScanIncludes.convention(listOf("**/*.graphqls"))
        schemaScanExcludes.convention(listOf())

        schemaDirectivePrimaryKey.convention(KobbyDirective.PRIMARY_KEY)
        schemaDirectiveRequired.convention(KobbyDirective.REQUIRED)
        schemaDirectiveDefault.convention(KobbyDirective.DEFAULT)
        schemaDirectiveSelection.convention(KobbyDirective.SELECTION)
        schemaDirectiveResolve.convention(KobbyDirective.RESOLVE)

        scalars.convention(PREDEFINED_SCALARS)

        relativePackage.convention(true)
        rootPackageName.convention("kobby.kotlin")

        contextQuery.convention("query")
        contextMutation.convention("mutation")
        contextSubscription.convention("subscription")

        dtoPackageName.convention("dto")
        dtoPostfix.convention("Dto")
        dtoApplyPrimaryKeys.convention(false)
        dtoJacksonEnabled.convention(project.provider {
            project.hasDependency("com.fasterxml.jackson.core", "jackson-annotations")
        })
        dtoBuilderEnabled.convention(true)
        dtoBuilderPostfix.convention("Builder")
        dtoBuilderCopyFun.convention("copy")
        dtoGraphQLEnabled.convention(true)
        dtoGraphQLPackageName.convention("graphql")

        entityEnabled.convention(true)
        entityPackageName.convention("entity")
        entityWithCurrentProjectionFun.convention("__withCurrentProjection")
        entityProjectionPostfix.convention("Projection")
        entityProjectionArgument.convention("__projection")
        entityWithoutPrefix.convention("__without")
        entityMinimizeFun.convention("__minimize")
        entityQualificationPostfix.convention("Qualification")
        entityQualifiedProjectionPostfix.convention("QualifiedProjection")
        entityOnPrefix.convention("__on")
        entitySelectionPostfix.convention("Selection")
        entitySelectionArgument.convention("__selection")
        entityQueryPostfix.convention("Query")
        entityQueryArgument.convention("__query")

        implPackageName.convention("entity.impl")
        implPostfix.convention("Impl")
        implInternal.convention(true)
        implInnerPrefix.convention("__inner")

        adapterKtorSimpleEnabled.convention(project.provider {
            project.hasDependency("io.ktor", "ktor-client-cio")
        })
        adapterKtorCompositeEnabled.convention(project.provider {
            project.hasDependency("io.ktor", "ktor-client-cio")
        })
        adapterKtorPackageName.convention("adapter.ktor")
        adapterKtorPostfix.convention("KtorAdapter")

        resolverEnabled.convention(project.provider {
            project.hasDependency("com.graphql-java-kickstart", "graphql-java-tools")
        })
        resolverPublisherEnabled.convention(project.provider {
            project.hasDependency("org.reactivestreams", "reactive-streams")
        })
        resolverPackageName.convention("resolver")
        resolverPostfix.convention("Resolver")

        outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/kobby/main/kotlin"))
    }

    @TaskAction
    fun generateKotlinDslClientAction() {
        val graphQLSchemaFiles: List<File> = schemaFiles.get().map {
            it.asFile.absoluteFile.also { file ->
                if (!file.isFile) {
                    "Specified schema file does not exist: $it".throwIt()
                }
            }
        }

        if (graphQLSchemaFiles.isEmpty()) {
            "GraphQL schema files not found".throwIt()
        }

        val directiveLayout = mapOf(
            KobbyDirective.PRIMARY_KEY to schemaDirectivePrimaryKey.get(),
            KobbyDirective.REQUIRED to schemaDirectiveRequired.get(),
            KobbyDirective.DEFAULT to schemaDirectiveDefault.get(),
            KobbyDirective.SELECTION to schemaDirectiveSelection.get(),
            KobbyDirective.RESOLVE to schemaDirectiveResolve.get()
        )

        val contextName = (contextName.orNull ?: graphQLSchemaFiles.singleOrNull()?.contextName)
            ?.filter { it.isJavaIdentifierPart() }
            ?.takeIf { it.firstOrNull()?.isJavaIdentifierStart() ?: false }
            ?: "graphql"
        val capitalizedContextName = contextName.capitalize()

        val rootPackage: List<String> = mutableListOf<String>().also { list ->
            if (relativePackage.get()) {
                graphQLSchemaFiles
                    .map { it.parent.pathIterator() }
                    .extractCommonPrefix()
                    .removePrefixOrEmpty(project.file(schemaScanDir.get()).absoluteFile.path.pathIterator())
                    .forEach {
                        list += it
                    }
            }
            rootPackageName.orNull?.forEachPackage { list += it }
        }

        val contextPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            contextPackageName.orNull?.forEachPackage { list += it }
        }

        val dtoPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            dtoPackageName.orNull?.forEachPackage { list += it }
        }

        val dtoGraphQLPackage: List<String> = mutableListOf<String>().also { list ->
            list += dtoPackage
            dtoGraphQLPackageName.orNull?.forEachPackage { list += it }
        }

        val entityPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            entityPackageName.orNull?.forEachPackage { list += it }
        }

        val implPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            implPackageName.orNull?.forEachPackage { list += it }
        }

        val adapterKtorPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            adapterKtorPackageName.orNull?.forEachPackage { list += it }
        }

        val resolverPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            resolverPackageName.orNull?.forEachPackage { list += it }
        }

        val layout = KotlinLayout(
            scalars.get(),
            KotlinContextLayout(
                contextPackage.toPackageName(),
                contextName,
                Decoration(contextPrefix.orNull ?: capitalizedContextName, contextPostfix.orNull),
                contextQuery.get(),
                contextMutation.get(),
                contextSubscription.get()
            ),
            KotlinDtoLayout(
                dtoPackage.toPackageName(),
                Decoration(dtoPrefix.orNull, dtoPostfix.orNull),
                Decoration(dtoEnumPrefix.orNull, dtoEnumPostfix.orNull),
                Decoration(dtoInputPrefix.orNull, dtoInputPostfix.orNull),
                dtoApplyPrimaryKeys.get(),
                KotlinDtoJacksonLayout(dtoJacksonEnabled.get()),
                KotlinDtoBuilderLayout(
                    dtoBuilderEnabled.get(),
                    Decoration(dtoBuilderPrefix.orNull, dtoBuilderPostfix.orNull),
                    dtoBuilderCopyFun.get()
                ),
                KotlinDtoGraphQLLayout(
                    dtoGraphQLEnabled.get(),
                    dtoGraphQLPackage.toPackageName(),
                    Decoration(
                        dtoGraphQLPrefix.orNull?.trim() ?: capitalizedContextName,
                        dtoGraphQLPostfix.orNull
                    )
                )
            ),
            KotlinEntityLayout(
                entityEnabled.get(),
                entityPackage.toPackageName(),
                Decoration(entityPrefix.orNull, entityPostfix.orNull),
                entityWithCurrentProjectionFun.get(),
                KotlinEntityProjectionLayout(
                    Decoration(entityProjectionPrefix.orNull, entityProjectionPostfix.orNull),
                    entityProjectionArgument.get(),
                    Decoration(entityWithPrefix.orNull, entityWithPostfix.orNull),
                    Decoration(entityWithoutPrefix.orNull, entityWithoutPostfix.orNull),
                    entityMinimizeFun.get(),
                    Decoration(entityQualificationPrefix.orNull, entityQualificationPostfix.orNull),
                    Decoration(entityQualifiedProjectionPrefix.orNull, entityQualifiedProjectionPostfix.orNull),
                    Decoration(entityOnPrefix.orNull, entityOnPostfix.orNull)
                ),
                KotlinEntitySelectionLayout(
                    Decoration(entitySelectionPrefix.orNull, entitySelectionPostfix.orNull),
                    entitySelectionArgument.get(),
                    Decoration(entityQueryPrefix.orNull, entityQueryPostfix.orNull),
                    entityQueryArgument.get()
                )
            ),
            KotlinImplLayout(
                implPackage.toPackageName(),
                Decoration(implPrefix.orNull, implPostfix.orNull),
                implInternal.get(),
                Decoration(implInnerPrefix.orNull, implInnerPostfix.orNull)
            ),
            KotlinAdapterLayout(
                KotlinAdapterKtorLayout(
                    adapterKtorSimpleEnabled.get(),
                    adapterKtorCompositeEnabled.get(),
                    adapterKtorPackage.toPackageName(),
                    Decoration(
                        adapterKtorPrefix.orNull?.trim() ?: capitalizedContextName,
                        adapterKtorPostfix.orNull
                    )
                )
            ),
            KotlinResolverLayout(
                resolverEnabled.get(),
                resolverPublisherEnabled.get(),
                resolverPackage.toPackageName(),
                Decoration(
                    (resolverPrefix.orNull?.trim() ?: capitalizedContextName).let {
                        if (it == "GraphQL") "IGraphQL" else it
                    }, resolverPostfix.orNull
                ),
                resolverArgument.orNull,
                resolverToDoMessage.orNull
            )
        )

        val targetDirectory = outputDirectory.get().asFile
        if (!targetDirectory.isDirectory && !targetDirectory.mkdirs()) {
            "Failed to create directory for generated sources: $targetDirectory".throwIt()
        }

        val schema = try {
            parseSchema(directiveLayout, *graphQLSchemaFiles.map { FileReader(it) }.toTypedArray())
        } catch (e: Exception) {
            "Schema parsing failed.".throwIt(e)
        }

        val output = try {
            generateKotlin(schema, layout)
        } catch (e: Exception) {
            "Kotlin DSL generation failed.".throwIt(e)
        }

        output.forEach {
            it.writeTo(targetDirectory)
        }
    }

    private fun String.throwIt(cause: Throwable? = null): Nothing {
        val message = "[kobby] $this${if (cause == null) "" else " " + cause.message}"
        System.err.println(message)
        if (cause == null) {
            throw TaskInstantiationException(message)
        } else {
            throw TaskInstantiationException(message, cause)
        }
    }

    private fun Project.resolveDependencies(): Sequence<ResolvedDependency> = this.configurations.asSequence()
        .filter { it.isCanBeResolved }
        .flatMap { it.resolvedConfiguration.firstLevelModuleDependencies }
        .flatMap {
            sequence {
                yield(it)
                yieldAll(it.children)
            }
        }

    private fun Project.hasDependency(moduleGroup: String, moduleName: String): Boolean =
        this.resolveDependencies().any {
            it.moduleGroup == moduleGroup && it.moduleName == moduleName
        }
}