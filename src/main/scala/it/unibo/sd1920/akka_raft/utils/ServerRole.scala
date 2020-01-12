package it.unibo.sd1920.akka_raft.utils

object ServerRole {
  sealed abstract class ServerRole() {}
  case object LEADER extends ServerRole()
  case object FOLLOWER extends ServerRole()
  case object CANDIDATE extends ServerRole()
}
