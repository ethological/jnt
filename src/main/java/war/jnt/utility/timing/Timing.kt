package war.jnt.utility.timing

class Timing {
    private var startTime: Long = 0
    private var endTime: Long = 0

    companion object {
        fun current(): Long {
            return System.currentTimeMillis()
        }
    }

    fun begin() {
        startTime = current()
    }

    fun end() {
        endTime = current()
    }

    fun calc(): Long {
        return (endTime - startTime)
    }
}