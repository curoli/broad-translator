package broadtranslator.engine.mock


import scala.sys.process._
import scala.collection.mutable.{Map => MMap, Set => MSet}

import java.net.URI
import java.io.File
import java.io.PrintWriter
import java.io.FileWriter
import java.io.FileReader
import java.io.BufferedReader

import broadtranslator.engine.TranslatorEngine
import broadtranslator.engine.api._
import broadtranslator.engine.api.smart.SmartSpecs

import play.api.Logger
import play.api.Configuration

/**
  * broadtranslator
  * Created by oliverr on 3/31/2017.
  */
class MockTranslatorEngine extends TranslatorEngine {
  val applesNames = Seq("Gala", "Granny Smith", "Fuji", "Pink Lady")
  val applesList = VarValueSet.StringList(applesNames)
  val applesGroup = VariableGroupId("apples")
  val appleOneVar = VariableId("apple one")
  val appleTwoVar = VariableId("apple two")

  val orangesNames = Seq("Navel", "Clementine")
  val orangesList = VarValueSet.StringList(orangesNames)
  val orangesGroup = VariableGroupId("oranges")
  val bigOrangeVar = VariableId("big orange")
  val smallOrangeVar = VariableId("small orange")

  private val logger = Logger(getClass)
  
  
  override def getAvailableModelIds: ModelListResult =
    ModelListResult(Seq("ModelOne", "ModelTwo", "ModelRed", "ModelBlue").map(ModelId))

  override def getSmartSpecs(modelId: ModelId): SmartSpecs =
    SmartSpecs(modelId.string, new URI("http://www.broadinstitute.org/translator"))

  def getModelSignatureMock(modelId: ModelId): ModelSignatureResult =
    ModelSignatureResult(modelId, Map(
      applesGroup -> VariableGroup(modelId, applesGroup, asConstraints = true, asOutputs = false, applesList),
      orangesGroup -> VariableGroup(modelId, orangesGroup, asConstraints = false, asOutputs = true, orangesList)
    ))

  def getVariablesByGroupMock(modelId: ModelId, groupId: VariableGroupId): VariablesByGroupResult =
    VariablesByGroupResult(VariableGroup(modelId, groupId, asConstraints = true, asOutputs = false, applesList),
      Seq(appleOneVar, appleTwoVar))

      
  def evaluateMock(request: EvaluateRequest): EvaluateResult ={
    EvaluateResult(Seq(
      GroupWithProbabilities(orangesGroup, Seq(
        VariableWithProbabilities(bigOrangeVar, ProbabilityDistribution.Discrete(Map(
          1 -> 0.85, 2 -> 0.15
        ))),
        VariableWithProbabilities(smallOrangeVar, ProbabilityDistribution.Discrete(Map(
          1 -> 0.07, 2 -> 0.93
        )))
      ))
    ))
  }
  
  
  override def getModelSignature(modelId: ModelId): ModelSignatureResult = {
    val (ioMap, varMap) = loadSignature(modelId.string)
    val response = createSignatureResult(modelId.string, ioMap, varMap)
    return response
  }
  
  override def getVariablesByGroup(modelId: ModelId, groupId: VariableGroupId): VariablesByGroupResult = {
    val (ioMap, varMap) = loadSignature(modelId.string)
    val group = variableMap(modelId.string, groupId.string, ioMap(groupId.string), varMap(groupId.string))
    val variables = for ((variable, properties) <- varMap(groupId.string)) yield VariableId(variable)
    return VariablesByGroupResult(group, variables.toSeq)
  }
    
  private def loadSignature(modelId: String): (MMap[String,MSet[String]], MMap[String,MMap[String,(String, String, String)]]) = {
    val file = new File("models/"+modelId+"/modelSignature.txt")
    logger.info("loading model signature for "+modelId+" from "+file)
    val input = new BufferedReader(new FileReader(file))
    val header = parseHeader(input.readLine()) 
    val ioMap = MMap[String,MSet[String]]()
    val varMap = MMap[String,MMap[String,(String, String, String)]]()
    var line = input.readLine()
    while (line != null) {
      val row = line.split("\t")
      val io = row(header("io"))
      val groupName = row(header("variableGroup"))
      val variableName = row(header("variableName"))
      val uri = row(header("uri"))
      val valueType = row(header("valueType"))
      val values = row(header("allowedVariableValues"))

      if (!ioMap.contains(groupName)) ioMap(groupName) = MSet()
      ioMap(groupName) add io
      
      if (!varMap.contains(groupName)) varMap(groupName) = MMap()
      val group = varMap(groupName)
      group(variableName) = (uri, valueType, values)
      
      line = input.readLine()
    }
    input.close()
    (ioMap, varMap)
  }
  
  private def createSignatureResult(modelId: String, ioMap: MMap[String,MSet[String]], varMap: MMap[String,MMap[String,(String, String, String)]]): ModelSignatureResult = {
    val groups = for ((groupId, io) <- ioMap) yield ( VariableGroupId(groupId) -> variableMap(modelId, groupId, ioMap(groupId), varMap(groupId)))
    return ModelSignatureResult(ModelId(modelId), groups.toMap)
  }
  
  private def variableMap(modelId: String, groupId: String, ioSet: MSet[String], variables: MMap[String,(String, String, String)]): VariableGroup = {
    val asInput = ioSet.contains("input") || ioSet.contains("input; output")
    val asOutput = ioSet.contains("output") || ioSet.contains("input; output")
    val uriSet = (for ((variable, (uri, valueType, values)) <- variables) yield uri).toSet
    val uri = if (uriSet.size == 1) Some(uriSet.toSeq(0)) else None
    val typeSet = (for ((variable, (uri, valueType, values)) <- variables) yield valueType).toSet
    val valueType = if (typeSet.size == 1) Some(typeSet.toSeq(0)) else None
    val valuesSet = (for ((variable, (uri, valueType, values)) <- variables) yield values).toSet
    val valuesString = if (valuesSet.size == 1) Some(valuesSet.toSeq(0)) else None
    logger.info("Homogenious variable properties (model="+modelId+" group="+groupId+"): URI="+uri + " type="+ valueType + " values="+ valuesString)
    val valueSet: VarValueSet = valuesString match {
      case None => VarValueSet.AnyString
      case Some(string) => valueType match {
        case None => VarValueSet.StringList(string.split(";"))
        case Some("Number") => VarValueSet.NumberList(string.split(";").map(_.toDouble))
        case Some("Boolean") => VarValueSet.Boolean
        case _ => VarValueSet.StringList(string.split(";"))
      }
    }
    return VariableGroup(ModelId(modelId), VariableGroupId(groupId), asInput, asOutput, valueSet)
  }
    
  override def evaluate(request: EvaluateRequest): EvaluateResult = {
    val inputFile = File.createTempFile("translatorRequest", ".txt")
    val outputFile = File.createTempFile("translatorResponse", ".txt")
    exportRequest(request, inputFile)
    executeModelCall(request.modelId.string, inputFile, outputFile)
    val response = importResponse(outputFile)
    outputFile.deleteOnExit()
    return response
  }
  
  
  private def exportRequest(request: EvaluateRequest, file: File): Unit = {
    val out = new PrintWriter(new FileWriter(file))
    out.println("io\tvariableGroup\tvariableName\tvariableValue\tprobability")
    for (
      group <- request.priors;
      variableGroup = group.groupId.string;
      variable <- group.varsWithProbs;
      variableName = variable.variableId.string
    ) {
      variable.probabilityDistribution match {
        case ProbabilityDistribution.Discrete(distribution) => for ((value, probability) <- distribution) {
          out.println("input\t" + variableGroup + "\t" + variableName + "\t" + value + "\t" + probability)
        }
        case _ =>
      }
    }
    for (
      group <- request.outputs;
      variableGroup = group.groupId.string;
      variable <- group.variableIds;
      variableName = variable.string
    ) {
      out.println("output\t" + variableGroup + "\t" + variableName + "\t\t")
    }
    out.close()
  }

  private def executeModelCall(modelId: String, input: File, output: File): Unit = {
    val cmd = "Rscript models/"+modelId+"/main.R "+input+" "+output
    logger.info("executing model: "+cmd)
    cmd.! //TODO capture stdout+stderr to logger
  }
  
  private def importResponse(file: File): EvaluateResult = {
    val response: MMap[String, MMap[String, MMap[Int, Double]]] = loadResponse(file)(_.toInt)
    //println(response)
    return createEvaluateResult(response)
  }
  
  private def loadResponse[T](file: File)(implicit f: String => T): MMap[String,MMap[String,MMap[T,Double]]] = {
    val input = new BufferedReader(new FileReader(file))
    val header = parseHeader(input.readLine())
    val groups = MMap[String, MMap[String, MMap[T, Double]]]()
    var line = input.readLine()
    while (line != null) {
      val row = line.split("\t")
      if (row(header("io")) == "output") {
        val groupName = row(header("variableGroup"))
        val variableName = row(header("variableName"))
        val value = row(header("variableValue"))
        val probability = row(header("probability")).toDouble
        if (!groups.contains(groupName)) groups(groupName) = MMap()
        val group = groups(groupName)
        if (!group.contains(variableName)) group(variableName) = MMap()
        val variable = group(variableName)
        variable(value) = probability
      }
      line = input.readLine()
    }
    input.close()
    return groups
  }
 
  private def parseHeader(header: String): Map[String,Int] = {
    val map = MMap[String, Int]()
    var i = 0;
    for (columnName <- header.split("\t")) {
      map.put(columnName, i)
      i = i + 1
    }
    return map.toMap
  }
  
  private def createEvaluateResult[T](response: MMap[String,MMap[String,MMap[T,Double]]]): EvaluateResult = {

    var groupList = List[GroupWithProbabilities]()
    for ((groupName, group) <- response) {
      val variableList = for ((variableName, variable) <- group) yield VariableWithProbabilities(VariableId(variableName), ProbabilityDistribution.Discrete(variable.toMap))
      groupList = GroupWithProbabilities(VariableGroupId(groupName), variableList.toSeq) :: groupList
    }
    //println(groupList)
    return EvaluateResult(groupList)
  }
  

}
