package pi.servlet

import com.google.appengine.api.datastore.Query.{CompositeFilterOperator, FilterOperator, FilterPredicate, SortDirection}
import com.google.appengine.api.datastore._
import java.util.concurrent.atomic.{AtomicLong, AtomicInteger}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import pi.data.Work
import scala.annotation.tailrec
import scala.util.Random
import FilterOperator._
import CompositeFilterOperator._
import SortDirection._
import scala.collection.mutable
import java.util

class WorkServlet extends HttpServlet {

  import WorkServlet._

  implicit def tuple2FilterPredicate(x: (String, FilterOperator, Any)): FilterPredicate = {
    new FilterPredicate(x._1, x._2, x._3)
  }

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) = {
    sendWork(resp)
  }

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) = {
    saveResults(req)
    sendWork(resp)
  }

  override def doPut(req: HttpServletRequest, resp: HttpServletResponse) = {
    saveResults(req)
  }

  def sendWork(resp: HttpServletResponse) {
    resp.setContentType("application/json")
    resp.getWriter.println(getWork().toJSON)
  }

  def saveResults(req: HttpServletRequest) {
    val lineOption = Option(req.getReader.readLine())

    lineOption map {
      line => Work(line)
    } map {
      case data: Work if data.isComplete => putCompleteInDatastore(data)
      case data: Work => putIncompleteInDatastore(data)
    }
  }

  private[this] def getWork(): Work = {
    val q = new Query("Work")
      .setAncestor(key)
      .addSort("timestamp", ASCENDING)
      .setFilter {
      or(
        and(
          ("isComplete", EQUAL, false),
          ("inProgress", EQUAL, false)
        ),
        and(
          ("timestamp", LESS_THAN, System.currentTimeMillis() - WorkTimeout * 1000),
          ("isComplete", EQUAL, false)
        )
      )
    }

    val entityOption = datastore.prepare(q).getFirst
    val entity = entityOption.getOrElse(generateWork())

    entity.setProperty("inProgress", true)
    entity.setProperty("timestamp", System.currentTimeMillis())
    datastore put entity

    val work = Work(entity)
    println("Sending work to client... " + work.digitPos)
    work
  }

  private[this] def generateWork(): Entity = {
    println("Adding work to datastore...")

    val batch = new java.util.ArrayList[Entity](BatchSize)
    for (i <- 1 to BatchSize) {
      val currentDigitPos = digitPos.getAndAdd(5)
      batch.add(Work(currentDigitPos).toEntity)
    }

    batch.get(0).setProperty("inProgress", true)
    datastore put batch

    println("Done")

    batch.get(0)
  }

  private[this] def putCompleteInDatastore(work: Work) = {
    println(work)
    putInDatastore(work)

    println {
      hexString(work.pid).substring(0, 10) + " " +
        toByteList(work.pid, 10).map {
          byte => Integer.toHexString(byte.toUnsignedInt)
        }
    }
  }

  private[this] def putIncompleteInDatastore(work: Work) = {
    println(work)
    putInDatastore(work)
  }

  private[this] def putInDatastore(work: Work): Option[Key] = {
    val q = new Query("Work")
      .setAncestor(key)
      .setFilter(("digitPos", EQUAL, work.digitPos))

    val entityOption = Option(datastore.prepare(q).asSingleEntity())
    entityOption map {
      entity => {
        entity.setPropertiesFrom(work.toEntity)
        datastore put entity
      }
    }
  }

  def hexString(x: Double, numDigits: Int = 16) = {
    @tailrec
    def step(i: Int, x: Double, acc: String = ""): String = {
      i match {
        case 0 => acc
        case _ => {
          val newX = 16.0 * (x - Math.floor(x))
          step(i - 1, newX, acc + Integer.toHexString(newX.toInt))
        }
      }
    }

    step(numDigits, x).toUpperCase
  }

  def toByteList(x: Double, numDigits: Int = 16) = {
    @tailrec
    def step(i: Int, x: Double, acc: List[Byte] = List()): List[Byte] = {
      i match {
        case 0 => acc
        case _ => {
          val newX = 16.0 * (x - Math.floor(x))
          step(i - 1, newX, newX.toInt.toByte :: acc)
        }
      }
    }

    @tailrec
    def combineTwo(list: List[Byte], acc: List[Byte] = List()): List[Byte] = {
      list match {
        case Nil => acc
        case b1 :: b2 :: rest => combineTwo(rest, ((b2 << 4) + b1).toByte :: acc)
        case b1 :: rest => throw new RuntimeException("Odd list size not supported")
      }
    }

    combineTwo(step(numDigits, x))
  }
}

object WorkServlet {

  val StartDigitPos = 1000000L
  val BatchSize = 10
  val WorkTimeout = 30

  implicit val key = KeyFactory.createKey("Calculation", "Pi")

  val datastore = DatastoreServiceFactory.getDatastoreService

  val maxDigitPos = {
    datastore.prepare(
      new Query("Work").setAncestor(key).addSort("digitPos", DESCENDING)
    ).getFirst.map(
      entity => entity.getProperty("digitPos").asInstanceOf[Long] + 5
    ).getOrElse(StartDigitPos)
  }

  println(maxDigitPos)

  val digitPos = new AtomicLong(maxDigitPos)

  implicit class MyPreparedQuery(val x: PreparedQuery) {

    import FetchOptions.Builder._

    val rand = new Random

    def getFirst: Option[Entity] = {
      val list = x.asList(withLimit(1))
      if (list.size == 0) None
      else Some(list.get(0))
    }

    def getRandom(max: Int): Option[Entity] = {
      val list = x.asList(withOffset(rand.nextInt(max)).limit(1))
      if (list.size == 0) None
      else Some(list.get(0))
    }
  }

  implicit class UnsignedByte(val x: Byte) {
    def toUnsignedInt: Int = {
      x.toInt & 0x000000ff
    }
  }

}
