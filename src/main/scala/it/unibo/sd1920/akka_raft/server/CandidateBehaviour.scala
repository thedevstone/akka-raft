package it.unibo.sd1920.akka_raft.server

import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.raft.{AppendEntries, RequestVoteResult}
import it.unibo.sd1920.akka_raft.server.ServerActor.SchedulerTick
import it.unibo.sd1920.akka_raft.utils.NetworkConstants

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
    val lastEntry: Option[Entry[BankCommand]] = serverLog.getLastEntry()

    servers.foreach(server => server._2 ! AppendEntries(currentTerm,if (lastEntry.isEmpty) None else serverLog.getPreviousEntry(lastEntry.get),lastEntry,lastCommittedIndex))
    context.become(leaderBehaviour)
    startTimer()
  }


  private def restart(){
    currentTerm += 1
    voteCounter = 0
    startTimer()
  }
}
