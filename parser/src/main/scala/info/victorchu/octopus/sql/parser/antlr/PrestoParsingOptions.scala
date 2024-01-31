package info.victorchu.octopus.sql.parser.antlr

import java.util.Objects

class PrestoParsingOptions(val decimalLiteralTreatment: PrestoParsingOptions.DecimalLiteralTreatment) {
  def getDecimalLiteralTreatment: PrestoParsingOptions.DecimalLiteralTreatment = decimalLiteralTreatment
}
object PrestoParsingOptions{
  enum DecimalLiteralTreatment {
    case AS_DOUBLE, AS_DECIMAL, REJECT
  }
  def apply():PrestoParsingOptions={
    new PrestoParsingOptions(DecimalLiteralTreatment.REJECT)
  }
  def apply(decimalLiteralTreatment: PrestoParsingOptions.DecimalLiteralTreatment ):PrestoParsingOptions={
    Objects.requireNonNull(decimalLiteralTreatment, "decimalLiteralTreatment is null")
    new PrestoParsingOptions(decimalLiteralTreatment)
  }
}
