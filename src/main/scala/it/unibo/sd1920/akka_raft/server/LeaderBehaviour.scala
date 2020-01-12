package it.unibo.sd1920.akka_raft.server

import akka.actor.ActorRef
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.cluster.Member
import akka.routing.ActorRefRoutee
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.raft.AppendEntriesResult
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, IdentifyServer, ServerIdentity}
import it.unibo.sd1920.akka_raft.utils.NodeRole
import it.unibo.sd1920.akka_raft.server.LeaderBehaviour

private trait LeaderBehaviour {
  this: ServerActor =>

  private var followersStatusMap: Map[String,FollowerStatus] = Map()


  protected def leaderBehaviour: Receive = clusterBehaviour orElse {
    case AppendEntriesResult(res) => handleAppendResult(sender().path.name, res)
  }

  private def handleAppendResult(name: String,value: Boolean): Unit = name match {
    case _ => followersStatusMap = followersStatusMap + (name -> new FollowerStatus(1, 1))

  }
}


  class FollowerStatus(nextIndexToSend: Int, lastMatchIndex: Int){
  assert(nextIndexToSend >= 0)
  assert(lastMatchIndex >= 0)
}
