package māia.ml.learner.standard.hoeffdingtree.util

import māia.util.asIterable
import māia.util.enumerate
import māia.util.filter
import māia.util.map
import māia.util.mapInPlaceIndexed
import māia.util.maxIndex
import māia.util.maxReducer
import māia.util.nonZeroCount
import kotlin.math.log2

/**
 * The distribution of weights seen per-class by an observer.
 *
 * @author Corey Sterling (csterlin at waikato dot ac dot nz)
 */
inline class ObservedClassDistribution(
    val array : DoubleArray
): Iterable<Double> {

    constructor(size: Int): this(DoubleArray(size))

    val size: Int
        get() = array.size

    val maxClassIndex: Int
        get() {
            val maxIndex = array.maxIndex
            return if (array[maxIndex] == 0.0)
                -1
            else
                maxIndex
        }

    val nonZeroLength: Int
        get() = array.indexOfLast { it != 0.0 } + 1

    val isPure: Boolean
        get() = array.nonZeroCount < 2

    val promise: Double
        get() {
            val total = array.sum()
            return if (total > 0.0)
                total - array.reduce(maxReducer<Double>()::reduce)
            else
                0.0
        }

    val totalWeightSeen: Double
        get() = array.sum()

    val entropy: Double
        get() {
            var entropy = 0.0
            var sum = 0.0

            for (d in array) if (d > 0.0) {
                entropy -= d * log2(d)
                sum += d
            }

            return if (sum > 0.0)
                (entropy + sum * log2(sum)) / sum
            else
                0.0
        }

    operator fun get(index: Int): Double {
        return array[index]
    }

    operator fun set(index: Int, value: Double) {
        array[index] = value
    }

    operator fun plusAssign(other: ObservedClassDistribution) {
        array.mapInPlaceIndexed { i, d -> d + other[i] }
    }

    fun clone(): ObservedClassDistribution {
        return ObservedClassDistribution(array.clone())
    }

    override fun toString() : String {
        return array.joinToString(prefix = "[", postfix = "]")
    }

    override fun iterator() : Iterator<Double> {
        return array.iterator()
    }
}

val Array<ObservedClassDistribution>.entropy: Double
    get() {
        val distWeights = DoubleArray(this.size) { this[it].totalWeightSeen }
        val totalWeight = distWeights.sum()
        val entropy = this
            .enumerate()
            .map { (i, dist) -> distWeights[i] * dist.entropy }
            .asIterable()
            .sum()
        return entropy / totalWeight
    }

fun Array<ObservedClassDistribution>.numSubsetsGreaterThanFrac(
    minFrac: Double
): Int {
    val distWeights = DoubleArray(this.size) { this[it].totalWeightSeen }
    val totalWeight = distWeights.sum()
    return distWeights
        .iterator()
        .filter { (it / totalWeight) > minFrac }
        .asIterable()
        .count()
}
