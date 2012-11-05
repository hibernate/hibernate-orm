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
import org.hibernate.Session;

/**
 * Factory that knows how to create a {@link Criteria} for a given {@link Session}
 *
 * @author maxr@google.com (Max Ross)
 */
public interface CriteriaFactory {

  /**
   * Create a {@link Criteria} for the given {@link Session}
   *
   * @param session the {@link Session}  to be used when creating the {@link Criteria}
   * @return a {@link Criteria} associated with the given {@link Session}
   */
  Criteria createCriteria(Session session);
}
