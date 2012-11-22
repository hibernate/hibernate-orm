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

package org.hibernate.shards;

import org.hibernate.HibernateException;

/**
 * Exception thrown when someone attempts to create a cross-shard association.
 * Here's an example of a cross-shard association.  Let's say we have
 * AccountManager and Account.  There is an owned, one-to-many relationship
 * between AccountManagers and Accounts, and we are sharding by AccountManager.
 * That means an AccountManager and all her Accounts live on the same shard.
 * Now suppose you did the following:
 *
 * {@code
public void reassignLeastProfitableAccount(AccountManager mgr1, AccountManager mgr2) {
  Account acct = mgr1.removeLeastProfitableAccount();
  acct.setAccountManager(mgr2);
  mgr2.addAccount(acct);
}}
 * If the 2 managers happen to live on different shards and you were to then
 * attempt to save the second manager you would receive a
 * CrossShardAssociationException because the account lives on a different shard
 * than the manager with which you're attempting to associate it.
 *
 * Now you'll notice a few things about this example.  First, it doesn't really
 * respect the constraints of an owned one-to-many relationship.  If AccountManagers
 * truly own Accounts (as opposed to just being associated with them), it doesn't
 * makes sense to reassign an account because the AccountManager is part of that
 * object's identity.  And If the relationship is an association then you
 * probably shouldn't be using Hibernate Sharding to manage this relationship
 * because Accounts are going to pass between AccountManagers, which means
 * Accounts are going to need to pass between shards, which means you're better
 * off just letting Hibernate manage the relationship between AccountManagers
 * and account ids and loading the objects uniquely identified by those ids
 * on your own.
 *
 * The other thing you'll notice is that if the two managers happen to live on
 * the same shard this will work just fine.  Yup, it will.  We can detect
 * cross-shard relationships.  We can't detect risky code.  You just need to
 * be careful here.
 *
 * @author maxr@google.com (Max Ross)
 */
public class CrossShardAssociationException extends HibernateException {

  public CrossShardAssociationException(Throwable root) {
    super(root);
  }

  public CrossShardAssociationException(String string, Throwable root) {
    super(string, root);
  }

  public CrossShardAssociationException(String s) {
    super(s);
  }
}
