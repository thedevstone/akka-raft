package it.unibo.sd1920.akka_raft.server

import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.cluster.Member
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.raft.RequestVoteResult
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, IdentifyServer, SchedulerTick, ServerIdentity}
import it.unibo.sd1920.akka_raft.utils.{NetworkConstants, NodeRole}

private trait CandidateBehaviour {
  this: ServerActor =>
  private var voteCounter: Int = 0
  protected def candidateBehaviour: Receive = clusterBehaviour orElse {
    case SchedulerTick => restart()
    case req: RequestVoteResult => handleVote(req)
    case _ =>
  }

  private def handleVote(result: RequestVoteResult): Unit  = result match {
    case RequestVoteResult(_,followerTerm) if followerTerm > currentTerm => becomingFollower(followerTerm)
    case RequestVoteResult(result,followerTerm) if result && followerTerm == currentTerm => voteCounter += 1
      if (voteCounter > NetworkConstants.numberOfServer) becomingLeader()
    case _ =>
  }

  private def becomingFollower(followerTerm: Int){
    currentTerm = followerTerm
    context.become(followerBehaviour)
    startTimer()
  }

  private def becomingLeader(): Unit ={
    context.become(leaderBehaviour)
    startTimer()
  }

  private def restart(){
    currentTerm += 1
    voteCounter = 0
    startTimer()
  }
}
