const val BENCHMARK_SIZE = 10000

private val array: Array<Int> = Array(BENCHMARK_SIZE) {
    it
}

fun arrayLoop(): Long {
    var sum = 0L
    for (e in array) {
        sum += e
    }
    return sum
}

fun main() {
    arrayLoop()
}