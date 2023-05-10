package maia.ml.learner.standard

import kotlinx.coroutines.flow.collect
import maia.configure.Configurable
import maia.configure.Configuration
import maia.configure.ConfigurationElement
import maia.configure.ConfigurationItem
import maia.configure.asReconfigureBlock
import maia.ml.dataset.AsyncDataStream
import maia.ml.dataset.DataRow
import maia.ml.dataset.headers.DataColumnHeaders
import maia.ml.dataset.headers.DataColumnHeadersView
import maia.ml.dataset.headers.ensureOwnership
import maia.ml.dataset.type.DataRepresentation
import maia.ml.dataset.type.EntropicRepresentation
import maia.ml.dataset.type.standard.Nominal
import maia.ml.dataset.type.standard.NominalCanonicalRepresentation
import maia.ml.dataset.type.standard.NominalIndexRepresentation
import maia.ml.dataset.type.standard.Numeric
import maia.ml.dataset.type.standard.NumericCanonicalRepresentation
import maia.ml.dataset.util.allColumnsExcept
import maia.ml.learner.AbstractLearner
import maia.ml.learner.factory.ConfigurableLearnerFactory
import maia.ml.learner.type.Classifier
import maia.ml.learner.type.LearnerType
import maia.ml.learner.type.NoMissingTargets
import maia.ml.learner.type.Regressor
import maia.ml.learner.type.SingleTarget
import maia.ml.learner.type.classLearnerType
import maia.ml.learner.type.intersectionOf
import maia.ml.learner.type.unionOf
import maia.util.datastructure.OrderedHashSet
import maia.util.enumerate
import maia.util.error.UNREACHABLE_CODE
import maia.util.property.classlevel.override

// The types of learner that ZeroR can be
val SingleTargetClassifier = intersectionOf(SingleTarget, Classifier, NoMissingTargets)
val SingleTargetRegressor = intersectionOf(SingleTarget, Regressor, NoMissingTargets)
val ZeroRUninitialisedLearnerType = unionOf(SingleTargetClassifier, SingleTargetRegressor)

/**
 * TODO
 */
class ZeroRLearner(val targetIndex : Int) : AbstractLearner<AsyncDataStream<*>>(
        ZeroRUninitialisedLearnerType,
        AsyncDataStream::class
) {
    // Define the class-level learner-type
    companion object {
        init {
            ZeroRLearner::classLearnerType.override(
                ZeroRUninitialisedLearnerType
            )
        }
    }

    /** The accumulator for the target value. */
    private lateinit var accumulator : ZeroRAccumulator<*>

    override fun performInitialisation(
        headers : DataColumnHeaders
    ) : Triple<DataColumnHeaders, DataColumnHeaders, LearnerType> {
        // Get the type of the target column
        val targetType = headers[targetIndex].type

        // Determine what type of learner we are
        val type = when (targetType) {
            is Nominal<*, *, *, *, *> -> SingleTargetClassifier
            is Numeric<*, *> -> SingleTargetRegressor
            else -> throw Exception("ZeroR requires nominal or numeric target type, found $targetType")
        }

        // Create an accumulator for the target type
        accumulator = if (targetType is Nominal<*, *, *, *, *>)
            ZeroRNominalAccumulator(targetType.indexRepresentation)
        else
            ZeroRNumericAccumulator((targetType as Numeric<*, *>).canonicalRepresentation)

        return Triple(
            DataColumnHeadersView(headers, headers.allColumnsExcept(targetIndex)),
            DataColumnHeadersView(headers, OrderedHashSet(targetIndex)),
            type
        )
    }

    override suspend fun performTrain(trainingDataset : AsyncDataStream<*>) {
        trainingDataset.rowFlow().collect { row ->
            accumulator.accumulate(row)
        }
    }

    override fun performPredict(row : DataRow) : DataRow {
        return object : DataRow {
            override val headers : DataColumnHeaders = predictOutputHeaders
            override fun <T> getValue(
                representation : DataRepresentation<*, *, out T>
            ) : T = headers.ensureOwnership(representation) {
                accumulator.evaluate(representation)
            }
        }
    }

}

class ConfigurableZeroRLearnerFactory : ConfigurableLearnerFactory<ZeroRLearner, ZeroRConfiguration> {

    @Configurable.Register<ConfigurableZeroRLearnerFactory, ZeroRConfiguration>(
        ConfigurableZeroRLearnerFactory::class,
        ZeroRConfiguration::class
    )
    constructor(block : ZeroRConfiguration.() -> Unit = {}) : super(block)

    constructor(config : ZeroRConfiguration) : this(config.asReconfigureBlock())

    override fun create() : ZeroRLearner {
        return ZeroRLearner(configuration.targetIndex)
    }

}

class ZeroRConfiguration : Configuration() {

    @ConfigurationElement.WithMetadata("The column to treat as the training target")
    var targetIndex by ConfigurationItem { -1 }

}

sealed class ZeroRAccumulator<T> {

    abstract val representation: DataRepresentation<*, *, T>

    fun accumulate(row: DataRow) {
        accumulateValue(row.getValue(representation))
    }

    abstract fun accumulateValue(target : T)

    abstract fun <T2> evaluate(representation : DataRepresentation<*, *, T2>) : T2


}

class ZeroRNominalAccumulator(
    override val representation : NominalIndexRepresentation<*, *>
    ): ZeroRAccumulator<Int>() {

    val votes = Array(representation.dataType.numCategories) { 1 }

    var numVotes = representation.dataType.numCategories

    override fun accumulateValue(target : Int) {
        votes[target]++
        numVotes++
    }

    override fun <T> evaluate(representation : DataRepresentation<*, *, T>): T {
        var maxVote = 0
        var maxIndex = 0
        for ((index, value) in votes.iterator().enumerate()) {
            if (value > maxVote) {
                maxVote = value
                maxIndex = index
            }
        }
        return when (representation) {
            is NominalCanonicalRepresentation<*, *> -> representation.dataType[maxIndex]
            is NominalIndexRepresentation<*, *> -> maxIndex
            is EntropicRepresentation<*, *> -> maxIndex.toBigInteger()
            else -> UNREACHABLE_CODE("Protected by ensureOwnership")
        } as T
    }

}

class ZeroRNumericAccumulator(
    override val representation : NumericCanonicalRepresentation<*, *>
    ) : ZeroRAccumulator<Double>() {

    var total = 0.0

    var numVotes = 0

    override fun accumulateValue(target: Double) {
        total += target
        numVotes++
    }

    override fun <T> evaluate(representation : DataRepresentation<*, *, T>): T {
        return if (numVotes == 0)
            0.0 as T
        else
            (total / numVotes) as T
    }
}
