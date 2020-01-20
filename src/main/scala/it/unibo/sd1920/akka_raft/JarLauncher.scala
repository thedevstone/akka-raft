package it.unibo.sd1920.akka_raft

import it.unibo.sd1920.akka_raft.client.ClientActor
import it.unibo.sd1920.akka_raft.server.ServerActor

object JarLauncher {
  def main(args: Array[String]): Unit = {
    val nodeType: String = args(0)
    val id: String = args(1)
    val seed: String = if (args.length > 2) args(2) else ""
    args.length match {
      case 2 => nodeType match {
        case "client" => ClientActor.main(Seq(id).toArray)
        case "server" => ServerActor.main(Seq(id).toArray)
      }
      case 3 => nodeType match {
        case "client" => ClientActor.main(Seq(id, seed).toArray)
        case "server" => ServerActor.main(Seq(id, seed).toArray)
      }
    }

  }
}
