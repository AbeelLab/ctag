package nl.tudelft.pl2.representation.external.chunking

import nl.tudelft.pl2.representation.external.Edge

/**
  * Class that represents chunk of links.
  *
  * @param chunkLinkId The ID of the chunkLink
  * @param from        The [[Chunk]] this [[ChunkLink]] links from
  * @param to          The [[Chunk]] this [[ChunkLink]] links to
  * @param info        Info map containing METADATA about the link
  */
abstract class ChunkLink(chunkLinkId: Int,
                         from: Chunk,
                         to: Chunk,
                         info: Map[String, String]) {

  /**
    * Assure both ends of the link, so
    * both Chunks are in-memory and a link
    * is present to represent this ChunkLink.
    *
    * @return Link representing a connection
    *         between the from and to chunks.
    */
  def link(): Edge

  /**
    * Assures the presence of both ends of
    * the link in-memory. Both chunks will
    * have to be cached, after which this
    * method returns `true`. If something
    * prevents the chunks to be cached, this
    * returns `false`.
    *
    * @return `true` when both ends are present,
    *         `false` otherwise.
    */
  def assurePresence(): Boolean

}
