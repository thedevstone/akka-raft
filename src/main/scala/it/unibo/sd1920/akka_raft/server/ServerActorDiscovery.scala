package it.unibo.sd1920.akka_raft.server

import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.cluster.Member
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, IdentifyServer, ServerIdentity}
import it.unibo.sd1920.akka_raft.utils.NodeRole

private trait ServerActorDiscovery {
  this: ServerActor =>

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberUp], classOf[MemberDowned])
    cluster.registerOnMemberUp({
    })
  }

  protected def clusterBehaviour: Receive = {
    case MemberUp(member) => this.manageNewMember(member)
    case MemberDowned(member) =>
    case IdentifyServer(NodeRole.SERVER) => sender() ! ServerActor.ServerIdentity(self.path.name)
    case IdentifyServer(NodeRole.CLIENT) => sender() ! ClientActor.ServerIdentity(self.path.name)
    case ServerIdentity(name: String) => this.servers = this.servers + (name -> sender())
    case ClientIdentity(name: String) => this.clients = this.clients + (name -> sender())
  }

  private def manageNewMember(member: Member): Unit = member match {
    case m if member.roles.contains("server") =>
      context.system.actorSelection(s"${m.address}/user/**") ! ServerActor.IdentifyServer(NodeRole.SERVER)
    case m if member.roles.contains("client") =>
      context.system.actorSelection(s"${m.address}/user/**") ! ClientActor.IdentifyClient(NodeRole.SERVER)
    case _ =>
  }
}
