package it.unibo.sd1920.akka_raft.utils

import scala.util.Random

/**
 * Random utils.
 */
object RandomUtil {
  /**
   * Get a random between two numbers.
   *
   * @param first  first number
   * @param second second number
   * @return random number
   */
  def randomBetween(first: Int, second: Int): Int = first + Random.nextInt((second - first) + 1)
}
