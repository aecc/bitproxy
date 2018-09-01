package org.bitproxy

import java.net.InetAddress
import java.util
import java.util.Random

import net.tomp2p.dht.{FutureSend, PeerBuilderDHT, PeerDHT}
import net.tomp2p.futures.{BaseFuture, BaseFutureListener}
import net.tomp2p.p2p.{PeerBuilder, RequestP2PConfiguration, RoutingConfiguration}
import net.tomp2p.peers.{Number160, PeerAddress}
import net.tomp2p.rpc.ObjectDataReply

import scala.collection.JavaConverters._

object Main {

  case class Message(msg: String)
  case class Ask(msg: String)

  val DirectRoutingConfiguration = new RoutingConfiguration(1,1,5,1,1)
  val DirectRequestP2PConfiguration = new RequestP2PConfiguration(1, 5, 0)

  val RootPort = 4001
  val NumberClients = 10
  val MasterNodeId = newId(1)

  val random = new Random(42L)

  def main(args: Array[String]): Unit = {

    if (args.isEmpty) {
      println("Starting the server...")
      val mainPeer = new PeerBuilderDHT(
        new PeerBuilder(MasterNodeId)
          .ports(RootPort)
          .start
      ).start
      started(mainPeer)
      setupReplyHandler(Array(mainPeer))

    } else {
      println("Starting the clients...")
      startClientsAndBootstrap()
    }

  }

  private def startClientsAndBootstrap() : Unit = {

    println(s"Starting $NumberClients peers")
    (2 to NumberClients + 1).foreach {
      i =>
        val myPeer = new PeerBuilderDHT(new PeerBuilder(newId(i)).start()).start()
        val mainNodeAddress = util.Arrays.asList(new PeerAddress(MasterNodeId, InetAddress.getByName("127.0.0.1"), RootPort, RootPort))
        val futureBootstrap = myPeer.peer().bootstrap.bootstrapTo(mainNodeAddress).start
        futureBootstrap.awaitUninterruptibly()

        if (futureBootstrap.isSuccess) {

          setupReplyHandler(Array(myPeer))
          started(myPeer)
          printNeighbours(myPeer)

          i match {
            case 8 =>
              val futureSend = sendDirectly(myPeer, new Number160("0x9dacbfce34aac726dadd1ed574249f2fa9aa451d"), Ask(who(getPeerId(myPeer))))
              futureSend.addListener(new BaseFutureListener[FutureSend] {
                override def operationComplete(future: FutureSend): Unit = {
                  val values = future.rawDirectData2().values()
                  require(values.size() == 1)
                  val message = values.toArray.head.asInstanceOf[String]
                  Console.err.println(s"I'm ${myPeer.peerID} and I just got answered back with the message [$message]")
                }
                override def exceptionCaught(t: Throwable): Unit = ???
              })
            case 9 =>
              sendDirectly(myPeer, Number160.createHash(0), Message(hello(getPeerId(myPeer))))
            case _ =>
          }

          Thread.sleep(1000L)

        } else {
          println("Couldn't bootstrap node.")
        }

    }
  }

  private def getPeerId(peerDHT: PeerDHT): String = {
    peerDHT.peer().peerID().toString
  }

  private def started(peerDHT: PeerDHT): Unit = {
    println(s"Started: " + getPeerId(peerDHT))
  }

  private def newId(): Number160 = {
    Number160.createHash(random.nextInt())
  }

  private def newId(i: Int): Number160 = {
    new Number160(s"0x$i")
  }

  private def printNeighbours(thisPeer: PeerDHT): Unit = {
    val ns = thisPeer.peer().neighborRPC().getNeighbors(thisPeer.peerID(), 10)
    println(s"Neighbours: ")
    ns.asScala.foreach {
      n =>
        println(s"* ${n.peerId()}")
    }
  }

  private def sendDirectly(thisPeer: PeerDHT, destinationPeer: Number160, message: AnyRef): FutureSend = {
    thisPeer.send(destinationPeer)
      .routingConfiguration(DirectRoutingConfiguration)
      .requestP2PConfiguration(DirectRequestP2PConfiguration)
      .`object`(message).start()
  }

  private def hello(i: String): String = {
    s"Hello from me! $i"
  }

  private def who(i: String): String = {
    s"Who are you? I'm $i"
  }

  private def setupReplyHandler(peers: Array[PeerDHT]): Unit = {
    peers.foreach { peer =>
      peer.peer.objectDataReply(new ObjectDataReply() {
        @throws[Exception]
        override def reply(sender: PeerAddress, request: AnyRef): AnyRef = {
          request match {
            case Message(m) =>
              Console.err.println(s"I'm ${peer.peerID} and I just got the message [$m] from ${sender.peerId}")
              ""
            case Ask(m) =>
              Console.err.println(s"I'm ${peer.peerID} and I just got the question [$m] from ${sender.peerId}")
              s"I'm ${peer.peer().peerAddress().peerId()}"
            case _ =>
              Console.err.println("ERROR! WRONG MESSAGE!")
              ""
          }
        }
      })
    }
  }

}
