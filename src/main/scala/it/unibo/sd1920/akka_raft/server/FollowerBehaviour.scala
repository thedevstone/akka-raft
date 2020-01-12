package it.unibo.sd1920.akka_raft.server


import akka.actor.ActorRef
import akka.cluster.ClusterEvent.{MemberDowned, MemberUp}
import akka.cluster.Member
import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.model.BankStateMachine.BankCommand
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientIdentity, ClientRequest, IdentifyServer, SchedulerTick, ServerIdentity}
import it.unibo.sd1920.akka_raft.raft.{AckAppendEntries, AppendEntries, AppendEntriesResult, RaftMessage, Redirect}


private trait FollowerBehaviour {
  this: ServerActor =>
  private var leaderRef: ActorRef = _
  protected def followerBehaviour: Receive =  clusterBehaviour orElse {
    case SchedulerTick => context.become(candidateBehaviour); startTimer()
    case ClientRequest(_,_) => sender() ! Redirect(Some(leaderRef))

    case AppendEntries(_,_,entry,_) if entry.isEmpty => startTimer(); sender() ! AckAppendEntries
    case AppendEntries(leaderTerm, _, _, _) if leaderTerm < currentTerm => startTimer(); sender() ! AppendEntriesResult(false);

    case AppendEntries(_, previousEntry, entry, leaderLastCommit) if (previousEntry.isEmpty) => startTimer()
      callCommit(Math.min(serverLog.getCommitIndex, leaderLastCommit))
      sender() ! AppendEntriesResult(serverLog.putElementAtIndex(entry.get))
    case AppendEntries(_, previousEntry, _, _) if  !serverLog.contains(previousEntry.get) => startTimer()
      sender() ! AppendEntriesResult(false)
    case AppendEntries(_, _, entry, leaderLastCommit) =>  callCommit(Math.min(serverLog.getCommitIndex, leaderLastCommit))
      sender() ! AppendEntriesResult(serverLog.putElementAtIndex(entry.get))

  }
  def callCommit(index: Int) {
    serverLog.commit(index)
  }


}
