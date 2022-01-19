import java.lang.Double.sum
import java.lang.Exception
import java.lang.Integer.sum
import java.util.*

object MiscUtils {


    fun poisson(lambda: Double, r: Random): Int {
        if (lambda < 100.0) {
            var product = 1.0
            var sum = 1.0
            val threshold: Double = r.nextDouble() * Math.exp(lambda)
            var i = 1
            val max = Math.max(100, 10 * Math.ceil(lambda).toInt())
            while (i < max && sum <= threshold) {
                product *= lambda / i
                sum += product
                i++
            }
            return i - 1
        }
        val x: Double = lambda + Math.sqrt(lambda) * r.nextGaussian()
        return if (x < 0.0) {
            0
        } else Math.floor(x).toInt()
    }



    /**
     * Returns index of maximum element in a given array of doubles. First
     * maximum is returned.
     *
     * @param doubles the array of doubles
     * @return the index of the maximum element
     */
    fun  /*@pure@*/maxIndex(doubles: DoubleArray): Int {
        var maximum = 0.0
        var maxIndex = 0
        for (i in doubles.indices) {
            if (i == 0 || doubles[i] > maximum) {
                maxIndex = i
                maximum = doubles[i]
            }
        }
        return maxIndex
    }
}