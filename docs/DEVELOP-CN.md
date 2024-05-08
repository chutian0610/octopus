# Develop

## Requirements

- jdk 17+
- scala 3.x 
- sbt 1.9.x

## IDE

IntelliJ IDEA or VS Code，Please refer to [SBT IDE Integration](https://www.scala-sbt.org/1.x/docs/IDE.html)

## Code Style

### `?` operator

use rust like `?` operator([quest](https://github.com/reactivecore/quest)) to make complex logical more readable 

for example 

```scala 3
parseColumnNames(tokenStream).flatMap(columnNameList => {
  expectKeyWord(tokenStream, KEYWORDS.AS).flatMap(_ =>{
    expectToken(tokenStream, Tokens.leftParen).flatMap(_ => {
      parseQuery(tokenStream).flatMap(subQuery => {
        expectToken(tokenStream, Tokens.rightParen).flatMap(_ => {
          Right(WithQuery(
            position = name.position,
            name = name,
            columnNames = Some(columnNameList),
            query = subQuery))
        })
      })
    })
  })
})
```

```scala 3
import  quest._
quest{
  val columnNameList= parseColumnNames(tokenStream).?
  expectKeyWord(tokenStream, KEYWORDS.AS).?
  expectToken(tokenStream, Tokens.leftParen).?
  val subQuery= parseQuery(tokenStream).?
  expectToken(tokenStream, Tokens.rightParen).?
  Right(WithQuery(
        position = name.position, name = name,
        columnNames = Some(columnNameList),
        query = subQuery))
}
```
