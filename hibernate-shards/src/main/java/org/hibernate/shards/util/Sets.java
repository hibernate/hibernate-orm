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

package org.hibernate.shards.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper methods related to {@link Set}s.
 *
 * @author maxr@google.com (Max Ross)
 */
public class Sets {
  private Sets() {}

  /**
   * Construct a new {@link HashSet}, taking advantage of type inference to
   * avoid specifying the type on the rhs.
   */
  public static <E> HashSet<E> newHashSet() {
    return new HashSet<E>();
  }

  /**
   * Construct a new {@link HashSet} with the provided elements, taking advantage of type inference to
   * avoid specifying the type on the rhs.
   */
  public static <E> HashSet<E> newHashSet(E... elements) {
    HashSet<E> set = newHashSet();
    Collections.addAll(set, elements);
    return set;
  }

  /**
   * Construct a new {@link HashSet} with the contents of the provided {@link Iterable}, taking advantage of type inference to
   * avoid specifying the type on the rhs.
   */
  public static <E> HashSet<E> newHashSet(Iterable<? extends E> elements) {
    HashSet<E> set = newHashSet();
    for(E e : elements) {
      set.add(e);
    }
    return set;
  }
}
