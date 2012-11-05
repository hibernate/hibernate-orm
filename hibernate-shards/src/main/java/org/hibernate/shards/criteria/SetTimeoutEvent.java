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

/**
 * Event that allows the timeout of a {@link Criteria} to be set lazily.
 * @see Criteria#setTimeout(int)   
 *
 * @author maxr@google.com (Max Ross)
 */
class SetTimeoutEvent implements CriteriaEvent {

  // the timeout we'll set on the Criteria when the event fires.
  private final int timeout;

  /**
   * Constructs a SetTimeoutEvent
   *
   * @param timeout the timeout we'll set on the {@link Criteria} when the
   * event fires.
   */
  public SetTimeoutEvent(int timeout) {
    this.timeout = timeout;
  }

  public void onEvent(Criteria crit) {
    crit.setTimeout(timeout);
  }
}
