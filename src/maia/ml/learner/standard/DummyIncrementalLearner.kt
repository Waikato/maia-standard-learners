package maia.ml.learner.standard

import kotlinx.coroutines.flow.collect
import maia.configure.Configurable
import maia.configure.Configuration
import maia.configure.ConfigurationElement
import maia.configure.ConfigurationItem
import maia.configure.asReconfigureBlock
import maia.ml.dataset.AsyncDataStream
import maia.ml.dataset.DataRow
import maia.ml.dataset.DataStream
import maia.ml.dataset.headers.DataColumnHeaders
import maia.ml.dataset.headers.DataColumnHeadersView
import maia.ml.dataset.headers.ensureOwnership
import maia.ml.dataset.type.DataRepresentation
import maia.ml.dataset.type.FiniteDataType
import maia.ml.dataset.util.allColumnsExcept
import maia.ml.learner.AbstractLearner
import maia.ml.learner.factory.ConfigurableLearnerFactory
import maia.ml.learner.type.AnyLearnerType
import maia.ml.learner.type.LearnerType
import maia.ml.learner.type.SingleTarget
import maia.ml.learner.type.classLearnerType
import maia.ml.learner.type.intersectionOf
import maia.util.datastructure.OrderedHashSet
import maia.util.filter
import maia.util.joinToStringOrNull
import maia.util.map
import maia.util.property.classlevel.override


val FiniteTargets = AnyLearnerType.extend("Finite Targets") { _, outputHeaders ->
    outputHeaders
            .iterator()
            .filter { it.type !is FiniteDataType<*, *, *> }
            .map { it.toString() }
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
) : AbstractLearner<AsyncDataStream<*>>(
        DummyIncrementalLearnerType,
        AsyncDataStream::class
) {
    private lateinit var targetType : FiniteDataType<*, *, *>

    override fun performInitialisation(
        headers : DataColumnHeaders
    ) : Triple<DataColumnHeaders, DataColumnHeaders, LearnerType> {
        val targetType = headers[targetIndex].type

        if (targetType !is FiniteDataType<*, *, *>) throw Exception("Target is not finite")

        this.targetType = targetType

        return Triple(
            DataColumnHeadersView(headers, headers.allColumnsExcept(targetIndex)),
            DataColumnHeadersView(headers, OrderedHashSet(targetIndex)),
            DummyIncrementalLearnerType
        )
    }

    override suspend fun performTrain(trainingDataset : AsyncDataStream<*>) {
        // Just drains the stream, no actual training is performed
        trainingDataset.rowFlow().collect {
            // Do nothing
        }
    }

    override fun performPredict(row : DataRow) : DataRow {
        return object : DataRow {
            override val headers : DataColumnHeaders = predictOutputHeaders
            override fun <T> getValue(
                representation : DataRepresentation<*, *, out T>
            ) : T = headers.ensureOwnership(representation) {
                val entropic = targetType.entropicRepresentation
                return convert(entropic.random(), entropic)
            }
        }
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
