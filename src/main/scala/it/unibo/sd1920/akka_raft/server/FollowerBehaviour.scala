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

    case AppendEntries(_, previousEntry, entry, _) if (previousEntry.isEmpty) => startTimer(); sender() ! AppendEntriesResult(serverLog.putElementAtIndex(entry.get))
    case AppendEntries(_, previousEntry, _, _) if  !serverLog.contains(previousEntry.get) => startTimer(); sender() ! AppendEntriesResult(false)

    /*
        case AppendEntries(leaderTerm, previousEntry, entry, leaderLastCommit) if serverLog.contains(previousEntry.get)


        case appEntry: AppendEntries => handleNewAppend(appEntry, sender())*/
  }

  private def handleNewAppend(appEntry: AppendEntries, leaderAddress: ActorRef): Unit ={
    startTimer()
    leaderRef = leaderAddress
    if(currentTerm < appEntry.leaderTerm){
      if(checkHeartBeat(appEntry) ){
        if((appEntry.previousEntry.get.term == serverLog.lastTerm && appEntry.previousEntry.get.index == serverLog.lastIndex)){
          appendResult(result = true)
        } else {
          appendResult(result = false)
        }
      }

    } else {
      appendResult(result = false)
    }
  }

  private def checkHeartBeat(appEntry: AppendEntries): Boolean ={
    appEntry.previousEntry.nonEmpty && appEntry.entry.isEmpty
  }

  private def appendResult(result: Boolean): Unit ={
    leaderRef ! AppendEntriesResult(result)
  }

}
