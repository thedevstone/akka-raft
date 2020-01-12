package it.unibo.sd1920.akka_raft.model

class CommandLog[Command](private var entries: List[Entry[Command]]
                         ) {
  //Log indexes
  private var commitIndex = -1
  //index ->                    prevI lastI nextI
  //index -> |  0  |  1  |  2  |  3  |  4  |     |
  //term  -> |  0  |  0  |  1  |  1  |  2  |     |
  def size: Int = this.entries.size
  def getEntries: List[Entry[Command]] = this.entries
  def previousIndex: Option[Int] = lastIndex match {
    case 0 => None
    case n => Some(n - 1)
  }
  def nextIndex: Int = size

  def lastTerm: Int = this.entries.lastOption.map(t => t.term).getOrElse(0)
  def lastIndex: Int = this.entries.lastOption.map(t => t.index).getOrElse(-1)

  def contains(entry: Entry[Command]): Boolean = entries.contains(entry)

  def getPreviousEntry(entry: Entry[Command]): Option[Entry[Command]] = entry.index match {
    case 0 => None
    case n => Some(entries(n - 1))
  }

  def getLastEntry: Option[Entry[Command]] = {
    if (entries.isEmpty) return None
    Some(entries(lastIndex))
  }
  def getEntryAtIndex(index: Int): Option[Entry[Command]] = index match {
    case n if size > 0 && n < size => Some(entries(n)) //positive values
    case _ => None //negative values
  }

  def committedEntries: List[Entry[Command]] = entries.slice(0, commitIndex + 1)

  //Log Operation
  def commit(index: Int): Unit = this.commitIndex = index
  def getCommitIndex: Int = this.commitIndex
  def append(entry: Entry[Command]): Unit = entries = entries :+ entry
  def remove(index: Int): Unit = entries = entries.slice(0, index)

  def putElementAtIndex(entry: Entry[Command]): Boolean = {
    if (entry.index > size && entry.index < 0) return false

    if (entries.nonEmpty && entry.index < size) this.remove(entry.index)

    this.append(entry)
    true
  }
}

object CommandLog {
  def emptyLog[T](): CommandLog[T] = new CommandLog(List.empty)
  def populatedLog[T](initialLog: List[Entry[T]]): CommandLog[T] = new CommandLog(initialLog)
}

case class Entry[Command](command: Command,
                          term: Int,
                          index: Int,
                          requestId: Long
                         ) {
  assert(index >= 0) //Come in java
  assert(term >= 0)
}
