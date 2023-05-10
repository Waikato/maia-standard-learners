package maia.ml.learner.standard.hoeffdingtree.observer

import maia.ml.dataset.DataRow
import maia.ml.dataset.type.DataType
import maia.ml.dataset.type.standard.Nominal
import maia.ml.learner.standard.hoeffdingtree.split.criterion.SplitCriterion
import maia.ml.learner.standard.hoeffdingtree.util.ObservedClassDistribution

/**
 * TODO: What class does.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
abstract class AttributeClassObserver<D: DataType<*, *>>(
    val attributeDataType: D,
    val classDataType: Nominal<*, *, *, *, *>
) {
    val numTargetClasses: Int
        get() = classDataType.numCategories

    var observedWeight: Double = 0.0
        private set

    fun observe(
        row: DataRow,
        classIndex: Int,
        weight: Double
    ) {
        observedWeight += weight
        observeAttributeForClass(row, classIndex, weight)
    }

    protected abstract fun observeAttributeForClass(
        row: DataRow,
        classIndex: Int,
        weight: Double
    )

    abstract fun probabilityOfAttributeValueGivenClass(
        row : DataRow,
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
