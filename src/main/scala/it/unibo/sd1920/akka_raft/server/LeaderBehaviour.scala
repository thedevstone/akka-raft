package it.unibo.sd1920.akka_raft.server

import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.cluster.Member
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, IdentifyServer, ServerIdentity}
import it.unibo.sd1920.akka_raft.utils.NodeRole

private trait LeaderBehaviour {
  this: ServerActor =>

  private var followersStatusMap: Map[Int,FollowerStatus] = Map()


  protected def leaderBehaviour: Receive = clusterBehaviour orElse {
    case _ =>

  }
}

private class FollowerStatus(nextIndexToSend: Int, lastMatchIndex: Int){
  assert(nextIndexToSend >= 0)
  assert(lastMatchIndex >= 0)
}
