package nl.tudelft.pl2.data.builders

import nl.tudelft.pl2.data.Graph.{Coordinates, Options}
//scalastyle:off underscore.import
import nl.tudelft.pl2.representation.external._
//scalastyle:on underscore.import

import scala.collection.mutable

/**
  * Node that is currently being built by the [[ZeroZoomBuilder]].
  *
  * @param id       The id of the [[BuilderNode]].
  * @param layer    The layer of the [[BuilderNode]].
  * @param name     The name of the [[BuilderNode]].
  * @param content  The content of the [[BuilderNode]].
  * @param outgoing The links of the [[BuilderNode]].
  * @param options  The options of the [[BuilderNode]].
  */
class BuilderNode(val id: Int,
                  val name: String,
                  val layer: Int,
                  val content: String,
                  val incoming: mutable.Buffer[Int],
                  val outgoing: mutable.Buffer[Int],
                  val options: Options,
                  val genomes: Coordinates) {
  def nodify(): Node = new Node(id,
    name,
    layer,
    content,
    incoming.map(i => Edge(i, id)),
    outgoing.map(o => Edge(id, o)),
    options,
    genomes)
}

case class BuilderBubble(override val id: Int,
                         override val name: String,
                         override val layer: Int,
                         override val content: String,
                         cHi: Char, cLo: Char,
                         override val options: Options,
                         override val incoming: mutable.Buffer[Int],
                         end: Int)
  extends BuilderNode(id,
    name,
    layer,
    content,
    incoming,
    mutable.Buffer[Int](end),
    options,
    Map()) {
  override def nodify(): Node = Bubble(id,
    name,
    layer,
    content,
    cHi,
    cLo,
    incoming.map(i => Edge(i, id)),
    options,
    end)
}

case class BuilderIndel(override val id: Int,
                        override val name: String,
                        override val layer: Int,
                        override val content: String,
                        midContent: String,
                        override val options: Options,
                        override val incoming: mutable.Buffer[Int],
                        end: Int)
  extends BuilderNode(id,
    name,
    layer,
    content,
    incoming,
    mutable.Buffer[Int](end),
    options,
    Map()) {
  override def nodify(): Indel = Indel(id,
    name,
    layer,
    content,
    midContent,
    incoming.map(i => Edge(i, id)),
    options,
    end)
}

case class BuilderChain(override val id: Int,
                        override val layer: Int,
                        override val options: Options,
                        override val incoming: mutable.Buffer[Int],
                        override val outgoing: mutable.Buffer[Int])
  extends BuilderNode(id,
    "",
    layer,
    "",
    incoming,
    outgoing,
    options,
    Map()) {
  override def nodify(): Chain = Chain(id,
    layer,
    incoming.map(i => Edge(i, id)),
    outgoing.map(o => Edge(id, o)),
    options)
}
