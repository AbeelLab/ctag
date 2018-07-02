package nl.tudelft.pl2.data.builders

import nl.tudelft.pl2.data.Graph.Options
import org.apache.logging.log4j.Logger


trait ZoomBuilder {

  val LOGGER: Logger

  def build(): Unit

  def flush(): Unit

  def close(): Unit

  def registerNode(node: BuilderNode): Unit

  def expandNodeDataMapsByID(id: Int, data: ZoomBuilderData): Unit = {
    if (!data.nodeDataByID.contains(id)) {
      LOGGER.debug("Expand data with id {}", id)
      val chunks = data.prevLvlIndex.getIndexedChunksByNodeID(id)
      chunks.foreach(ic => if (!data.chunksRetrieved(ic.index)) {
        data.prevLvlReader.readDataChunkToBuilderNodes(ic.offset, ic.length).foreach(
          bn => {
            data.nodeDataByID.put(bn.id, bn)
            data.nodeDataByLayer.addBinding(bn.layer, bn.id)
          }

        )
        LOGGER.debug("Chunk {} was retrieved.", ic.index)
        data.chunksRetrieved(ic.index) = true
      })
    }
  }

  /**
    * Removes non-ORI or START and
    * double options.
    *
    * @param startOptions The options of the start Node.
    * @param hiMidOptions The options of the first middle Node.
    * @param loMidOptions The options of the second middle Node.
    * @return The merged options.
    */
  def mergeOptions(startOptions: Options, hiMidOptions: Options, loMidOptions: Options): Options = {
    // Todo: how to handle the start option?
    // Todo: find a way to do this without preferring loMidOptions.
    (startOptions ++ hiMidOptions ++ loMidOptions)
      .filter(p => p._1.equals("ORI") || p._1.equals("START"))
  }

}
