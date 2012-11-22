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
import org.hibernate.criterion.Projection;

/**
 * Event that allows the {@link Projection} of a {@link Criteria} to be set lazily.
 * @see Criteria#setProjection(Projection)
 *
 * @author maxr@google.com (Max Ross)
 */
class SetProjectionEvent implements CriteriaEvent {

  // the Projection we'll set on the Critiera when the event fires
  private final Projection projection;

  /**
   * Constructs a SetProjectionEvent
   *
   * @param projection the projection we'll set on the {@link Criteria} when the
   * event fires.
   */
  public SetProjectionEvent(Projection projection) {
    this.projection = projection;
  }


  public void onEvent(Criteria crit) {
    crit.setProjection(projection);
  }
}
