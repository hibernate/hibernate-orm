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
 * String utilities.
 *
 * @author maxr@google.com (Max Ross)
 */
public class StringUtil {

  /**
   * Helper function for null, empty, and whitespace string testing.
   *
   * @return true if s == null or s.equals("") or s contains only whitespace
   *         characters.
   */
  public static boolean isEmptyOrWhitespace(String s) {
    s = makeSafe(s);
    for (int i = 0, n = s.length(); i < n; i++) {
      if (!Character.isWhitespace(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return true if s == null or s.equals("")
   */
  public static boolean isEmpty(String s) {
    return makeSafe(s).length() == 0;
  }

  /**
   * Helper function for making null strings safe for comparisons, etc.
   *
   * @return (s == null) ? "" : s;
   */
  public static String makeSafe(String s) {
    return (s == null) ? "" : s;
  }

  /**
   * @return the string provided with its first character capitalized
   */
  public static String capitalize(String s) {
    if (s.length() == 0) {
      return s;
    }
    char first = s.charAt(0);
    char capitalized = Character.toUpperCase(first);
    return (first == capitalized)
        ? s
        : capitalized + s.substring(1);
  }
}
