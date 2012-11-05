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

package org.hibernate.shards.transaction;

import junit.framework.TestCase;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.classic.Session;
import org.hibernate.shards.Shard;
import org.hibernate.shards.ShardDefaultMock;
import org.hibernate.shards.ShardedTransactionDefaultMock;
import org.hibernate.shards.defaultmock.SessionDefaultMock;
import org.hibernate.shards.engine.ShardedSessionImplementorDefaultMock;
import org.hibernate.shards.util.Lists;

import javax.transaction.Synchronization;
import java.util.List;

/**
 * @author Tomislav Nad
 */
public class ShardedTransactionImplTest extends TestCase {

  private ShardedTransactionImpl sti;
  private TransactionStub transaction1;

  private class TransactionStub extends ShardedTransactionDefaultMock {
    public boolean fail = false;
    public boolean wasCommitted = false;

    @Override
    public void setupTransaction(org.hibernate.Session session) {}

    @Override
    public void begin() throws HibernateException {
      if (fail) {
        throw new TransactionException("failed");
      }
    }

    @Override
    public void commit() throws HibernateException {
      if (fail) {
        throw new TransactionException("failed");
      }
      wasCommitted = true;
    }

    @Override
    public void rollback() throws HibernateException {
      if (fail) {
        throw new TransactionException("failed");
      }
    }

    @Override
    public boolean wasCommitted() throws HibernateException {
      return wasCommitted;
    }

    @Override
    public boolean isActive() throws HibernateException {
      return true;
    }

    @Override
    public void registerSynchronization(Synchronization synchronization)
        throws HibernateException {}

    @Override
    public void setTimeout(int seconds) {}
  }

  private static class MockSession extends SessionDefaultMock {
    private Transaction transaction;

    MockSession(Transaction t) {
      transaction = t;
    }

    @Override
    public Transaction getTransaction() {
      return transaction;
    }
  }

  private static class MockShard extends ShardDefaultMock {
    private Session session;

    MockShard(Session s) {
      session = s;
    }

    @Override
    public Session getSession() {
      return session;
    }
  }

  private static class MockShardedSessionImplementor extends ShardedSessionImplementorDefaultMock {
    private List<Shard> shards;

    MockShardedSessionImplementor(List<Shard> shards) {
      this.shards = shards;
    }

    @Override
    public List<Shard> getShards() {
      return shards;
    }
  }

  @Override
  protected void setUp() {
    TransactionStub transaction1
        = new TransactionStub();
    this.transaction1 = new TransactionStub();
    List<Shard> shards = Lists.newArrayList();
    shards.add(new MockShard(new MockSession(transaction1)));
    shards.add(new MockShard(new MockSession(this.transaction1)));
    sti = new ShardedTransactionImpl(new MockShardedSessionImplementor(shards));
  }

  public void testBeginSimple() {
    sti.begin();
    assertTrue(sti.isActive());
    assertFalse(sti.wasCommitted());
    assertFalse(sti.wasRolledBack());

    // test double begin
    sti.begin();
    assertTrue(sti.isActive());
    assertFalse(sti.wasCommitted());
    assertFalse(sti.wasRolledBack());

    // test begin after commit failed
    transaction1.fail = true;
    try {
      sti.commit();
      fail();
    } catch (HibernateException he) {
      // good
    }
    transaction1.fail = false;
    try {
      sti.begin();
      fail();
    } catch (HibernateException he) {
      // good
    }
  }

  public void testBeginWithOneFailedTransaction() {
    transaction1.fail = true;
    try {
      sti.begin();
      fail();
    } catch (HibernateException he) {
      assertFalse(sti.isActive());
    }

    transaction1.fail = false;
    sti.begin();
    assertTrue(sti.isActive());
    assertFalse(sti.wasCommitted());
    assertFalse(sti.wasRolledBack());
  }

  public void testCommitSimple() {
    try {
      sti.commit();
      fail();
    } catch (HibernateException he) {
      // good
    }

    sti.begin();
    sti.commit();
    assertTrue(sti.wasCommitted());
    assertFalse(sti.isActive());
  }

  public void testCommitWithOneFailedTransaction() {
    sti.begin();
    transaction1.fail = true;
    try {
      sti.commit();
      fail();
    } catch (HibernateException he) {
      assertFalse(sti.wasCommitted());
      assertTrue(he.getCause() instanceof HibernateException);
    }
  }

  public void testRollbackSimple() {
    try {
      sti.rollback();
      fail();
    } catch (HibernateException he) {
      // good
      assertFalse(sti.wasRolledBack());
    }

    sti.begin();
    sti.rollback();
    assertTrue(sti.wasRolledBack());

    sti.commit();
    try {
      sti.rollback();
      fail();
    } catch (HibernateException he) {
      // good
      assertTrue(sti.wasRolledBack());
    }

    sti.begin();
    transaction1.fail = true;
    try {
      sti.commit();
    } catch (HibernateException he) {
      assertFalse(sti.wasRolledBack());
      assertTrue(he.getCause() instanceof HibernateException);
      sti.rollback();
      assertTrue(sti.wasRolledBack());
    }
  }

  public void testRollbackWithOneFailedTransaction() {
    sti.begin();
    transaction1.fail = true;
    try {
      sti.rollback();
    } catch (HibernateException he) {
      assertFalse(sti.wasRolledBack());
      assertTrue(he.getCause() instanceof HibernateException);
    }
  }

  public void testMultipleIterations() {
    sti.begin();
    sti.commit();

    sti.begin();
    assertTrue(sti.isActive());
    assertFalse(sti.wasCommitted());
    sti.commit();
    assertTrue(sti.wasCommitted());
  }
}
