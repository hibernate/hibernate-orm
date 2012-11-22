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

/**
 * Uniquely identifies a {@link ShardedCriteria} 
 *
 * @author maxr@google.com (Max Ross)
 */
public class CriteriaId {

  // the actual id
  private final int id;

  /**
   * Construct a CriteriaId
   *
   * @param id the int representation of the id
   */
  public CriteriaId(int id) {
    this.id = id;
  }

  /**
   * @return the int representation
   */
  public int getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CriteriaId)) {
      return false;
    }

    final CriteriaId criteriaId = (CriteriaId)o;

    if (id != criteriaId.id) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return id;
  }
}
