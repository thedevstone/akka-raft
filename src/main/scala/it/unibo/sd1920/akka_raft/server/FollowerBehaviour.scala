package it.unibo.sd1920.akka_raft.server

import it.unibo.sd1920.akka_raft.server.ServerActor.{ClientRequest, SchedulerTick}

private trait FollowerBehaviour {
  this: ServerActor =>

  protected def followerBehaviour: Receive = clusterBehaviour orElse {
    case SchedulerTick => context.become(clusterBehaviour orElse candidateBehaviour); startTimer()
    case ClientRequest(_, _) => ??? //redirect
  }

}
