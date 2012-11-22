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
import org.hibernate.FetchMode;

/**
 * Event that allows the {@link FetchMode} of a {@link Criteria} to be set lazily.
 * @see Criteria#setFetchMode(String, FetchMode)
 *
 * @author maxr@google.com (Max Ross)
 */
class SetFetchModeEvent implements CriteriaEvent {

  // the association path that will be set on the Criteria
  private final String associationPath;

  // the FetchMode that will be set on the Criteria
  private final FetchMode mode;

  /**
   * Construct a SetFetchModeEvent
   *
   * @param associationPath the association path of the fetch mode
   * we'll set on the {@link Criteria} when the event fires.
   * @param mode the mode we'll set on the {@link Criteria} when the event fires.
   */
  public SetFetchModeEvent(String associationPath, FetchMode mode) {
    this.associationPath = associationPath;
    this.mode = mode;
  }

  public void onEvent(Criteria crit) {
    crit.setFetchMode(associationPath, mode);
  }
}
