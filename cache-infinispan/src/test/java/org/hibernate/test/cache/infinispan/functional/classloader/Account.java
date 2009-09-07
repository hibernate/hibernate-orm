/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cache.infinispan.functional.classloader;

import java.io.Serializable;

/**
 * Comment
 * 
 * @author Brian Stansberry
 */
public class Account implements Serializable {

   private static final long serialVersionUID = 1L;

   private Integer id;
   private AccountHolder accountHolder;
   private Integer balance;
   private String branch;

   public Integer getId() {
      return id;
   }

   public void setId(Integer id) {
      this.id = id;
   }

   public AccountHolder getAccountHolder() {
      return accountHolder;
   }

   public void setAccountHolder(AccountHolder accountHolder) {
      this.accountHolder = accountHolder;
   }

   public Integer getBalance() {
      return balance;
   }

   public void setBalance(Integer balance) {
      this.balance = balance;
   }

   public String getBranch() {
      return branch;
   }

   public void setBranch(String branch) {
      this.branch = branch;
   }

   public boolean equals(Object obj) {
      if (obj == this)
         return true;
      if (!(obj instanceof Account))
         return false;
      Account acct = (Account) obj;
      if (!safeEquals(id, acct.id))
         return false;
      if (!safeEquals(branch, acct.branch))
         return false;
      if (!safeEquals(balance, acct.balance))
         return false;
      if (!safeEquals(accountHolder, acct.accountHolder))
         return false;
      return true;
   }

   @Override
   public int hashCode() {
      int result = 17;
      result = result * 31 + safeHashCode(id);
      result = result * 31 + safeHashCode(branch);
      result = result * 31 + safeHashCode(balance);
      result = result * 31 + safeHashCode(accountHolder);
      return result;
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer(getClass().getName());
      sb.append("[id=");
      sb.append(id);
      sb.append(",branch=");
      sb.append(branch);
      sb.append(",balance=");
      sb.append(balance);
      sb.append(",accountHolder=");
      sb.append(accountHolder);
      sb.append("]");
      return sb.toString();
   }

   private static int safeHashCode(Object obj) {
      return obj == null ? 0 : obj.hashCode();
   }

   private static boolean safeEquals(Object a, Object b) {
      return (a == b || (a != null && a.equals(b)));
   }

}
