<div align="center">
<h1>Exposed-DynamoDB</h1>
</div>
<br>

[简体中文](./README-zh.md)

<br>

<div align="center">

![maven central version](https://img.shields.io/maven-central/v/com.labijie.orm/exposed-dynamodb?logo=java)
![maven snapshot version](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fcom%2Flabijie%2Form%2Fexposed-dynamodb%2Fmaven-metadata.xml&label=maven-snapshot&color=pink
)
![workflow status](https://img.shields.io/github/actions/workflow/status/endink/Exposed-DynamoDB/build.yml)
![license](https://img.shields.io/github/license/endink/Exposed-DynamoDB?style=flat-square)
![Static Badge](https://img.shields.io/badge/GraalVM-supported-green?style=flat&logoColor=blue&labelColor=orange)

</div>

<br>

This project is inspired by [Kotlin-Exposed](https://github.com/JetBrains/Exposed) and aims to provide a type-safe **Kotlin DSL query syntax** for **AWS DynamoDB**.  
It allows developers to manipulate DynamoDB tables like local objects and provides concise, intuitive, and composable APIs, including:

- Table schema definition
- Type-safe query conditions
- Projection queries
- Conditional insert / update / delete
- Multi-level nested field operations (list / map / set)
- Support for DynamoDB function expressions (attribute_type, list_append, etc.)
- Kotlin-style API, closely following the Exposed development experience

> This project is still WIP, and currently only provides a `1.0.0-SNAPSHOT` version. Please test thoroughly before using it in production.  
> You can check the unit test code to see full usage examples.

---

## 🚀 Getting Started

### Requirements
- JDK 21+
- Kotlin 2.1.12+
- AWS SDK for Java v2
- Gradle 8.14

### Install Dependencies

#### Gradle (Kotlin DSL)
```kotlin
dependencies {
    // AWS SDK v2 DynamoDB
    implementation("software.amazon.awssdk:dynamodb: 2.27.3")
    // This project's DSL
    implementation("com.labijie.orm:dynamodb-exposed: 1.0.0")
}

```

#### Maven
```xml
<dependencies>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb</artifactId>
        <version>2.25.54</version>
    </dependency>
    <dependency>
        <groupId>com.labijie.orm</groupId>
        <artifactId>dynamodb-exposed-dsl</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

---

## 🛠️ Usage

### 1. Define Table Schema

Tables in code represent logical tables (defined in the style of relational databases), while in reality, DynamoDB tables are only identified by their name. For example,  
`test_table` represents a DynamoDB table.

```kotlin
object TestTable : DynamoTable("test_table") {

    val pk = string("pk")            // partition key
    val sk = string("sk")            // sort key

    val name = string("name_s")
    val boolValue = boolean("bool_value")
    val intValue = integer("int_value")
    val floatValue = float("float_value")
    val doubleValue = double("double_value")
    val shortValue = short("short_value")
    val longValue = long("long_value")
        .index("idx_long", ProjectionType.ALL)
    val stringValue = string("string_value")
        .index("idx_string", ProjectionType.INCLUDE, name, boolValue)
```

The `string` operation maps a property directly to a DynamoDB attribute, for example:

```kotlin
val name = string("name_s")
```

This maps a column to a DynamoDB attribute with the name `"name_s"`.

#### Single Table Modeling

Like all column-oriented NoSQL databases, DynamoDB encourages single-table modeling. Therefore, multiple logical tables can map to the same physical DynamoDB table using the same table name:

```kotlin
object TestTable1 : DynamoTable("test_table")
object TestTable2 : DynamoTable("test_table")
object TestTable3 : DynamoTable("test_table")
```

Queries and updates can operate across logical tables using expressions.

You can also use inheritance to avoid repeating PK/SK/LSI definitions:

```kotlin
abstract class TestTableBase : DynamoTable("test_table") {
    val pk = string("pk")            // partition key
    val sk = string("sk")            // sort key
    
    val longValue = long("long_value").index("idx_long", ProjectionType.ALL) // LSI with projection
    val stringValue = string("string_value").index("idx_string", ProjectionType.INCLUDE, name, boolValue) // LSI with projection

    override val keys: DynamoKeys
        get() = DynamoKeys(pk, sk)
}

object TestTable2 : TestTableBase() {
    // ... your columns
}

object TestTable3 : TestTableBase() {
    // ... your columns
}
```

---

#### Creating DynamoDB Table

```kotlin
val client: DynamoDbClient
DynamodbSchemaUtils.createTableIfNotExist(client, TestTable)
```

### 2. Query Example

```kotlin

//GetItem    
val got = TestTable.get().keys {
    (TestTable.pk eq "bbb") and (TestTable.sk eq "abcd")
}
client.getItem(got.request())

//Query
val query = TestTable.query().keys {
    (TestTable.pk eq "bbb") and (TestTable.sk beginWith "abc")
}
client.query(got.request())

```

#### Projection

If no projection is specified for Get/Query operations, all attributes (`Column`) in the current logical table (`DynamoTable`) are queried by default. You can also specify projected attributes manually.

1. Project current table (default behavior)
```kotlin
TestTable.get {
    project(TestTable)
}.keys {
    TestTable.name eq "bbb"
}
```

2. Project all attributes
```kotlin
TestTable.get {
    projectAll()
}.keys {
    TestTable.name eq "bbb"
}
```

3. Project specific columns
```kotlin
TestTable.get {
    projectAll(TestTable.name, TestTable.listValue[0]["a"]) // query name and list[0].a
}.keys {
    TestTable.name eq "bbb"
}
```

4. Multi-table projection
```kotlin
TestTable.get {
    projectAll(Table1, Table2, Table3.a) // All attributes from Table1 and Table2, a column from Table3
}.keys {
    TestTable.name eq "bbb"
}
```

#### Using Local Secondary Index (LSI)

```kotlin
val query = TestTable.query {
        project(TestTable.name, TestTable, TestTable.listValue[0]["a"])
    }
    .keys (index = "idx_long") {
        (TestTable.name eq "a") and
        (TestTable.shortValue eq 3.toShort())
    }
    .filter {
        (TestTable.name contains "aaa") and
        (TestTable.name eq "a") and
        (TestTable.name.size() lessEq 3) and
        (TestTable.name.isString()) and  // attribute_type(name, "S")
        ((TestTable.name beginsWith "333") or (TestTable.shortValue.between(1, 3))) or
        (TestTable.intValue.exists()) or
        (TestTable.stringSet contains "1") or
        not(TestTable.intValue eq 3)
    }
query.request()
```

---

### 3. Insert Data Example

```kotlin
// Simple insert
TestTable.put {
    it[TestTable.name] = "a"
    it[TestTable.intValue] = null
}
```

Conditional insert
```kotlin
TestTable.put {
    it[TestTable.name] = "a"
} condition {
    TestTable.intValue eq 0
}
```

---

### 4. Delete Data Example

```kotlin
// Simple delete by PK and SK
TestTable.delete {
    keys {
        TestTable.name eq "bbb"
    }
}

// Conditional delete
TestTable.delete {
    keys {
        TestTable.name eq "bbb"
    }
    condition { TestTable.intValue eq 0 }
}
```

---

### 5. Update Data Example

Simple update

```kotlin
// Update by PK and SK
TestTable.update({
    keys { TestTable.name eq "bbb" }
}) {
    it[TestTable.name] = "aaa"
    it[TestTable.floatValue] = TestTable.floatValue + 1.0f // set floatValue = floatValue + 1
    it[TestTable.intValue] = TestTable.intValue.ifNotExists(0) + 1 // set intValue = if_not_exists(int_value, 0) + 1
    it[TestTable.numberSet] += setOfString(123, 456) // ADD
    it[TestTable.stringSet] -= setOfString("ccc") // DELETE
    it[TestTable.mapValue]["aaa"][1]["bb"] += setOfString("aaa", "bbb") // ADD
}
```

Conditional update

```kotlin
TestTable.update({
    keys { TestTable.name eq "bbb" }
    condition {
        (TestTable.intValue eq 1) or
        (TestTable.intValue less 10)
    }
}) {
    it[TestTable.intValue] = TestTable.intValue.ifNotExists(0) + 1
    it[TestTable.stringSet] += setOfString("aa", "bbb")
    it[TestTable.stringSet] -= setOfString("ccc")
    it[TestTable.mapValue]["aaa"][1]["bb"] += setOfString("aaa", "bbb")
}
```

> Functions like list_append / ADD / DELETE are automatically converted.

---

## 🧪 Demo Example

Start a local DynamoDB (recommended [DynamoDB Local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)) or use a real AWS account.

```kotlin
class CRUDTester {

    private lateinit var client: DynamoDbClient

    fun testCRUD() {

        // Get single item
        val got = TestTable.get().keys {
            TestTable.name eq "bbb"
        }
        client.getItem(got.request())

        // Insert item
        TestTable.put {
            it[TestTable.name] = "a"
            it[TestTable.intValue] = null
        }

        // Update item
        TestTable.update({
            keys { TestTable.name eq "bbb" }
            condition { (TestTable.intValue eq 1) or (TestTable.intValue less 10) }
        }) {
            it[TestTable.intValue] = TestTable.intValue + 1
            it[TestTable.stringSet] += setOfString("aa", "bbb")
        }

        // Delete item
        TestTable.delete {
            keys { TestTable.name eq "bbb" }
            condition { TestTable.intValue eq 0 }
        }

        // Query example
        val query = TestTable.query()
            .keys(index = "idx_long") {
                TestTable.name eq "a" and (TestTable.name beginsWith "aa")
            }
            .filter {
                (TestTable.name contains "aaa") or not(TestTable.intValue eq 3)
            }
        query.request()
    }
}
```

---

## 📦 Why use exposed-dynamodb?
- Type-safe DynamoDB DSL
- Supports projection queries and conditional expressions
- Native support for nested list / map / set operations
- Kotlin-style API, closely following the Kotlin-Exposed programming experience

---

## 📜 License
This project is licensed under [Apache License 2.0](./LICENSE).  
Issues and PRs are welcome to help improve the functionality 🎉

