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

package org.hibernate.shards.strategy.exit;

import junit.framework.TestCase;

/**
 * @author Maulik Shah
 */
public class ExitOperationUtilsTest extends TestCase {

    private class MyInt {
      private final Integer i;

      private final String name;

      private MyInt innerMyInt;

      public MyInt(int i, String name) {
        this.i = i;
        this.name = name;
      }

      public MyInt getInnerMyInt() {
        return innerMyInt;
      }

      public void setInnerMyInt(MyInt innerMyInt) {
        this.innerMyInt = innerMyInt;
      }

      public Number getValue() {
        return i;
      }

      public String getName() {
        return name;
      }

    }

  public void testGetPropertyValue() throws Exception {
    MyInt myInt = new MyInt(1,"one");
    myInt.setInnerMyInt(new MyInt(5, "five"));
    assertEquals(5, ExitOperationUtils.getPropertyValue(myInt,"innerMyInt.value"));
    assertEquals("five", ExitOperationUtils.getPropertyValue(myInt,"innerMyInt.name"));
  }
}
