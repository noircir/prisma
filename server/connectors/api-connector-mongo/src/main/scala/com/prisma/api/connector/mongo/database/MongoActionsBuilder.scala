package com.prisma.api.connector.mongo.database
import org.mongodb.scala.{MongoClient, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}

// format: off
trait AllActions
  extends NodeActions
//    with RelationActions
//    with ScalarListActions
//    with ValidationActions
//    with RelayIdActions
//    with ImportActions
    with MiscActions

trait AllQueries
  extends NodeSingleQueries
//    with NodeManyQueries
//    with RelationQueries
//    with ScalarListQueries
//    with MiscQueries
// format: on

case class MongoActionsBuilder(
    schemaName: String,
    client: MongoClient
)(implicit ec: ExecutionContext)
    extends AllActions
    with AllQueries {
  val database = client.getDatabase(schemaName)
}

sealed trait MongoAction[+A] {
  def map[B](f: A => B): MongoAction[B] = MapAction(this, f)

  def flatMap[B](f: A => MongoAction[B]): MongoAction[B] = FlatMapAction(this, f)
}

object MongoAction {
  def seq[A](actions: Vector[MongoAction[A]]): MongoAction[Vector[A]] = {
    SequenceAction(actions)
  }

  def successful[A](value: A) = SuccessAction(value)
}

case class MapAction[A, B](source: MongoAction[A], fn: A => B)                  extends MongoAction[B]
case class FlatMapAction[A, B](source: MongoAction[A], fn: A => MongoAction[B]) extends MongoAction[B]

case class SimpleMongoAction[+A](fn: MongoDatabase => Future[A]) extends MongoAction[A]

case class SequenceAction[A](actions: Vector[MongoAction[A]]) extends MongoAction[Vector[A]]
case class SuccessAction[A](value: A)                         extends MongoAction[A]
