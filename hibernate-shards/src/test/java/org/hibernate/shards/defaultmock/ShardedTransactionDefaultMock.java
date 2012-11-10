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

package org.hibernate.shards.defaultmock;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.LocalStatus;
import org.hibernate.shards.ShardedTransaction;

import javax.transaction.Synchronization;

/**
 * @author Tomislav Nad
 */
public class ShardedTransactionDefaultMock implements ShardedTransaction {

    @Override
    public void setupTransaction(Session session) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IsolationDelegate createIsolationDelegate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JoinStatus getJoinStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void markForJoin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void join() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resetJoinStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void markRollbackOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invalidate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInitiator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void begin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void commit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rollback() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocalStatus getLocalStatus() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isActive() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isParticipating() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean wasCommitted() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean wasRolledBack() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerSynchronization(Synchronization synchronization) throws HibernateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTimeout(int seconds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTimeout() {
        throw new UnsupportedOperationException();
    }
}
