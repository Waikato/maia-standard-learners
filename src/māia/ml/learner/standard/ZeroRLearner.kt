package māia.ml.learner.standard

import māia.configure.Configurable
import māia.configure.Configuration
import māia.configure.ConfigurationElement
import māia.configure.ConfigurationItem
import māia.configure.asReconfigureBlock
import māia.ml.dataset.DataRow
import māia.ml.dataset.DataStream
import māia.ml.dataset.headers.DataColumnHeaders
import māia.ml.dataset.headers.DataColumnHeadersView
import māia.ml.dataset.headers.ensureOwnership
import māia.ml.dataset.type.DataRepresentation
import māia.ml.dataset.type.EntropicRepresentation
import māia.ml.dataset.type.standard.Nominal
import māia.ml.dataset.type.standard.NominalCanonicalRepresentation
import māia.ml.dataset.type.standard.NominalIndexRepresentation
import māia.ml.dataset.type.standard.Numeric
import māia.ml.dataset.type.standard.NumericCanonicalRepresentation
import māia.ml.dataset.util.allColumnsExcept
import māia.ml.learner.AbstractLearner
import māia.ml.learner.factory.ConfigurableLearnerFactory
import māia.ml.learner.type.Classifier
import māia.ml.learner.type.LearnerType
import māia.ml.learner.type.NoMissingTargets
import māia.ml.learner.type.Regressor
import māia.ml.learner.type.SingleTarget
import māia.ml.learner.type.classLearnerType
import māia.ml.learner.type.intersectionOf
import māia.ml.learner.type.unionOf
import māia.util.datastructure.OrderedHashSet
import māia.util.enumerate
import māia.util.error.UNREACHABLE_CODE
import māia.util.property.classlevel.override

// The types of learner that ZeroR can be
val SingleTargetClassifier = intersectionOf(SingleTarget, Classifier, NoMissingTargets)
val SingleTargetRegressor = intersectionOf(SingleTarget, Regressor, NoMissingTargets)
val ZeroRUninitialisedLearnerType = unionOf(SingleTargetClassifier, SingleTargetRegressor)

/**
 * TODO
 */
class ZeroRLearner(val targetIndex : Int) : AbstractLearner<DataStream<*>>(
        ZeroRUninitialisedLearnerType,
        DataStream::class
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
            is Nominal<*, *, *, *> -> SingleTargetClassifier
            is Numeric<*, *> -> SingleTargetRegressor
            else -> throw Exception("ZeroR requires nominal or numeric target type, found $targetType")
        }

        // Create an accumulator for the target type
        accumulator = if (targetType is Nominal<*, *, *, *>)
            ZeroRNominalAccumulator(targetType.indexRepresentation)
        else
            ZeroRNumericAccumulator((targetType as Numeric<*, *>).canonicalRepresentation)

        return Triple(
            DataColumnHeadersView(headers, headers.allColumnsExcept(targetIndex)),
            DataColumnHeadersView(headers, OrderedHashSet(targetIndex)),
            type
        )
    }

    override fun performTrain(trainingDataset : DataStream<*>) {
        for (row in trainingDataset.rowIterator()) {
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
