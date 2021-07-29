package māia.ml.learner.standard

import māia.configure.Configurable
import māia.configure.Configuration
import māia.configure.ConfigurationElement
import māia.configure.ConfigurationItem
import māia.configure.asReconfigureBlock
import māia.ml.dataset.DataRow
import māia.ml.dataset.DataStream
import māia.ml.dataset.WithColumnHeaders
import māia.ml.dataset.type.DataType
import māia.ml.dataset.type.Nominal
import māia.ml.dataset.type.Numeric
import māia.ml.dataset.util.buildRow
import māia.ml.dataset.util.convertToExternalUnchecked
import māia.ml.dataset.util.convertToInternalUnchecked
import māia.ml.dataset.view.readOnlyViewAllColumnsExcept
import māia.ml.dataset.view.readOnlyViewColumns
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
import māia.util.property.classlevel.override

/**
 * TODO
 */

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
    private lateinit var accumulator : ZeroRAccumulator

    override fun performInitialisation(
            headers : WithColumnHeaders
    ) : Triple<WithColumnHeaders, WithColumnHeaders, LearnerType> {
        // Get the type of the target column
        val targetType = headers.getColumnHeader(targetIndex).type

        // Determine what type of learner we are
        val type = when (targetType) {
            is Nominal -> SingleTargetClassifier
            is Numeric -> SingleTargetRegressor
            else -> throw Exception(
                    "ZeroR requires nominal or numeric target type, found $targetType"
            )
        }

        // Create an accumulator for the target type
        accumulator = if (targetType is Nominal)
            ZeroRNominalAccumulator(targetType)
        else
            ZeroRNumericAccumulator(targetType as Numeric<*>)

        return Triple(
                headers.readOnlyViewAllColumnsExcept(targetIndex),
                headers.readOnlyViewColumns(OrderedHashSet(targetIndex)),
                type
        )
    }

    override fun performTrain(trainingDataset : DataStream<*>) {
        for (row in trainingDataset.rowIterator()) {
            accumulator.accumulate(row.getColumn(targetIndex))
        }
    }

    override fun performPredict(row : DataRow) : DataRow {
        return predictOutputHeaders.buildRow(cacheHeaders = false) { _ ->
            accumulator.type.convertToInternalUnchecked(accumulator.evaluate())
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

sealed class ZeroRAccumulator {

    abstract val type: DataType<*, *>

    abstract fun accumulate(target : Any?)

    abstract fun evaluate() : Any

}

class ZeroRNominalAccumulator(override val type : Nominal<*>) : ZeroRAccumulator() {

    val votes = Array(type.numCategories) { 1 }

    var numVotes = type.numCategories

    override fun accumulate(target : Any?) {
        val category = type.convertToExternalUnchecked(target)
        val index = type.indexOf(category)
        votes[index]++
        numVotes++
    }

    override fun evaluate(): String {
        var maxVote = 0
        var maxIndex = 0
        for ((index, value) in votes.iterator().enumerate()) {
            if (value > maxVote) {
                maxVote = value
                maxIndex = index
            }
        }
        return type[maxIndex]
    }

}

class ZeroRNumericAccumulator(override val type : Numeric<*>) : ZeroRAccumulator() {

    var total = 0.0

    var numVotes = 0

    override fun accumulate(target: Any?) {
        total += type.convertToExternalUnchecked(target).toDouble()
        numVotes++
    }

    override fun evaluate(): Number {
        return if (numVotes == 0)
            0.0
        else
            total / numVotes
    }
}
