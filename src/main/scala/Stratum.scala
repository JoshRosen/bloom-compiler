import com.typesafe.scalalogging.slf4j.Logging

class Stratum(rules: Iterable[Rule], tables: Iterable[Table[_]]) extends Logging {
  def fixpoint() {
    var reachedFixpoint = false
    do {
      val counts = tables.map(_.size)
      logger.debug("Computing fixpoint")
      reachedFixpoint = tables.zip(counts).forall { case (table, count) => table.size == count }
    } while (!reachedFixpoint)
    logger.debug("Finished computing fixpoint")
  }
}
