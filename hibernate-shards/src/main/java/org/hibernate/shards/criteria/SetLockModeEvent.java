/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.criteria;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.shards.session.ShardedSessionException;

/**
 * Event that allows the {@link LockMode} of a {@link Criteria} to be set lazily.
 * @see Criteria#setLockMode(LockMode)
 *
 * @author maxr@google.com (Max Ross)
 */
class SetLockModeEvent implements CriteriaEvent {

  private enum MethodSig {
    LOCK_MODE,
    LOCK_MODE_AND_ALIAS
  }

  // tells us which overload of setLockMode to use
  private final MethodSig methodSig;

  // the LockMode we'll set on the Criteria when the event fires
  private final LockMode lockMode;

  // the alias for which we'll set the lock mode on the Criteria when the event
  // fires.  Can be null
  private final String alias;

  /**
   * Construct a SetLockModeEvent
   *
   * @param methodSig tells us which overload of setLockMode to use
   * @param lockMode the lock mode we'll set when the event fires
   * @param alias the alias for which we'll set the lcok mode when the event
   * fires.  Can be null.
   */
  private SetLockModeEvent(
      MethodSig methodSig,
      LockMode lockMode,
      /*@Nullable*/ String alias) {
    this.methodSig = methodSig;
    this.lockMode = lockMode;
    this.alias = alias;
  }

  /**
   * Construct a SetLockModeEvent
   *
   * @param lockMode the lock mode we'll set when the event fires
   */
  public SetLockModeEvent(LockMode lockMode) {
    this(MethodSig.LOCK_MODE, lockMode, null);
  }

  /**
   * Construct a SetLockModeEvent
   *
   * @param lockMode the lock mode we'll set when the event fires
   * @param alias the alias for which we'll set the lock mode
   * when the event fires
   */
  public SetLockModeEvent(LockMode lockMode, String alias) {
    this(MethodSig.LOCK_MODE_AND_ALIAS, lockMode, alias);
  }

  public void onEvent(Criteria crit) {
    switch (methodSig) {
      case LOCK_MODE:
        crit.setLockMode(lockMode);
        break;
      case LOCK_MODE_AND_ALIAS:
        crit.setLockMode(alias, lockMode);
        break;
      default:
        throw new ShardedSessionException(
            "Unknown constructor type for SetLockModeEvent: " + methodSig);
    }
  }
}
