# Kotlin support for Apache Flink

This package provides type information and
specialized [type serializers](https://nightlies.apache.org/flink/flink-docs-release-1.15/docs/dev/datastream/fault-tolerance/serialization/types_serialization/)
for kotlin data classes (including `Pair` and `Triple`), as well as for Kotlin `Map` and `Collection` types.

## Installation

The package is available
in [Github](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package),
using Gradle:

```kotlin
implementation("com.lapanthere:flink-kotlin:0.1.0")
```

## Usage

### TypeInformation

Using the `createTypeInformation` that will return a Kotlin friendly `TypeInformation` for data classes, collections,
maps, etc:

```kotlin
dataStream.process(
    processFunction(),
    createTypeInformation<Row>()
)
```

It also supports fields name in the definition of keys, i.e. you will be able to use name of fields directly:

```kotlin
dataStream.join(another).where("name").equalTo("personName")
```

You can also annotate your data classes with the `@TypeInfo` annotation:

```kotlin
@TypeInfo(DataClassTypeInfoFactory::class)
data class Record(
    val name: String,
    val value: Long
)
```

## Schema evolution

Schema evolution for data classes follow this set of rules:

1. Fields can be removed. Once removed, the previous value for the removed field will be dropped in future checkpoints
   and savepoints.
2. New fields can be added. The new field will be initialized to the default value for its type, as defined by Java.
3. Declared fields types cannot change.
4. Class name of type cannot change, including the namespace of the class.
5. Null fields are not supported.

