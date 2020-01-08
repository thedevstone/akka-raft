package it.unibo.sd1920.akka_raft.model

import scala.collection.mutable.ArrayBuffer

case class CommandLog[Command](
                                private var entries: ArrayBuffer[Entry[Command]],
                                committedIndex: Int
){
  def length: Int = this.entries.length
  def commands: Seq[Command] = this.entries.map(entry => entry.command)
  def terms: Seq[Int] = this.entries.map(entry => entry.term)
  def lastTerm: Int = this.entries.lastOption.map(t => t.term).getOrElse(0)
  def lastIndex: Int = this.entries.lastOption.map(t => t.index).getOrElse(0)
  def consistencyCheck(previousEntry: Entry[Command], entryToAppend: Entry[Command]): Boolean = previousEntry match  {
    case Entry(_, _, _, _) if !this.entries.contains(previousEntry) => false
    case Entry(_, prevTerm , prevIndex, _) if entries(prevIndex).term != prevTerm =>
      entries = entries.takeWhile(entry => entry.index < prevIndex)
      false
    case Entry(_, prevTerm, prevIndex, _) if entries(prevIndex).term == prevTerm =>
      entries(entryToAppend.index) = entryToAppend
      true
    case Entry(_, _, _, _) => entries.+=(entryToAppend)
      true
  }
}
case class Entry[T](
  command: T,
  term: Int,
  index: Int,
  requestId: Long
) {
  assert(index > 0) //Come in java
  assert(term > 0)
  def previousTerm: Int = term - 1
  def previousIndex: Int = index - 1
}
