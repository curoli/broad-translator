package broadtranslator.engine.api.signature

case class ProbabilityDistributionName(name: String) {

}

object ProbabilityDistributionName {

  def scalar = ProbabilityDistributionName("scalar")
  def discrete = ProbabilityDistributionName("discrete")
  def empirical = ProbabilityDistributionName("empirical")
  def Gaussian = ProbabilityDistributionName("Gaussian")
  def Poisson = ProbabilityDistributionName("Poisson")

}
