package it.unibo.sd1920.akka_raft.model

import scala.collection.mutable.ArrayBuffer

class CommandLog(
  private var entries: ArrayBuffer[Entry]
){
  //Log indexes
  private var commitIndex = -1
  //index ->        prevI             lastI nextI
  //index -> |  X  |  1  |  2  |  3  |  4  |     |
  //term  -> |  X  |  0  |  1  |  1  |  2  |     |
  def length: Int = this.entries.size

  def commands: Seq[(String,Int)] = this.entries.map(entry => entry.command)
  def terms: Seq[Int] = this.entries.map(entry => entry.term)

  def previousIndex: Int = lastIndex match {
    case 1 => 1
    case n => n - 1
  }
  def nextIndex: Int = length + 1

  def previousEntry(entry: Entry): Entry = {
    if (this.entries.contains(entry))this.entries(entry.index-1)

    this.entries(entry.index-1)
  }

  def lastTerm: Int = this.entries.lastOption.map(t => t.term).getOrElse(0)
  def lastIndex: Int = this.entries.lastOption.map(t => t.index).getOrElse(1)

  def getEntryAtIndex(index: Int): Option[Entry] = index match {
    case 0 => None              //zero value
    case n => Some(entries(n))  //positive values
    case _ => None              //negative values
  }

  def committedEntries: Seq[Entry] = entries.slice(0, commitIndex)

  //Conssitency check
  def consistencyCheck(previousEntry: Entry, entryToAppend: Entry): Boolean = previousEntry match  {
    case Entry(_, _, _, _) if !this.entries.contains(previousEntry) => false
    case Entry(_, prevTerm , prevIndex, _) if entries(prevIndex-1).term != prevTerm =>
      entries = entries.takeWhile(entry => entry.index < prevIndex)
      false
    case Entry(_, prevTerm, prevIndex, _) if entries(prevIndex-1).term == prevTerm =>
      entries(entryToAppend.index) = entryToAppend
      true
    case Entry(_, _, _, _) => append(entryToAppend)
      true
  }

  //Log Operation
  def commit(index: Int): Unit = this.commitIndex = index
  def getCommitIndex: Int = this.commitIndex
  def append(entry: Entry): Unit = entries += entry
}

object CommandLog {
  def emptyLog[T](): CommandLog = new CommandLog(ArrayBuffer.empty)
  def populatedLog[T](initialLog: ArrayBuffer[Entry]): CommandLog = new CommandLog(initialLog)
}

case class Entry(
  command: (String,Int),
  term: Int,
  index: Int,
  requestId: Long
) {
  assert(index >= 0 /* || index = -1*/ ) //Come in java
  assert(term >= 0)

}
