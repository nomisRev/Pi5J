package  io.github.nomisrev

import com.pi4j.Pi4J
import com.pi4j.exception.LifecycleException
import com.pi4j.io.gpio.digital.*
import sun.misc.Signal

fun main() {
    val pi4j = Pi4J.newAutoContext()
    println("<-- The Pi4J DHT22 Kotlin Project -->")

    Signal.handle(Signal("INT")) {
        try {
            pi4j.shutdown()
        } catch (e: LifecycleException) {
            e.printStackTrace()
        }
        System.exit(2)
    }

    val input = pi4j.create(
            DigitalInput.newConfigBuilder(pi4j)
                .id("Data_In")
                .name("Data_In")
                .address(7)
                .pull(PullResistance.PULL_DOWN)
                .provider("gpoid")
                .build()
        )

    pi4j.shutdown()
}