package it.unibo.sd1920.akka_raft.server

import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.cluster.Member
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, IdentifyServer, ServerIdentity}
import it.unibo.sd1920.akka_raft.utils.NodeRole

private trait CandidateBehaviour {
  this: ServerActor =>

  protected def candidateBehaviour: Receive = clusterBehaviour orElse {
    case _ =>
  }
}
