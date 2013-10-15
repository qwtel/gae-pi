package pi.data

import pi.json.JSON
import pi.json.JSON._
import scala.beans.BeanProperty
import com.google.appengine.api.datastore.{Key, Entity}

case class Work(@BeanProperty var digitPos: Long,
                @BeanProperty var step: Long = 1,
                @BeanProperty var i: Long = 0,
                @BeanProperty var acc: Double = 0.0,
                @BeanProperty var pid: Double = 0.0,
                @BeanProperty var isComplete: Boolean = false,
                @BeanProperty var inProgress: Boolean = false,
                @BeanProperty var timestamp: Long = System.currentTimeMillis()) {

  def this() = this(0, 0, 0, 0.0, 0.0, false, false, 0)

  def toEntity(implicit key: Key) = {
    val work = new Entity("Work", key)
    work.setProperty("digitPos", digitPos)
    work.setProperty("step", step)
    work.setProperty("i", i)
    work.setProperty("acc", acc)
    work.setProperty("pid", pid)
    work.setProperty("isComplete", isComplete)
    work.setProperty("inProgress", inProgress)
    work.setProperty("timestamp", timestamp)
    work
  }

  def toJSON: String = JSON.makeJSON(this.toMap)

  /*
   * http://stackoverflow.com/questions/1226555/case-class-to-map-in-scala
   */
  private[this] def toMap = {
    (Map[String, Any]() /: this.getClass.getDeclaredFields) {
      (a, f) =>
        f setAccessible true
        a + (f.getName -> f.get(this))
    }
  }
}

object Work {

  def apply(entity: Entity): Work = {
    Work(
      entity.getProperty("digitPos").asInstanceOf[Long],
      entity.getProperty("step").asInstanceOf[Long],
      entity.getProperty("i").asInstanceOf[Long],
      entity.getProperty("acc").asInstanceOf[Double],
      entity.getProperty("pid").asInstanceOf[Double],
      entity.getProperty("isComplete").asInstanceOf[Boolean],
      entity.getProperty("inProgress").asInstanceOf[Boolean],
      entity.getProperty("timestamp").asInstanceOf[Long]
    )
  }

  def apply(s: String): Work = {
    val tree = JSON.parseJSON(s)
    Work(
      tree.digitPos,
      tree.step,
      tree.i,
      tree.acc,
      tree.pid,
      tree.isComplete,
      tree.inProgress,
      tree.timestamp
    )
  }
}
