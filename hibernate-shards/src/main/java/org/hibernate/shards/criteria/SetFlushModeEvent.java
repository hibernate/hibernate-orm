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
import org.hibernate.FlushMode;

/**
 * Event that allows the {@link FlushMode} of a {@link Criteria} to be set lazily.
 * @see Criteria#setFlushMode(FlushMode)
 *
 * @author maxr@google.com (Max Ross)
 */
class SetFlushModeEvent implements CriteriaEvent {

  // the flushMode we'll set on the Critiera when the event fires
  private final FlushMode flushMode;

  /**
   * Construct a SetFlushModeEvent
   *
   * @param flushMode the flushMode that
   * we'll set on the {@link Criteria} when the event fires.
   */
  public SetFlushModeEvent(FlushMode flushMode) {
    this.flushMode = flushMode;
  }

  public void onEvent(Criteria crit) {
    crit.setFlushMode(flushMode);
  }
}
