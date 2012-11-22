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

package org.hibernate.shards.model;

import java.io.Serializable;

/**
 * @author maxr@google.com (Max Ross)
 */
public class Window {
  private Serializable windowId;

  private boolean opens;

  public Serializable getWindowId() {
    return windowId;
  }

  public void setWindowId(Serializable windowId) {
    this.windowId = windowId;
  }

  public boolean getOpens() {
    return opens;
  }

  public void setOpens(boolean opens) {
    this.opens = opens;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final Window window = (Window) o;

    return windowId.equals(window.windowId);
  }

  @Override
  public int hashCode() {
    return windowId.hashCode();
  }
}
