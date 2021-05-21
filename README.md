# Overview

## Kobby is a codegen plugin of Kotlin DSL Client by GraphQL schema

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/simple_query.png)

The generated DSL supports execution of complex GraphQL queries and mutations in Kotlin with syntax similar to native
GraphQL syntax. Moreover, you can customize generated DSL by means of GraphQL schema directives and Kotlin extension
functions.

## Gradle

```kotlin
plugins {
    id("io.github.ermadmi78.kobby") version "1.0.0-beta.04"
}
```

## Maven

Maven plugin is under construction now

# License

[Apache License Version 2.0](https://github.com/ermadmi78/kobby/blob/main/LICENSE)

# Usage

Usage example see [here](https://github.com/ermadmi78/kobby-gradle-example)

## Define your GraphQL schema

Put your GraphQL schema in project resources with `graphqls` extension. For example, let define
[cinema.graphqls](https://github.com/ermadmi78/kobby-gradle-example/blob/gradle-tutorial/cinema-api/src/main/resources/io/github/ermadmi78/kobby/cinema/api/cinema.graphqls)
schema and put it in `resources/io/github/ermadmi78/kobby/cinema/api/`

## Configure Kobby Gradle plugin

* Add Kobby plugin to your `build.gradle.kts`, to generate Kotlin DSL.
* Configure Kotlin data types for scalars, defined in GraphQL schema (use `kobby` extension in `build.gradle.kts`).
* Add Jackson dependency to generate Jackson annotations for DTO classes.
* Add Kotlin plugin to your `build.gradle.kts`, to compile generated DSL.

```kotlin
import io.github.ermadmi78.kobby.kobby

description = "Cinema API Example"

plugins {
    kotlin("jvm")
    `java-library`
    id("io.github.ermadmi78.kobby")
}

kobby {
    kotlin {
        scalars = mapOf(
            "Date" to typeOf("java.time", "LocalDate"),
        )
    }
}

val kotlinJvmVersion: String by project
tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = kotlinJvmVersion
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
}

val jacksonVersion: String by project
dependencies {
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
}
```

## Generate Kotlin DSL from your GraphQL schema

Execute `gradle build` command to generate Kotlin DSL. Entry point of DSL will be placed in `cinema.kt` file
(name of DSL entry point file is same as name of GraphQL schema file):

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/cinema_api.png)

## Write DSL Adapter

In `cinema.kt` will be placed `cinemaContextOf` builder function, that creates `CinemaContext` - the entry point of
generated DSL. Note, that prefixes of builder function, adapter and context interfaces are same as name of GraphQL
schema file.

```kotlin
public fun cinemaContextOf(adapter: CinemaAdapter): CinemaContext = CinemaContextImpl(adapter)

public interface CinemaContext {
    public suspend fun query(__projection: QueryProjection.() -> Unit): Query

    public suspend fun mutation(__projection: MutationProjection.() -> Unit): Mutation
}

public interface CinemaAdapter {
    public suspend fun executeQuery(query: String, variables: Map<String, Any?>): QueryDto

    public suspend fun executeMutation(query: String, variables: Map<String, Any?>): MutationDto
}
```

We have to pass instance of `CinemaAdapter` interface to `cinemaContextOf` function to create `CinemaContext`.
The `CinemaAdapter` is not generated by Kobby Plugin, so, let write [Ktor](https://ktor.io/)
implementation
of [adapter](https://github.com/ermadmi78/kobby-gradle-example/blob/gradle-tutorial/cinema-kotlin-client/src/main/kotlin/io/github/ermadmi78/kobby/cinema/kotlin/client/adapter.kt):

```kotlin
package io.github.ermadmi78.kobby.cinema.kotlin.client

import io.github.ermadmi78.kobby.cinema.api.kobby.kotlin.CinemaAdapter
import io.github.ermadmi78.kobby.cinema.api.kobby.kotlin.dto.MutationDto
import io.github.ermadmi78.kobby.cinema.api.kobby.kotlin.dto.QueryDto
import io.github.ermadmi78.kobby.cinema.api.kobby.kotlin.dto.graphql.CinemaException
import io.github.ermadmi78.kobby.cinema.api.kobby.kotlin.dto.graphql.CinemaMutationResult
import io.github.ermadmi78.kobby.cinema.api.kobby.kotlin.dto.graphql.CinemaQueryResult
import io.github.ermadmi78.kobby.cinema.api.kobby.kotlin.dto.graphql.CinemaRequest
import io.ktor.client.*
import io.ktor.client.request.*

class CinemaKtorAdapter(private val client: HttpClient) : CinemaAdapter {
    override suspend fun executeQuery(query: String, variables: Map<String, Any?>): QueryDto {
        val request = CinemaRequest(query, variables)
        val result = client.post<CinemaQueryResult> {
            body = request
        }

        result.errors?.takeIf { it.isNotEmpty() }?.let {
            throw CinemaException("GraphQL query failed", request, it)
        }
        return result.data ?: throw CinemaException(
            "GraphQL query completes successfully but returns no data",
            request
        )
    }

    override suspend fun executeMutation(query: String, variables: Map<String, Any?>): MutationDto {
        val request = CinemaRequest(query, variables)
        val result = client.post<CinemaMutationResult> {
            body = request
        }

        result.errors?.takeIf { it.isNotEmpty() }?.let {
            throw CinemaException("GraphQL mutation failed", request, it)
        }
        return result.data ?: throw CinemaException(
            "GraphQL mutation completes successfully but returns no data",
            request
        )
    }
}
```

Your can use any other HTTP Client to implement adapter. For example,
see [here](https://github.com/ermadmi78/kobby-gradle-example/blob/main/cinema-server/src/test/kotlin/io/github/ermadmi78/kobby/cinema/server/CinemaTestAdapter.kt)
adapter implementation for Spring Boot integration tests.

## Configure Ktor client

```kotlin
private val httpClient = HttpClient {
    expectSuccess = true
    install(JsonFeature) {
        serializer = JacksonSerializer {
            registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            registerModule(JavaTimeModule())
            // Force Jackson to serialize dates as String
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
        url { takeFrom("http://localhost:8080/graphql") }
    }
}
```

## Ok, we are ready to execute GraphQL queries by means of generated Kotlin DSL

### Simple query

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/simple_query.png)

### Simple mutation

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/mutation.png)

### We can upload complex graph by means of our Kotlin DSL

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/complex_query.png)

### GraphQL's unions and interfaces are supported too

![alt text](https://github.com/ermadmi78/kobby/blob/main/images/union_query.png)

