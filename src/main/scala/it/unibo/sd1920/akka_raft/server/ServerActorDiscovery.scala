package it.unibo.sd1920.akka_raft.server

import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.cluster.Member
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, IdentifyServer, ServerIdentity}
import it.unibo.sd1920.akka_raft.utils.NodeRole

/**
 * Node discovery behaviour in Akka Cluster.
 */
private trait ServerActorDiscovery {
  this: ServerActor =>

  protected def clusterDiscoveryBehaviour: Receive = {
    case MemberUp(member) => this.manageNewMember(member)
    case MemberDowned(member) =>
    case IdentifyServer(NodeRole.SERVER) => sender() ! ServerActor.ServerIdentity(self.path.name)
    case IdentifyServer(NodeRole.CLIENT) => sender() ! ClientActor.ServerIdentity(self.path.name)
    case ClientIdentity(name: String) => addClient(name)
    case ServerIdentity(name: String) => addServer(name)
  }

  /**
   * Manage a new member event.
   *
   * @param member the member that joined the cluster and become up
   */
  private def manageNewMember(member: Member): Unit = member match {
    case m if member.roles.contains("server") =>
      context.system.actorSelection(s"${m.address}/user/**") ! ServerActor.IdentifyServer(NodeRole.SERVER)
    case m if member.roles.contains("client") =>
      context.system.actorSelection(s"${m.address}/user/**") ! ClientActor.IdentifyClient(NodeRole.SERVER)
    case _ =>
  }

  /**
   * Handle ClientIdentity
   *
   * Add client to clients map when client is identified
   *
   * @param name client id
   */
  private def addClient(name: String): Unit = {
    this.clients = this.clients + (name -> sender())
  }

  /**
   * Handle ServerIdentity
   *
   * Add server to servers map when server is identified
   *
   * @param name server id
   */
  private def addServer(name: String): Unit = {
    this.servers = this.servers + (name -> sender())
    if (servers.size >= 5) {
      context.become(followerBehaviour)
      startTimeoutTimer()
    }
  }
}
