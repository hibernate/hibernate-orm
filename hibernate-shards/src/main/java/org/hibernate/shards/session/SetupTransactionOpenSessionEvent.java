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

package org.hibernate.shards.session;

import org.hibernate.Session;
import org.hibernate.shards.ShardedTransaction;

/**
 * OpenSessionEvent which adds newly opened session to the specified
 * ShardedTransaction.
 *
 * @author Tomislav Nad
 */
public class SetupTransactionOpenSessionEvent implements OpenSessionEvent {

  private final ShardedTransaction shardedTransaction;

  public SetupTransactionOpenSessionEvent(ShardedTransaction shardedTtransaction) {
    this.shardedTransaction = shardedTtransaction;
  }

  public void onOpenSession(Session session) {
    shardedTransaction.setupTransaction(session);
  }
}
