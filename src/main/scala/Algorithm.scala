package org.template.vanilla

import org.apache.predictionio.controller.{ PersistentModel, PersistentModelLoader }
import org.apache.predictionio.controller.PAlgorithm
import org.apache.predictionio.controller.Params

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import grizzled.slf4j.Logger

case class AlgorithmParams(mult: Int) extends Params

class Algorithm(val ap: AlgorithmParams)
  extends PAlgorithm[PreparedData, Model, Query, PredictedResult] {

  @transient lazy val logger = Logger[this.type]

  def train(sc: SparkContext, data: PreparedData): Model = {
    // Simply count number of events
    // and multiple it by the algorithm parameter
    // and store the number as model
    val count = data.events.count().toInt * ap.mult
    new Model(mc = count)
  }

  def predict(model: Model, query: Query): PredictedResult = {
    // Prefix the query with the model data
    val result = s"${model.mc}-${query.q}"
    PredictedResult(p = result)
  }
}

class Model(val mc: Int) extends PersistentModel[AlgorithmParams] {
  override def toString = s"mc=${mc}"

  def save(id: String,
    params: AlgorithmParams,
    sc: SparkContext): Boolean = {
    sc.parallelize(Seq(mc)).saveAsObjectFile(s"/pio-model/${id}/testWrite")
    true
  }
}

object Model extends PersistentModelLoader[AlgorithmParams, Model] {
  def apply(id: String, params: AlgorithmParams,
    sc: Option[SparkContext]) = {

    val count = sc.get.objectFile[Int](s"/pio-model/${id}/testWrite").first

    new Model(mc = count)
  }
}
