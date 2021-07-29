package māia.ml.learner.standard.hoeffdingtree.util

import kotlin.math.ln
import kotlin.math.sqrt

fun hoeffdingBound(
    range: Double,
    confidence: Double,
    n: Double
): Double {
    return sqrt(
        ((range * range) * ln(1.0 / confidence))
                /
                (2.0 * n)
    )
}
