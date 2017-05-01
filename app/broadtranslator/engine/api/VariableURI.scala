package broadtranslator.engine.api

object VariableURI {
  def apply(uri: Option[String]): Option[VariableURI] = uri.map(VariableURI(_))
}

case class VariableURI(uri: String) {
  
}