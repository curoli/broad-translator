package broadtranslator.json

import broadtranslator.engine.api.id._
import broadtranslator.engine.api.evaluate._
import TranslatorIdsJsonReading.{ variableIdReads, groupIdReads, modelIdReads }
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

/**
 * broadtranslator
 * Created by oliverr on 4/4/2017.
 */
object EvaluateRequestJsonReading {

  implicit val outputGroupReads: Reads[OutputGroup] =
    ((JsPath \ "variableGroupID").read[VariableGroupId] and
      (JsPath \ "variableID").read[Seq[VariableId]])(OutputGroup)

  implicit val variableValueReads: Reads[VariableValue] = new Reads[VariableValue] {
    override def reads(json: JsValue): JsResult[VariableValue] = json match {
      case JsBoolean(boolean) => JsSuccess(VariableValue.BooleanValue(boolean))
      case JsNumber(number)   => JsSuccess(VariableValue.NumberValue(number.toDouble))
      case JsString(string)   => JsSuccess(VariableValue.StringValue(string))
      case _                  => JsError(s"Expected Boolean, Number or String, but got $json.")
    }
  }

  implicit val valueProbabilityReads: Reads[ValueProbability] =
    ((JsPath \ "variableValue").read[VariableValue] and (JsPath \ "priorProbability").read[Double])(ValueProbability)

  implicit val modelVariableReads: Reads[ModelVariable] =
    ((JsPath \ "variableID").read[VariableId] and
      (JsPath \ "priorDistribution").read[Seq[ValueProbability]])(ModelVariable(_, _))

  implicit val variableGroupReads: Reads[VariableGroup] =
    ((JsPath \ "variableGroupID").read[VariableGroupId] and
      (JsPath \ "modelVariable").read[Seq[ModelVariable]])(VariableGroup)

  implicit val evaluateRequestReads: Reads[EvaluateModelRequest] =
    ((JsPath \ "modelID").read[ModelId] and
      (JsPath \ "modelInput").read[Seq[VariableGroup]] and
      (JsPath \ "modelOutput").read[Seq[OutputGroup]])(EvaluateModelRequest)

}