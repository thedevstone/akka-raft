package it.unibo.sd1920.akka_raft.server

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import akka.cluster.Cluster
import it.unibo.sd1920.akka_raft.server.StateMachine._
import it.unibo.sd1920.akka_raft.model.Entry
import it.unibo.sd1920.akka_raft.utils.RandomTime._


 private class StateMachine extends Actor  with ActorLogging {
  private def MIN_WORK_DELAY = 500
  private def MAX_WORK_DELAY = 2000
  protected[this] val cluster: Cluster = Cluster(context.system)
  private var statusMap: Map[Int, Int] = Map()
//  private var lastExecutedIndex:Int = 0

  override def receive: Receive = {
    case ExecuteCommand(Entry(command,_,index,_)) => {
      this.context.parent ! CommandResult(execute(index,command))//CoSA RISPONDO???

    }
  }
  //SE HAI IDEE PIù CARINE JUST DO IT!
   private def execute(index:Int, command: (String,Int)): (Boolean,Int) =  {//il bool serve per sapere se è andata a buon fine l'int il valore, il valore 0 per la put
     Thread.sleep(randomBetween(MIN_WORK_DELAY,MAX_WORK_DELAY))             // non dice nulla, il booleando dice se ha fallito, se il valore non c'è la mette
     command match{                                                         //altrimenti la mette lostesso. per la get il valore 0 con false indica il fallimento, 0 true successo c'è il valore 0
       case ("put",_) => if(this.statusMap.contains(index)) return (false,0) else
         this.statusMap = this.statusMap + (index -> 1) return (true,0)

       case ("get",_) => if (this.statusMap.contains(index)) return (true,this.statusMap.get(index).get) else return (false,0)
     }
   }

}

object StateMachine {

  sealed trait StateMachineMsg
  case class ExecuteCommand(entry :Entry) extends StateMachineMsg
  case class CommandResult(result:(Boolean,Int)) extends StateMachineMsg
  case class GetLastCommandExecuted(lastIndex:Int) extends StateMachineMsg

  def props: Props = Props(new StateMachine())

}