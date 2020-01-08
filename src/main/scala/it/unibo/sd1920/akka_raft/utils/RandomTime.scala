package it.unibo.sd1920.akka_raft.utils

import scala.util.Random

object RandomTime {
  def randomBetween(first:Int,seconds:Int) : Int =  first + Random.nextInt( (seconds - first ) + 1 )
}
