package com.amazon.milan.flink.compiler.internal

import com.amazon.milan.flink.components.LatestApplyCoProcessFunction
import com.amazon.milan.flink.{FlinkTypeNames, RuntimeEvaluator, TypeUtil}
import com.amazon.milan.program.{FlatMap, LatestBy, LeftJoin}
import com.amazon.milan.types.RecordWithLineage
import com.amazon.milan.typeutil.TypeDescriptor
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator


/**
 * Methods for applying windowed join operations to Flink data streams.
 */
object FlinkWindowedJoinFactory {
  private val typeName: String = getClass.getTypeName.stripSuffix("$")

  def applyLatestByThenApply(mapExpr: FlatMap,
                             leftInputStream: SingleOutputStreamOperator[_],
                             rightInputStream: SingleOutputStreamOperator[_],
                             lineageRecordFactory: JoinLineageRecordFactory): SingleOutputStreamOperator[RecordWithLineage[_]] = {
    val joinExpr = mapExpr.source.asInstanceOf[LeftJoin]
    val latestByExpr = joinExpr.right.asInstanceOf[LatestBy]
    val keyType = latestByExpr.keyFunc.tpe

    val leftTypeName = TypeUtil.getTypeName(leftInputStream.getType)
    val rightTypeName = TypeUtil.getTypeName(rightInputStream.getType)
    val keyTypeName = keyType.fullName
    val outputTypeName = mapExpr.getRecordTypeName

    val eval = RuntimeEvaluator.instance
    eval.evalFunction[FlatMap, SingleOutputStreamOperator[_], SingleOutputStreamOperator[_], JoinLineageRecordFactory, SingleOutputStreamOperator[RecordWithLineage[_]]](
      "mapExpr",
      eval.getClassName[FlatMap],
      "leftInputStream",
      FlinkTypeNames.singleOutputStreamOperator(leftTypeName),
      "rightInputStream",
      FlinkTypeNames.singleOutputStreamOperator(rightTypeName),
      "lineageRecordFactory",
      eval.getClassName[JoinLineageRecordFactory],
      s"${this.typeName}.applyLatestByThenApplyImpl[$leftTypeName, $rightTypeName, $keyTypeName, $outputTypeName](mapExpr, leftInputStream, rightInputStream, lineageRecordFactory)",
      mapExpr,
      leftInputStream,
      rightInputStream,
      lineageRecordFactory)
  }

  def applyLatestByThenApplyImpl[TLeft, TRight, TKey, TOut](mapExpr: FlatMap,
                                                            leftInputStream: SingleOutputStreamOperator[TLeft],
                                                            rightInputStream: SingleOutputStreamOperator[TRight],
                                                            lineageRecordFactory: JoinLineageRecordFactory): SingleOutputStreamOperator[RecordWithLineage[TOut]] = {
    val joinExpr = mapExpr.source.asInstanceOf[LeftJoin]
    val latestByExpr = joinExpr.right.asInstanceOf[LatestBy]

    val eval = RuntimeEvaluator.instance

    val leftRecordType = joinExpr.left.recordType.asInstanceOf[TypeDescriptor[TLeft]]
    val rightRecordType = joinExpr.right.getInputRecordType.asInstanceOf[TypeDescriptor[TRight]]
    val rightTypeInfo = eval.createTypeInformation(rightRecordType)

    val keyType = latestByExpr.keyFunc.tpe.asInstanceOf[TypeDescriptor[TKey]]
    val keyTypeInfo = eval.createTypeInformation(keyType)

    val outputType = mapExpr.tpe.getRecordType.asInstanceOf[TypeDescriptor[TOut]]
    val outputTypeInfo = eval.createTypeInformation(outputType)

    val coProcessFunction =
      new LatestApplyCoProcessFunction[TLeft, TRight, TKey, TOut](
        latestByExpr.dateExtractor,
        latestByExpr.keyFunc,
        mapExpr.expr,
        leftRecordType,
        rightRecordType,
        rightTypeInfo,
        keyTypeInfo,
        outputTypeInfo,
        lineageRecordFactory)

    val leftKeyed = leftInputStream.keyBy(new SingletonKeySelector[TLeft])
    val rightKeyed = rightInputStream.keyBy(new SingletonKeySelector[TRight])

    leftKeyed.connect(rightKeyed).process(coProcessFunction)
  }

  class SingletonKeySelector[T] extends KeySelector[T, Int] {
    override def getKey(in: T): Int = 0
  }

}
