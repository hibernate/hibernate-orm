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
import java.util.Iterator;
import java.util.List;

/**
 * Helper methods related to {@link Iterable}s.
 *
 * @author maxr@google.com (Max Ross)
 */
public class Iterables {

  private Iterables() {}

  /**
   * @return an Iterable that allows you to iterate over the contents of
   * all the iterables passed in.
   */
  public static <T> Iterable<T> concat(Iterable<? extends T>... iterables) {
    List<T> list = Lists.newArrayList();
    for(Iterable<? extends T> subIterable : iterables) {
      for(T obj : subIterable) {
        list.add(obj);
      }
    }
    return list;
  }

  /**
   * @return an Iterable that allows you to iterate over the contents of
   * all the iterables passed in.
   */
  public static <T> Iterable<T> concat(Iterable<? extends Iterable<? extends T>> iterable) {
    List<T> list = Lists.newArrayList();
    for(Iterable<? extends T> subIterable : iterable) {
      for(T obj : subIterable) {
        list.add(obj);
      }
    }
    return list;
  }

  /**
   * Provides a rotated view of a list.  Differs from {@link Collections#rotate}
   * only in that it leaves the underlying list unchanged.  Note that this is a
   * "live" view of the list that will change as the list changes.  However, the
   * behavior of an {@link Iterator} constructed from a rotated view of the list
   * is undefined if the list is changed after the Iterator is constructed.
   *
   * @param list the list to return a rotated view of.
   * @param distance the distance to rotate the list.  There are no constraints
   * on this value; it may be zero, negative, or greater than
   * {@code list.size()}.
   * @return a rotated view of the given list
   */
  public static <T> Iterable<T> rotate(final List<T> list, final int distance) {
    Preconditions.checkNotNull(list);

    /*
     * If no rotation is requested or there is nothing to rotate (list of size
     * 0 or 1), just return the original list
     */
    if (distance == 0 || list.size() <= 1) {
      return list;
    }

    return new Iterable<T>() {
      /**
       * Determines the actual distance we need to rotate (distance provided
       * might be larger than the size of the list or negative).
       */
      private int calcActualDistance(int size) {
        // we already know distance and size are non-zero
        int actualDistance = distance % size;
        if (actualDistance < 0) {
          // distance must have been negative
          actualDistance += size;
        }
        return actualDistance;
      }

      public Iterator<T> iterator() {
        int size = list.size();
        int actualDistance = calcActualDistance(size);
        // optimization:
        // lists of a size that go into the distance evenly don't need rotation
        if (actualDistance == 0) {
          return list.iterator();
        }

        @SuppressWarnings("unchecked")
        Iterable<T> rotated = concat(
            list.subList(actualDistance, size), list.subList(0, actualDistance));
        return rotated.iterator();
      }
    };
  }

}
