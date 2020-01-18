package it.unibo.sd1920.akka_raft.model

class CommandLog[Command](
  private var entries: List[Entry[Command]]
) {
  //Log indexes
  private var commitIndex = -1
  //index ->                    prevI lastI nextI
  //index -> |  0  |  1  |  2  |  3  |  4  |     |
  //term  -> |  0  |  0  |  1  |  1  |  2  |     |
  /**
   * Get log size.
   *
   * @return the log size
   */
  def size: Int = this.entries.size

  /**
   * Get list of entries.
   *
   * @return the List of entries
   */
  def getEntries: List[Entry[Command]] = this.entries

  /**
   * Get previous index of the last entry.
   *
   * if 0 or -1 return None else n - 1
   *
   * @return
   */
  def previousIndex: Option[Int] = lastIndex match {
    case 0 => None
    case -1 => None
    case n => Some(n - 1)
  }

  /**
   * Get the next index, in other words next index is size of the list
   *
   * @return the next index
   */
  def nextIndex: Int = size

  /**
   * Get the term of the last entry.
   *
   * if log is empty return term = 0
   *
   * @return the last term
   */
  def lastTerm: Int = this.entries.lastOption.map(t => t.term).getOrElse(0)

  /**
   * Get the index of the last entry.
   *
   * if log is empty return index = -1
   *
   * @return the last index
   */
  def lastIndex: Int = this.entries.lastOption.map(t => t.index).getOrElse(-1)

  /**
   * Check if log contains an entry.
   *
   * @param entry the entry to check
   * @return true if entry is contained in log, false otherwise
   */
  def contains(entry: Entry[Command]): Boolean = entries.contains(entry)

  /**
   * Get the previous entry of a given entry.
   *
   * @param entry the entry
   * @return the previous entry
   */
  def getPreviousEntry(entry: Entry[Command]): Option[Entry[Command]] = entry.index match {
    case 0 => None
    case n => Some(entries(n - 1))
  }

  /**
   * Get all the entries between two given indexes.
   *
   * @param from the index of the fist entry
   * @param to   the index of the last entry
   * @return the list of entries
   */
  def getEntriesBetween(from: Int, to: Int): List[Entry[Command]] = {
    entries.slice(from + 1, to + 1)
  }

  /**
   * Check if a request id is present.
   *
   * @param reqID the request id
   * @return true if request id is present, false otherwise
   */
  def isReqIdPresent(reqID: Int): Boolean = entries.map(e => e.requestId).contains(reqID)

  /**
   * Get the last entry.
   *
   * @return the last entry
   */
  def getLastEntry: Option[Entry[Command]] = {
    if (entries.isEmpty) return None
    Some(entries(lastIndex))
  }

  /**
   * Get entry at a given index.
   *
   * returns None if the entry is not contained in log
   *
   * @param index the index
   * @return the entry
   */
  def getEntryAtIndex(index: Int): Option[Entry[Command]] = index match {
    case n if size > 0 && n < size => Some(entries(n)) //positive values
    case _ => None //negative values
  }

  /**
   * Get index given a request id.
   *
   * @param reqId the request id
   * @return the index of the request id
   */
  def getIndexFromReqId(reqId: Int): Option[Int] = {
    entries.filter(e => e.requestId == reqId).map(entry => entry.index).lastOption
  }

  /**
   * Check if the entry for this request id is committed.
   *
   * @param reqId the request id
   * @return true if the entry is committed
   */
  def isReqIdCommitted(reqId: Int): Boolean = {
    if (getIndexFromReqId(reqId).isEmpty) return false
    getIndexFromReqId(reqId).get <= commitIndex
  }

  /**
   * Get all committed entries.
   *
   * @return the list of committed entries
   */
  def committedEntries: List[Entry[Command]] = entries.slice(0, commitIndex + 1)

  //Log Operation
  /**
   * Commit all entry with index below the given index.
   *
   * @param index the index
   */
  def commit(index: Int): Unit = this.commitIndex = index

  /**
   * Get the commit index.
   *
   * @return the commit index
   */
  def getCommitIndex: Int = this.commitIndex

  /**
   * Append the entry at the end of the log.
   *
   * @param entry the entry
   */
  def append(entry: Entry[Command]): Unit = entries = entries :+ entry

  /**
   * Remove all the entry above the given index.
   *
   * @param index the index
   */
  def remove(index: Int): Unit = entries = entries.slice(0, index)

  /**
   * Put entry at the given index.
   *
   * @param entry the entry
   * @return true if put succeeded, false otherwise
   */
  def putElementAtIndex(entry: Entry[Command]): Boolean = {
    if (entry.index > size || entry.index < 0) return false
    if (entries.nonEmpty && entry.index < size) this.remove(entry.index)
    this.append(entry)
    true
  }
}

/**
 * Factory for command log creation.
 */
object CommandLog {
  def emptyLog[T](): CommandLog[T] = new CommandLog(List.empty)
  def populatedLog[T](initialLog: List[Entry[T]]): CommandLog[T] = new CommandLog(initialLog)
}

/**
 * Entry.
 *
 * @param command   the command
 * @param term      the term
 * @param index     the index
 * @param requestId the request id
 * @tparam Command command type
 */
case class Entry[Command](
  command: Command,
  term: Int,
  index: Int,
  requestId: Int
) {
  assert(index >= 0)
  assert(term >= 0)
}
