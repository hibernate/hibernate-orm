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
 * Event that allows a Subcriteria to be lazily added to a Criteria.
 *
 * @author maxr@google.com (Max Ross)
 */
public class CreateSubcriteriaEvent implements CriteriaEvent {

  private final SubcriteriaFactory subcriteriaFactory;
  private final ShardedSubcriteriaImpl.SubcriteriaRegistrar subcriteriaRegistrar;

  public CreateSubcriteriaEvent(SubcriteriaFactory subcriteriaFactory, ShardedSubcriteriaImpl.SubcriteriaRegistrar subcriteriaRegistrar) {
    this.subcriteriaFactory = subcriteriaFactory;
    this.subcriteriaRegistrar = subcriteriaRegistrar;
  }

  public void onEvent(Criteria crit) {
    subcriteriaRegistrar.establishSubcriteria(crit, subcriteriaFactory);
  }
}
