/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.jbc2.functional.classloader;

import java.io.Serializable;

/**
 * Comment
 * 
 * @author Brian Stansberry
 * @version $Revision: 60233 $
 */
//   @NamedQuery(name="account.totalbalance.default",query="select account.balance from Account as account where account.accountHolder = ?1",
//               hints={@QueryHint(name="org.hibernate.cacheable",value="true")}),
//   @NamedQuery(name="account.totalbalance.namedregion",query="select account.balance from Account as account where account.accountHolder = ?1",
//               hints={@QueryHint(name="org.hibernate.cacheRegion",value="AccountRegion"),
//                      @QueryHint(name="org.hibernate.cacheable",value="true")
//                     }),
//   @NamedQuery(name="account.branch.default",query="select account.branch from Account as account where account.accountHolder = ?1",
//               hints={@QueryHint(name="org.hibernate.cacheable",value="true")}),
//   @NamedQuery(name="account.branch.namedregion",query="select account.branch from Account as account where account.accountHolder = ?1",
//               hints={@QueryHint(name="org.hibernate.cacheRegion",value="AccountRegion"),
//                      @QueryHint(name="org.hibernate.cacheable",value="true")
//                     }),
//   @NamedQuery(name="account.bybranch.default",query="select account from Account as account where account.branch = ?1",
//               hints={@QueryHint(name="org.hibernate.cacheable",value="true")}),
//   @NamedQuery(name="account.bybranch.namedregion",query="select account from Account as account where account.branch = ?1",
//               hints={@QueryHint(name="org.hibernate.cacheRegion",value="AccountRegion"),
//                      @QueryHint(name="org.hibernate.cacheable",value="true")
//                     })
public class Account implements Serializable
{
   
   private static final long serialVersionUID = 1L;
   
   private Integer id;
   private AccountHolder accountHolder;
   private Integer balance;
   private String branch;
   
   public Integer getId()
   {
      return id;
   }
   public void setId(Integer id)
   {
      this.id = id;
   }
   
   public AccountHolder getAccountHolder()
   {
      return accountHolder;
   }
   public void setAccountHolder(AccountHolder accountHolder)
   {
      this.accountHolder = accountHolder;
   }
   
   public Integer getBalance()
   {
      return balance;
   }
   public void setBalance(Integer balance)
   {
      this.balance = balance;
   }
   public String getBranch()
   {
      return branch;
   }
   public void setBranch(String branch)
   {
      this.branch = branch;
   }
   
   public boolean equals(Object obj)
   {
      if (obj == this) return true;
      if (!(obj instanceof Account)) return false;
      Account acct = (Account)obj;
      if (!safeEquals(id, acct.id)) return false;
      if (!safeEquals(branch, acct.branch)) return false;
      if (!safeEquals(balance, acct.balance)) return false;
      if (!safeEquals(accountHolder, acct.accountHolder)) return false;
      return true;
   }
   
   public int hashCode( )
   {
      int result = 17;
      result = result * 31 + safeHashCode(id);
      result = result * 31 + safeHashCode(branch);
      result = result * 31 + safeHashCode(balance);
      result = result * 31 + safeHashCode(accountHolder);
      return result;
   }
   
   public String toString()
   {
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
