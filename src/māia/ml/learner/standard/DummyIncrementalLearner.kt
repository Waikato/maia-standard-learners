package māia.ml.learner.standard

import māia.configure.Configurable
import māia.configure.Configuration
import māia.configure.ConfigurationElement
import māia.configure.ConfigurationItem
import māia.configure.asReconfigureBlock
import māia.ml.dataset.DataRow
import māia.ml.dataset.DataStream
import māia.ml.dataset.WithColumnHeaders
import māia.ml.dataset.type.FiniteDataType
import māia.ml.dataset.util.buildRow
import māia.ml.dataset.util.randomInternal
import māia.ml.dataset.view.readOnlyViewAllColumnsExcept
import māia.ml.dataset.view.readOnlyViewColumns
import māia.ml.learner.AbstractLearner
import māia.ml.learner.factory.ConfigurableLearnerFactory
import māia.ml.learner.type.AnyLearnerType
import māia.ml.learner.type.LearnerType
import māia.ml.learner.type.SingleTarget
import māia.ml.learner.type.classLearnerType
import māia.ml.learner.type.intersectionOf
import māia.util.datastructure.OrderedHashSet
import māia.util.filter
import māia.util.joinToStringOrNull
import māia.util.map
import māia.util.property.classlevel.override


val FiniteTargets = AnyLearnerType.extend("Finite Targets") { _, outputHeaders ->
    outputHeaders
            .iterateColumnHeaders()
            .filter { it.type !is FiniteDataType }
            .map { "${it.name} is not a finite data-type" }
            .joinToStringOrNull("; ", "Non-finite headers: ")
}

val DummyIncrementalLearnerType = intersectionOf(FiniteTargets, SingleTarget)

/**
 * Test learner which performs no actual learning, instead returning random
 * values from the target attribute.
 *
 * @param targetIndex
 *          The index of the target attribute to generate predictions for.
 */
class DummyIncrementalLearner(
        val targetIndex : Int
) : AbstractLearner<DataStream<*>>(
        DummyIncrementalLearnerType,
        DataStream::class
) {
    private lateinit var targetType : FiniteDataType<*, *>

    override fun performInitialisation(headers : WithColumnHeaders) : Triple<WithColumnHeaders, WithColumnHeaders, LearnerType> {
        val targetType = headers.getColumnHeader(targetIndex).type

        if (targetType !is FiniteDataType<*, *>) throw Exception("Target is not finite")

        this.targetType = targetType

        return Triple(
                headers.readOnlyViewAllColumnsExcept(targetIndex),
                headers.readOnlyViewColumns(OrderedHashSet(targetIndex)),
                DummyIncrementalLearnerType
        )
    }

    override fun performTrain(trainingDataset : DataStream<*>) {
        // Does nothing
    }

    override fun performPredict(row : DataRow) : DataRow {
        return predictOutputHeaders.buildRow(cacheHeaders = false) { _ -> targetType.randomInternal() }
    }

    companion object {
        init {
            DummyIncrementalLearner::classLearnerType.override(
                DummyIncrementalLearnerType
            )
        }
    }

}

class ConfigurableDummyIncrementalLearnerFactory : ConfigurableLearnerFactory<DummyIncrementalLearner, DummyIncrementalLearnerConfiguration> {

    @Configurable.Register<ConfigurableDummyIncrementalLearnerFactory, DummyIncrementalLearnerConfiguration>(
        ConfigurableDummyIncrementalLearnerFactory::class,
        DummyIncrementalLearnerConfiguration::class
    )
    constructor(block : DummyIncrementalLearnerConfiguration.() -> Unit = {}) : super(block)

    constructor(config : DummyIncrementalLearnerConfiguration) : this(config.asReconfigureBlock())

    override fun create() : DummyIncrementalLearner {
        return DummyIncrementalLearner(configuration.target)
    }
}

class DummyIncrementalLearnerConfiguration : Configuration() {

    @ConfigurationElement.WithMetadata("The index of the target to learn")
    var target by ConfigurationItem { -1 }

}
