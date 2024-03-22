# Develop

## env 

- jdk 17+
- scala 3.x 
- sbt 1.9.x

## plugin

- Antlr4Plugin: 根据g4文件生成antlr4 代码的插件

## IDE

使用bsp方式导入vscode，参考 [https://www.scala-sbt.org/1.x/docs/IDE.html](https://www.scala-sbt.org/1.x/docs/IDE.html)

## 参考资料

- [ISO sql 99](https://ronsavage.github.io/SQL/sql-99.bnf.html)


version = 3.8.0
runner.dialect = scala3
maxColumn = 100
align.preset = false
assumeStandardLibraryStripMargin = true
align.stripMargin = true
danglingParentheses.preset = true
rewrite.rules = [SortImports]
importSelectors = singleLine
binPack.parentConstructors = true
includeCurlyBraceInSelectChains = false