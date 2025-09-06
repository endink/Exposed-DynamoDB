
<div align="center">
<h1>Exposed-DynamoDB</h1>
</div>
<br>

[English](./README.md)

<br>

<div align="center">

![maven central version](https://img.shields.io/maven-central/v/com.labijie.orm/exposed-dynamodb?logo=java)
![workflow status](https://img.shields.io/github/actions/workflow/status/endink/Exposed-DynamoDB/build.yml)
![license](https://img.shields.io/github/license/endink/Exposed-DynamoDB?style=flat-square)
![Static Badge](https://img.shields.io/badge/GraalVM-supported-green?style=flat&logoColor=blue&labelColor=orange)

</div>

<br>

本项目受到 [Kotlin-Exposed](https://github.com/JetBrains/Exposed) 启发，旨在为 **AWS DynamoDB** 提供一种类型安全的 **Kotlin DSL 查询语法**。  
它能让开发者像操作本地对象一样操作 DynamoDB 表，提供简洁、直观、可组合的 API，包括：

- 表结构定义
- 类型安全的查询条件
- 投影查询
- 条件插入 / 更新 / 删除
- 多层嵌套字段操作（list / map / set）
- DynamoDB 函数表达式支持（attribute_type, list_append 等）
- Kotlin 风格 API，贴近 Exposed 的开发体验

> 本项目仍然在开发中，目前只提供 `1.0.0-SNAPSHOT` 版本，在生产环境中使用请仔细测试。      
> 你可以查看单元测试代码了解完整用法。

---

## 🚀 Getting Started

### 环境要求
- JDK 21+
- Kotlin 2.1.12+
- AWS SDK for Java v2
- Gradle 8.14

### 安装依赖

#### Gradle (Kotlin DSL)
```kotlin
dependencies {
    // AWS SDK v2 DynamoDB
    implementation("software.amazon.awssdk:dynamodb: 2.27.3")
    // 本项目 DSL
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

## 🛠️ 用法

### 1. 定义表结构

代码中的表代表一个逻辑表 (按关系数据库习惯定义的"表")， 实际上 DynamoDb 的表仅受 name 影响，如下
`test_table` 是 DynamoDB 的一个表。

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

    val stringSet = stringSet("string_set")
    val numberSet = numberSet("number_set")
    val binaryValue = binary("binary_value")
    val binarySet = binarySet("binary_set")

    val listValue = list("list_value")
    val mapValue = map("map_value")

    override val keys: DynamoKeys
        get() = DynamoKeys(pk, sk)
}
```
string 等操作将映射一个到一个 DynamoDB 属性，例如 
```kotlin
val name = string("name_s")
``` 

表示映射一个属性到 DynamoDB， 在 DynamoDB 表中，这个属性名称是 "name_s" .

#### 单表建模    

和所有的列式 NOSQL 数据库一样，DynamoDB 推崇单表建模，因此逻辑表结构可以通过相同的表名来映射到同一个 DynamoDB 的表。

```kotlin

object TestTable1 : DynamoTable("test_table")

object TestTable2 : DynamoTable("test_table")

object TestTable3 : DynamoTable("test_table")

```

查询和更新中我们通过表达式实现跨逻辑表更新数据。    

对于单表建模，还可以通过继承方式，这样无需重复定义 PK/SK/LSI

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
    //...your columns
}

object TestTable3 : TestTableBase() {
    //...your columns
}

```


---

#### 创建 DynamoDB 表

```kotlin

val client: DynamoDbClient
DynamodbSchemaUtils.createTableIfNotExist(client, TestTable)

```

### 2. 查询示例

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

#### 投影

如果 Get/Query 操作中没有指定投影，默认查询当前逻辑表 (`DynamoTable`) 中的所有属性 (`Column`)。你也可以手动指定要投影的属性。   

1. 投影当前 Table (默认行为)
```kotlin
TestTable.get {
    project(TestTable)
}.keys {
    TestTable.name eq "bbb"
}

```

2. 查询所有属性
```kotlin
TestTable.get {
    projectAll()
}.keys {
    TestTable.name eq "bbb"
}

```

3. 查询指定列
```kotlin
TestTable.get {
    projectAll(TestTable.name, TestTable.listValue[0]["a"]) // 查询 name 和 list[0].a
}.keys {
    TestTable.name eq "bbb"
}

```

4. 多表查询
```kotlin
TestTable.get {
    projectAll(Table1, Table2, Table3.a) // Table1 和 Table2 所有属性, Table3 的 a 属性
}.keys {
    TestTable.name eq "bbb"
}

```

#### 使用本地二级索引 (LSI)

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

### 3. 插入数据示例

```kotlin

简单更新

TestTable.put { 
    it[TestTable.name] = "a"
    it[TestTable.intValue] = null
}
```

条件表达式
```kotlin
TestTable.put {
    it[TestTable.name] = "a"
} condition {
    TestTable.intValue eq 0
}
```

---

### 4. 删除数据示例

```kotlin
// 简单根据 PK, SK 删除
TestTable.delete {
    keys {
        TestTable.name eq "bbb"
    }
}

// 条件表达式删除
TestTable.delete {
    keys {
        TestTable.name eq "bbb"
    }
    condition { TestTable.intValue eq 0 }
}
```

---

### 5. 更新数据示例

简单更新   

```kotlin
// 简单根据 PK, SK 更新
TestTable.update({
    keys { TestTable.name eq "bbb" }
}) {
    it[TestTable.name] = "aaa"
    it[TestTable.floatValue] = TestTable.floatValue + 1.0f // set floatValue =  floatValue + 1
    it[TestTable.intValue] = TestTable.intValue.ifNotExists(0) + 1 // set intValue =  if_not_exists(int_value, 0) + 1
    it[TestTable.numberSet] += setOfString(123, 456) //ADD
    it[TestTable.stringSet] -= setOfString("ccc") //DELETE
    it[TestTable.mapValue]["aaa"][1]["bb"] += setOfString("aaa", "bbb") //ADD
}

```

条件表达式更新    

```kotlin
TestTable.update({
    keys { TestTable.name eq "bbb" }
    condition { 
        (TestTable.intValue eq 1) or 
        (TestTable.intValue less 10) 
    }
}) {
    it[TestTable.intValue] = TestTable.intValue.ifNotExists(0) + 1 // set intValue =  if_not_exists(int_value, 0) + 1
    it[TestTable.stringSet] += setOfString("aa", "bbb") //ADD
    it[TestTable.stringSet] -= setOfString("ccc") //DELETE
    it[TestTable.mapValue]["aaa"][1]["bb"] += setOfString("aaa", "bbb") //ADD
}
```

> 支持 list_append/ADD/DELETE 函数自动转换。

---

## 🧪 Demo 示例

启动本地 DynamoDB（推荐 [DynamoDB Local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)）或使用真实 AWS 账户。

```kotlin
class CRUDTester {

    private lateinit var client: DynamoDbClient

    fun testCRUD() {

        // 获取单条记录
        val got = TestTable.get().keys {
            TestTable.name eq "bbb"
        }
        client.getItem(got.request())

        // 插入记录
        TestTable.put {
            it[TestTable.name] = "a"
            it[TestTable.intValue] = null
        }

        // 更新记录
        TestTable.update({
            keys { TestTable.name eq "bbb" }
            condition { (TestTable.intValue eq 1) or (TestTable.intValue less 10) }
        }) {
            it[TestTable.intValue] = TestTable.intValue + 1
            it[TestTable.stringSet] += setOfString("aa", "bbb")
        }

        // 删除记录
        TestTable.delete {
            keys { TestTable.name eq "bbb" }
            condition { TestTable.intValue eq 0 }
        }

        // 查询示例
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

## 📦 为什么要用 exposed-dynamodb ?
- 类型安全的 DynamoDB DSL
- 支持投影查询、条件表达式
- 原生支持 list / map / set 嵌套操作
- Kotlin 风格 API，贴近 kotlin-exposed 的编程体验

---

## 📜 License
本项目采用 [Apache License 2.0](./LICENSE)。  
欢迎提交 Issue 和 PR，一起完善功能 🎉
