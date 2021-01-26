package io.kobby.generator.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import io.kobby.model.KobbyNode
import io.kobby.model.KobbySchema

/**
 * Created on 24.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
internal fun generateEntity(schema: KobbySchema, layout: KotlinLayout): List<FileSpec> = with(layout) {
    val files = mutableListOf<FileSpec>()

    val buildEntity: FileSpecBuilder.(KobbyNode) -> Unit = { node ->
        buildInterface(node.entityName) {
            node.implements {
                addSuperinterface(it.entityClass)
            }
            node.comments {
                addKdoc(it)
            }
            node.fields { field ->
                buildProperty(field.name, field.type.entityType) {
                    if (field.isOverride()) {
                        addModifiers(KModifier.OVERRIDE)
                    }
                    field.comments {
                        addKdoc(it)
                    }
                }
            }
        }
    }

    val buildProjection: FileSpecBuilder.(KobbyNode) -> Unit = { node ->
        buildInterface(node.projectionName) {
            addAnnotation(context.dslClass)
            node.implements {
                addSuperinterface(it.projectionClass)
            }
            node.comments {
                addKdoc(it)
            }
            node.fields.values.asSequence().filter { !it.isRequired() }.forEach { field ->
                buildFunction(field.projectionFieldName) {
                    addModifiers(KModifier.ABSTRACT)
                    if (field.isOverride()) {
                        addModifiers(KModifier.OVERRIDE)
                    }
                    field.comments {
                        addKdoc(it)
                    }

                    val isSelection = field.isSelection()
                    field.arguments.values.asSequence()
                        .filter { !isSelection || !it.type.nullable }
                        .forEach { arg ->
                            buildParameter(arg.name, arg.type.dtoType) {
                                if (arg.type.nullable) {
                                    defaultValue("null")
                                }
                                arg.comments {
                                    addKdoc(it)
                                }
                            }
                        }
                    field.lambda?.also {
                        buildParameter(it) {
                            defaultValue("{}")
                        }
                    }
                }
            }
        }
    }

    val buildSelection: FileSpecBuilder.(KobbyNode) -> Unit = { node ->
        node.fields.values.asSequence().filter { it.isSelection() }.forEach { field ->
            buildInterface(field.selectionName) {
                addAnnotation(context.dslClass)
                field.comments {
                    addKdoc(it)
                }
                field.arguments.values.asSequence().filter { it.type.nullable }.forEach { arg ->
                    buildProperty(arg.name, arg.type.entityType) {
                        mutable()
                        arg.comments {
                            addKdoc(it)
                        }
                    }
                }
            }

            if (field.type.hasProjection) {
                buildInterface(field.queryName) {
                    addAnnotation(context.dslClass)
                    field.comments {
                        addKdoc(it)
                    }
                    addSuperinterface(field.selectionClass)
                    addSuperinterface(field.type.node.qualifiedProjectionClass)
                }
            }
        }
    }

    val buildQualification: FileSpecBuilder.(KobbyNode) -> Unit = { node ->
        buildInterface(node.qualificationName) {
            addAnnotation(context.dslClass)
            node.comments {
                addKdoc(it)
            }
            node.subObjects { subObject ->
                buildFunction(subObject.projectionOnName) {
                    addModifiers(KModifier.ABSTRACT)
                    subObject.comments {
                        addKdoc(it)
                    }
                    buildParameter(entity.projection.projectionArgument, subObject.projectionLambda) {
                        defaultValue("{}")
                    }
                }
            }
        }
    }

    val buildQualifiedProjection: FileSpecBuilder.(KobbyNode) -> Unit = { node ->
        buildInterface(node.qualifiedProjectionName) {
            addAnnotation(context.dslClass)
            node.comments {
                addKdoc(it)
            }
            addSuperinterface(node.projectionClass)
            addSuperinterface(node.qualificationClass)
        }
    }

    //******************************************************************************************************************
    //                                                Objects
    //******************************************************************************************************************
    schema.objects { node ->
        files += buildFile(entity.packageName, node.entityName) {
            buildEntity(node)
            buildProjection(node)
            buildSelection(node)
        }
    }

    //******************************************************************************************************************
    //                                                Interfaces
    //******************************************************************************************************************
    schema.interfaces { node ->
        files += buildFile(entity.packageName, node.entityName) {
            buildEntity(node)
            buildProjection(node)
            buildQualification(node)
            buildQualifiedProjection(node)
            buildSelection(node)
        }
    }

    //******************************************************************************************************************
    //                                                Unions
    //******************************************************************************************************************
    schema.unions { node ->
        files += buildFile(entity.packageName, node.entityName) {
            buildEntity(node)
            buildProjection(node)
            buildQualification(node)
            buildQualifiedProjection(node)
            buildSelection(node)
        }
    }

    files
}