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
 * Helper methods for checking preconditions.
 *
 * @author maxr@google.com (Max Ross)
 */
public class Preconditions {

  private Preconditions() { }

  /**
   * @param expression the boolean to evaluate
   * @throws IllegalArgumentException thrown if boolean is false
   */
  public static void checkArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * @param expression the boolean to evaluate
   * @throws IllegalStateException thrown if boolean is false
   */
  public static void checkState(boolean expression) {
    if (!expression) {
      throw new IllegalStateException();
    }
  }

  /**
   * @param reference the object to compare against null
   * @throws NullPointerException thrown if object is null
   */
  public static void checkNotNull(Object reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
  }
}
