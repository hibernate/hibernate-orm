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

/**
 * A simple class to represent a pair.
 *
 * @author maxr@google.com (Max Ross)
 */
public class Pair<A,B> {
  public final A first;

  /** The second element of the pair. */
  public final B second;

  private Pair(/*@Nullable*/ A first, /*@Nullable*/ B second) {
    this.first = first;
    this.second = second;
  }

  public A getFirst() {
    return first;
  }

  public B getSecond() {
    return second;
  }

  public static <A,B> Pair<A,B> of(/*@Nullable*/ A first, /*@Nullable*/ B second) {
    return new Pair<A,B>(first, second);
  }

  private static boolean eq(/*@Nullable*/ Object a, /*@Nullable*/ Object b) {
    return a == b || (a != null && a.equals(b));
  }

  @Override
  public boolean equals(/*@Nullable*/ Object object) {
    if (object instanceof Pair<?,?>) {
      Pair<?,?> other = (Pair<?,?>) object;
      return eq(first, other.first) && eq(second, other.second);
    }
    return false;
  }
}
