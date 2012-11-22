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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper methods related to {@link List}s.
 *
 * @author maxr@google.com (Max Ross)
 */
public class Lists {

  private Lists() { }

  /**
   * Construct a new {@link ArrayList}, taking advantage of type inference to
   * avoid specifying the type on the rhs.
   */
  public static <E> ArrayList<E> newArrayList() {
    return new ArrayList<E>();
  }

  /**
   * Construct a new {@link ArrayList} with the specified capacity, taking advantage of type inference to
   * avoid specifying the type on the rhs.
   */
  public static <E> ArrayList<E> newArrayListWithCapacity(int initialCapacity) {
    return new ArrayList<E>(initialCapacity);
  }

  /**
   * Construct a new {@link ArrayList} with the provided elements, taking advantage of type inference to
   * avoid specifying the type on the rhs.
   */
  public static <E> ArrayList<E> newArrayList(E... elements) {
    ArrayList<E> set = newArrayList();
    Collections.addAll(set, elements);
    return set;
  }

  /**
   * Construct a new {@link ArrayList} with the contents of the provided {@link Iterable}, taking advantage of type inference to
   * avoid specifying the type on the rhs.
   */
  public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
    ArrayList<E> list = newArrayList();
    for(E e : elements) {
      list.add(e);
    }
    return list;
  }

  /**
   * Construct a new {@link LinkedList}, taking advantage of type inference to
   * avoid specifying the type on the rhs.
   */
  public static <E> LinkedList<E> newLinkedList() {
    return new LinkedList<E>();
  }
}
