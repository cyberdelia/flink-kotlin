# Kotlin support for Apache Flink

This package provides a type information specific to kotlin data classes (including `Pair` and `Triple`).

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

1. Fields can be removed. Once removed, the previous value for the removed field will be dropped in future checkpoints and savepoints.
2. New fields can be added. The new field will be initialized to the default value for its type, as defined by Java.
3. Declared fields types cannot change.
4. Class name of type cannot change, including the namespace of the class.
5. Null fields are not supported.

