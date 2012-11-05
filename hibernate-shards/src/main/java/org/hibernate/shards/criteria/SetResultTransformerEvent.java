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
import org.hibernate.transform.ResultTransformer;

/**
 * Event that allows the {@link ResultTransformer} of a {@link Criteria} to be set lazily.
 * @see Criteria#setResultTransformer(ResultTransformer)
 *
 * @author maxr@google.com (Max Ross)
 */
class SetResultTransformerEvent implements CriteriaEvent {

  // the resultTransformer we'll set on the Critieria when the event fires
  private final ResultTransformer resultTransformer;

  /**
   * Constructs a SetResultTransformerEvent
   *
   * @param resultTransformer the resultTransformer we'll set on the {@link Criteria} when
   * the event fires.
   */
  public SetResultTransformerEvent(ResultTransformer resultTransformer) {
    this.resultTransformer = resultTransformer;
  }

  public void onEvent(Criteria crit) {
    crit.setResultTransformer(resultTransformer);
  }
}
