package io.github.nomisrev

import com.pi4j.context.Context
import com.pi4j.io.gpio.digital.DigitalInput
import com.pi4j.io.gpio.digital.DigitalInputConfigBuilder
import com.pi4j.io.gpio.digital.DigitalOutput
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder
import com.pi4j.io.gpio.digital.DigitalState
import com.pi4j.io.gpio.digital.DigitalStateChangeEvent
import com.pi4j.io.gpio.digital.DigitalStateChangeListener
import com.pi4j.io.gpio.digital.PullResistance
import com.pi4j.util.Console
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit


// MCU/Pi, initiate sensor OP
private const val BEGIN_READINGS_MILLS: Int = 20

// these two signal the sensor is prepared to send data
private const val PREPARE_DATA_LOW_PULSE_MICS: Int = 80
private const val PREPARE_DATA_HIGH_PULSE_MICS: Int = 80

// data signals '0 or '1' by pulse length
private const val ZERO_PULSE_MICS: Int = 27 // zero bit
private const val ONE_PULSE_MICS: Int = 70 //  one bit
private const val TOTAL_NUM_BITS: Int = 40
private const val RH_NUM_BITS: Int = 16
private const val T_NUM_BITS: Int = 16
private const val CKSUM_NUM_BITS: Int = 8

/**
 * If the commented use of the listener DataInGpioListener, this would
 * be a more normal implementation. However, the time to idle the gpio from output operation and
 * re-init the gpio as an input with a listener takes too long to complete
 * and DHT22  signals are lost and the device attempt to send data fails.
 *
 * So, for the time being  a simple polling implementation is used.
 */
class DHT22(private val pi4j: Context, private val console: Console, dataPinNum: Int, traceLevel: String) {
  private val oeGpio: DigitalOutput? = null
  private var dataPinNum = 0xff
  private val traceLevel: String
  private var logger: Logger = LoggerFactory.getLogger(DHT22::class.java)
  private var dataOut: DigitalOutput? = null
  private var dataIn: DigitalInput? = null

  private var outputConfig1: DigitalOutputConfigBuilder? = null
  private var inputConfig1: DigitalInputConfigBuilder? = null

  private val listener: DataInGpioListener? = null


  private var timeElapsed: Long = 0
  private var data_bits_started: Boolean = false

  private var dataBits: Long = 0
  private var bitCounter: Int = 0
  var endInstant: Long = 0

  private var awaitingHigh: Boolean


  init {
    this.dataPinNum = dataPinNum
    this.traceLevel = traceLevel
    this.awaitingHigh = true
    System.setProperty("org.slf4j.simpleLogger.log." + DHT22::class.java.name, this.traceLevel)
    this.logger.trace(">>> Enter: init")
    this.logger.trace("Data Pin  " + this.dataPinNum)
    //   this.listener = new DHT22.DataInGpioListener();
    this.outputConfig1 = DigitalOutput.newConfigBuilder(this.pi4j)
      .id("Data_Out")
      .name("Data_Out")
      .address(this.dataPinNum)
      .shutdown(DigitalState.HIGH)
      .initial(DigitalState.HIGH)
      .provider("gpoid")
    this.inputConfig1 = DigitalInput.newConfigBuilder(this.pi4j)
      .id("Data_In")
      .name("Data_In")
      .address(this.dataPinNum)
      .pull(PullResistance.OFF)
      .provider("gpoid")
    this.logger.trace("<<< Exit: init")
  }


  // Various logging routines commented out as their cose creates errors in reading the DHT22 waveform
  private fun createOutputPin() {
    //  this.logger.trace(">>> Enter: createOutputPin");
    if (this.dataOut == null) {
      this.dataOut = pi4j.create(this.outputConfig1)
    } else {
      dataOut!!.initialize(this.pi4j)
    }
    //   this.logger.trace("<<< Exit: createOutputPin");
  }

  private fun createInputPin() {
    //   this.logger.trace(">>> Enter: createInputPin");
    if (this.dataIn == null) {
      this.dataIn = pi4j.create(this.inputConfig1)
    } else {
      dataIn!!.initialize(this.pi4j)
    }

    /*    if(this.listener == null){
        this.listener =  new DHT22.DataInGpioListener();
    }
    this.dataIn.addListener(this.listener);
  */
    //   this.logger.trace("<<< Exit: createInputPin");
  }

  private fun idleOutputPin() {
    //    this.logger.trace(">>> Enter: idleOutputPin");
    dataOut!!.shutdown(this.pi4j)
    //      this.logger.trace("<<< Exit: idleOutputPin");
  }

  private fun idleInputPin() {
    //  this.logger.trace(">>> Enter: idleInputPin");
    if (this.dataIn != null) {
      //   this.dataIn.removeListener(this.listener);
      dataIn!!.shutdown(this.pi4j)
    }
    //   this.logger.trace("<<< Exit: idleInputPin");
  }


  fun readAndDisplayData() {
    run {
      val temperature: Double
      val humidity: Double

      var res: DoubleArray
      for (i in 0..<TOTAL_NUM_BITS) if ((read().also { res = it!! }) != null) {
        temperature = res[0]
        humidity = res[1]
        var sign = ""
        if ((temperature.toLong() and 0x8000L) > 0) {
          sign = "-"
        }
        console.println(
          """
    RH : $humidity  T : $sign${temperature * 1.8 + 32}
"""
        )
        break
      } else try {
        Thread.sleep(300)
      } catch (e: Exception) {
      }
    }

    // this.readAndDisplayDataLL();
  }

  private fun read(): DoubleArray? {
    logger!!.trace(">>> Enter: read")
    this.createOutputPin()
    dataOut!!.state(DigitalState.LOW)
    var now = System.nanoTime()
    while (System.nanoTime() - now < 2000000);
    dataOut!!.state(DigitalState.HIGH)
    this.idleOutputPin()

    this.createInputPin()
    now = System.nanoTime()
    var state = dataIn!!.state()
    var `val`: Long = 0
    var lastHi = now
    var read = 0
    while (System.nanoTime() - now < 10000000) {
      val next = dataIn!!.state()
      // this.logger.trace("Pin's state was :" + next);
      if (state != next) {
        if (next == DigitalState.HIGH) lastHi = System.nanoTime()
        else {
          `val` = (`val` shl 1)
          read++
          if ((System.nanoTime() - lastHi) / 1000 > 48) `val`++ //  long duration high, signifies a 1
        }
        state = next
      }
    }
    this.idleInputPin()
    var rval: DoubleArray? = null
    var temperature = 0.0
    var humidity = 0.0
    //should be 40 but the first few bits are often missed and often equal 0
    //But enough read to see if the checksum validate we read all pertinent bit;
    if (read >= 38) {
      val hi = ((`val` and 0xff00000000L) shr 32).toInt()
      val hd = ((`val` and 0xff000000L) shr 24).toInt()
      val ti = ((`val` and 0xff0000L) shr 16).toInt()
      val td = ((`val` and 0xff00L) shr 8).toInt()
      val cs = (`val` and 0xffL).toInt()
      //checksum validation
      if (cs == ((hi + hd + ti + td) and 0xff)) {
        temperature =
          ((((ti and 0x7f) shl 8) + td) / 10.0) * (if ((ti and 0x80) != 0) -1 else 1) // check if sign bit set (neg) multi by -1
        humidity = (((hi shl 8) + hd) / 10).toDouble()
        rval = doubleArrayOf(temperature, humidity)

        logger!!.trace("Decoded values   T: " + temperature + "/" + ((temperature * 1.8) + 32) + "   RH : " + humidity)
      } else {
        logger!!.trace("Checksum failed val  : $`val`") //will return null and read() will be called again
      }
    }
    logger!!.trace("<<< Exit")
    return rval
  }

  /**
   * If the commented use of the listener DataInGpioListener, this would
   * be a more normal implementation. However, the time to idle the gpio from output operation and
   * re-init the gpio as an input with a listener takes too long to complete
   * and DHT22  signals are lost and the device attempt to send data fails.
   *
   * So, for the time being  a simple polling implementation is used.
   */
  private fun readAndDisplayDataLL() {
    logger!!.trace(">>> Enter: readAndDisplayDataLL")
    val now = System.nanoTime()
    this.idleInputPin() // possibly exists from previous OP
    this.createOutputPin()
    dataOut!!.low()
    while (System.nanoTime() - now < 2000000) {
    }
    dataOut!!.high()
    this.idleOutputPin()
    this.createInputPin()

    logger!!.trace("<<< Exit: readAndDisplayDataLL")
  }

  private fun process_timeCalc() {
    val durationMics = this.timeElapsed / 1000
    // this.logger.trace(" >>> Enter: process_timeCalc  duration MICs " + durationMics  + " bit counter : " +  this.bitCounter);
    if ((durationMics > ZERO_PULSE_MICS - 5) && (durationMics < ZERO_PULSE_MICS + 5)) {
      // zero bit
      //  this.logger.trace("zero bit ");
      this.dataBits = (this.dataBits shl 1) or 0L
      bitCounter++
    } else if ((durationMics > ONE_PULSE_MICS - 5) && (durationMics < ONE_PULSE_MICS + 5)) {
      // one bit
      //  this.logger.trace("one bit ");
      this.dataBits = (this.dataBits shl 1) or 1L
      bitCounter++
    }

    //  this.logger.trace("this.bitCounter " + this.bitCounter);
    if (this.bitCounter >= TOTAL_NUM_BITS) {
      this.data_bits_started = false
      // display the data
      //TODO validate cksum
      val rh = (this.dataBits shr (T_NUM_BITS + CKSUM_NUM_BITS)) / 10
      val t = ((this.dataBits shr CKSUM_NUM_BITS) and 0xffffL) / 10
      // ready for next calculation
      this.data_bits_started = true
      this.bitCounter = 0
      this.dataBits = 0
      var sign = ""
      if ((t and 0x80L) > 0) {
        sign = "-"
      }
      logger!!.trace("RH = " + rh + "   t = : " + sign + ((t * 1.8) + 32))
    }

    // this.logger.trace(" <<< Exit: process_timeCalc ");
  }

  protected fun sleepTimeMilliS(milliSec: Int) {
    val tu = TimeUnit.MILLISECONDS
    try {
      tu.sleep(milliSec.toLong())
    } catch (e: InterruptedException) {
      throw RuntimeException(e)
    }
  }


  /* Listener class        */
  private class DataInGpioListener : DigitalStateChangeListener {
    var startInstant: Instant? = null
    var timeElapsed: Duration? = null
    var data_bits_started: Boolean = false

    var dataBits: Long = 0
    var bitCounter: Int = 0

    init {
      println("DataInGpioListener ctor")
      /* Runtime.getRuntime().addShutdownHook(new Thread() {
          public void run() {
              System.out.println("DataInGpioListener: Performing ctl-C shutdown");
              // Thread.dumpStack();
          }
      });
      */
    }

    override fun onDigitalStateChange(event: DigitalStateChangeEvent<*>) {
      println(">>> Enter: onDigitalStateChange")
      this.startInstant = Instant.now() //  init Duration because first event is Low,
      // this is in prep to begin sending high----low transition to signify 0 or 1
      if (event.state() == DigitalState.HIGH) {
        this.startInstant = Instant.now()
        println("onDigitalStateChange Pin went High")
      } else if (event.state() == DigitalState.LOW) {
        val endInstant = Instant.now()
        println("onDigitalStateChange Pin went Low")
        val elapsed = Duration.between(startInstant, endInstant)
        this.timeElapsed = elapsed
        println("timeElapsed time MicS " + elapsed.nano / 1000)
        this.process_timeCalc()
      } else {
        println("Strange event state  " + event.state())
      }
      println("<<< Exit: onDigitalStateChange")
    }

    private fun process_timeCalc() {
      val durationMics = (timeElapsed!!.nano / 1000).toLong()
      println(" >>> Enter: process_timeCalc  duration MICs " + durationMics + " bit counter : " + this.bitCounter)
      if ((durationMics > ZERO_PULSE_MICS - 5) && (durationMics < ZERO_PULSE_MICS + 5)) {
        // zero bit
        println("zero bit ")
        this.dataBits = (this.dataBits shl 1) or 0L
      } else if ((durationMics > ONE_PULSE_MICS - 5) && (durationMics < ONE_PULSE_MICS + 5)) {
        // one bit
        println("one bit ")
        this.dataBits = (this.dataBits shl 1) or 1L
      }
      bitCounter++
      println("this.bitCounter " + this.bitCounter)

      if (this.bitCounter >= TOTAL_NUM_BITS) {
        this.data_bits_started = false
        // display the data
        //TODO validate cksum
        val rh = (this.dataBits shr (T_NUM_BITS + CKSUM_NUM_BITS)) / 10
        val t = ((this.dataBits shr CKSUM_NUM_BITS) and 0xffffL) / 10
        // ready for next calculation
        this.data_bits_started = true
        this.bitCounter = 0
        this.dataBits = 0
        println("RH = " + rh + "   t = : " + ((t * 1.8) + 32))
      }

      println(" <<< Exit: process_timeCalc ")
    }
  }
}