package it.unibo.sd1920.akka_raft.server

import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.cluster.Member
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, ClientRequest, IdentifyServer, SchedulerTick, ServerIdentity}
import it.unibo.sd1920.akka_raft.utils.NodeRole

private trait FollowerBehaviour {
  this: ServerActor =>

  protected def followerBehaviour: Receive = {
    case SchedulerTick => context.become(clusterBehaviour orElse candidateBehaviour); startTimer()
    case ClientRequest(_,_) => ??? //redirect
  }

}
