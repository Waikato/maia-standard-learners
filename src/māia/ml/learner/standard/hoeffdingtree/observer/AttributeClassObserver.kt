package māia.ml.learner.standard.hoeffdingtree.observer

import māia.ml.dataset.type.DataType
import māia.ml.dataset.type.Nominal
import māia.ml.learner.standard.hoeffdingtree.split.criterion.SplitCriterion
import māia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
abstract class AttributeClassObserver<D: DataType<*, *>>(
    val attributeDataType: D,
    val classDataType: Nominal<*>
) {
    val numTargetClasses: Int
        get() = classDataType.numCategories

    var observedWeight: Double = 0.0
        private set

    fun observe(
        value: Any?,
        classIndex: Int,
        weight: Double
    ) {
        observedWeight += weight
        observeAttributeForClass(value, classIndex, weight)
    }

    protected abstract fun observeAttributeForClass(
        value: Any?,
        classIndex: Int,
        weight: Double
    )

    abstract fun probabilityOfAttributeValueGivenClass(
        value: Any?,
        classIndex: Int
    ): Double

    abstract fun getBestEvaluatedSplitSuggestion(
        criterion: SplitCriterion,
        preSplitDistribution: ObservedClassDistribution,
        attributeIndex: Int,
        binaryOnly: Boolean
    ): SplitSuggestion?

    abstract fun observeAttributeTarget(
        attVal: Double,
        target: Double
    )
}
