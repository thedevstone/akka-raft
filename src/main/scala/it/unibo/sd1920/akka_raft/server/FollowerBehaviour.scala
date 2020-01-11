package it.unibo.sd1920.akka_raft.server

import akka.actor.ActorRef
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.cluster.Member
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, ClientRequest, IdentifyServer, SchedulerTick, ServerIdentity}
import it.unibo.sd1920.akka_raft.raft.{AppendEntries, RaftMessage, Redirect}

private trait FollowerBehaviour {
  this: ServerActor =>
  protected var leaderRef: ActorRef
  protected def followerBehaviour: Receive =  clusterBehaviour orElse  {
    case SchedulerTick => context.become(candidateBehaviour); startTimer()
    case ClientRequest(_,_) => sender() ! Redirect(Some(leaderRef))
    case AppendEntries(_,_,entry,_) if entry.isEmpty => startTimer() //il timer si resetta così?
    case AppendEntries(indexTerm, previousEntry, entry, leaderLastCommit) =>


  }

}
