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

package org.hibernate.shards.query;

import org.hibernate.LockMode;
import org.hibernate.Query;

/**
 * @author Maulik Shah
 */
public class SetLockModeEvent implements QueryEvent {

  private final String alias;
  private final LockMode lockMode;

  public SetLockModeEvent(String alias, LockMode lockMode) {
    this.alias = alias;
    this.lockMode = lockMode;
  }

  public void onEvent(Query query) {
    query.setLockMode(alias, lockMode);
  }

}
