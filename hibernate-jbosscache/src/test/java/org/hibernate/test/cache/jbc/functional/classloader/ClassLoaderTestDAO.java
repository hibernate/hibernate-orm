/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.test.cache.jbc.functional.classloader;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.transaction.TransactionManager;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comment
 * 
 * @author Brian Stansberry
 */
public class ClassLoaderTestDAO
{
   private static final Logger log = LoggerFactory.getLogger(ClassLoaderTestDAO.class);
   
   private SessionFactory sessionFactory;
   private TransactionManager tm;
   
   private Class acctClass;
   private Class holderClass;
   private Method setId;
   private Method setBalance;
   private Method setBranch;
   private Method setHolder;
   private Object smith;
   private Object jones;
   private Object barney;
   private Method setName;
   private Method setSsn;
   

   public ClassLoaderTestDAO(SessionFactory factory, TransactionManager tm) throws Exception
   {      
      this.sessionFactory = factory;
      this.tm = tm;
      
      acctClass = Thread.currentThread().getContextClassLoader().loadClass(getClass().getPackage().getName() + ".Account");
      holderClass = Thread.currentThread().getContextClassLoader().loadClass(getClass().getPackage().getName() + ".AccountHolder");
      setId = acctClass.getMethod("setId", Integer.class);
      setBalance = acctClass.getMethod("setBalance", Integer.class);
      setBranch = acctClass.getMethod("setBranch", String.class);
      setHolder = acctClass.getMethod("setAccountHolder", holderClass);
      
      
      setName = holderClass.getMethod("setLastName", String.class);
      setSsn = holderClass.getMethod("setSsn", String.class);
      
      smith = holderClass.newInstance();
      setName.invoke(smith, "Smith");
      setSsn.invoke(smith, "1000");
      
      jones = holderClass.newInstance();
      setName.invoke(jones, "Jones");
      setSsn.invoke(jones, "2000");
      
      barney = holderClass.newInstance();
      setName.invoke(barney, "Barney");
      setSsn.invoke(barney, "3000");
   }
   
   public Object getSmith() {
      return smith;
   }
   
   public Object getJones() {
      return jones;
   }
   
   public Object getBarney() {
      return barney;
   }
   
   public void updateAccountBranch(Integer id, String branch) throws Exception
   {
      log.debug("Updating account " + id + " to branch " + branch);
      tm.begin();
      try {
          Session session = sessionFactory.getCurrentSession();
          Object account = session.get(acctClass, id);
          setBranch.invoke(account, branch);
          session.update(account);
          tm.commit();
      }
      catch (Exception e) {
         log.error("rolling back", e);
         tm.rollback();
         throw e;
      }
      log.debug("Updated account " + id + " to branch " + branch);
   }
   
   public int getCountForBranch(String branch, boolean useRegion) throws Exception
   {      
      tm.begin();
      try {
          Query query = sessionFactory.getCurrentSession().createQuery("select account from Account as account where account.branch = :branch");
          query.setString("branch", branch);
          if (useRegion)
          {
              query.setCacheRegion("AccountRegion");
          }
          query.setCacheable(true);
          int result = query.list().size();
          tm.commit();
          return result;
      }
      catch (Exception e) {
         log.error("rolling back", e);
         tm.rollback();
         throw e;
      }
      
   }
   
   public void createAccount(Object holder, Integer id, Integer openingBalance, String branch) throws Exception
   {
       log.debug("Creating account " + id);
       tm.begin();
       try {
           Object account = acctClass.newInstance();
           setId.invoke(account, id);
           setHolder.invoke(account, holder);
           setBalance.invoke(account, openingBalance);
           setBranch.invoke(account, branch);
           sessionFactory.getCurrentSession().persist(account);
           tm.commit();
       }
       catch (Exception e) {
           log.error("rolling back", e);
           tm.rollback();
           throw e;
       }
       
       log.debug("Created account " + id);
   }
   
   public Account getAccount(Integer id) throws Exception
   {
      log.debug("Getting account " + id);
      tm.begin();
      try {
          Session session = sessionFactory.getCurrentSession();
          Account acct = (Account) session.get(acctClass, id);
          tm.commit();
          return acct;
      }
      catch (Exception e) {
          log.error("rolling back", e);
          tm.rollback();
          throw e;
      }
   }
   
   public Account getAccountWithRefresh(Integer id) throws Exception
   {
      log.debug("Getting account " + id + " with refresh");
      tm.begin();
      try {
          Session session = sessionFactory.getCurrentSession();
          Account acct  = (Account) session.get(acctClass, id);
          session.refresh(acct);
          acct = (Account) session.get(acctClass, id);
          tm.commit();
          return acct;
      }
      catch (Exception e) {
          log.error("rolling back", e);
          tm.rollback();
          throw e;
      }
   }
   
   public void updateAccountBalance(Integer id, Integer newBalance) throws Exception
   {
      log.debug("Updating account " + id + " to balance " + newBalance);
      tm.begin();
      try {
          Session session = sessionFactory.getCurrentSession();
          Object account = session.get(acctClass, id);
          setBalance.invoke(account, newBalance);
          session.update(account);
          tm.commit();
      }
      catch (Exception e) {
          log.error("rolling back", e);
          tm.rollback();
          throw e;
      }
      log.debug("Updated account " + id + " to balance " + newBalance);
   }
   
   public String getBranch(Object holder, boolean useRegion) throws Exception
   {      
      tm.begin();
      try {
          Query query = sessionFactory.getCurrentSession().createQuery("select account.branch from Account as account where account.accountHolder = ?");
          query.setParameter(0, holder);
          if (useRegion)
          {
             query.setCacheRegion("AccountRegion");
          }
          query.setCacheable(true);
          String result = (String) query.list().get(0);
          tm.commit();
          return result;
      }
      catch (Exception e) {
          log.error("rolling back", e);
          tm.rollback();
          throw e;
      }
   }
      
   public int getTotalBalance(Object holder, boolean useRegion)
      throws Exception
   {      
      List results = null;
      tm.begin();
      try {
          Query query = sessionFactory.getCurrentSession().createQuery("select account.balance from Account as account where account.accountHolder = ?");
          query.setParameter(0, holder);
          if (useRegion)
          {
             query.setCacheRegion("AccountRegion");
          }
          query.setCacheable(true);
          results = query.list();
          tm.commit();
      }
      catch (Exception e) {
          log.error("rolling back", e);
          tm.rollback();
          throw e;
      }
      
      int total = 0;
      if (results != null)
      {
         for (Iterator it = results.iterator(); it.hasNext();)
         {            
            total += ((Integer) it.next()).intValue();
            System.out.println("Total = " + total);
         }
      }
      return total;      
   }
   
   public void cleanup() throws Exception
   {
      internalCleanup();
   }
   
   private void internalCleanup() throws Exception
   {  
      if (sessionFactory != null)
      {         
         tm.begin();
         try {
            
             Session session = sessionFactory.getCurrentSession();
             Query query = session.createQuery("select account from Account as account");
             List accts = query.list();
             if (accts != null)
             {
                for (Iterator it = accts.iterator(); it.hasNext();)
                {
                   try
                   {
                      Object acct = it.next();
                      log.info("Removing " + acct);
                      session.delete(acct);
                   }
                   catch (Exception ignored) {}
                }
             }
             tm.commit();
         }
         catch (Exception e) {
             tm.rollback();
             throw e;
         }
      }      
   }
   
   public void remove()
   {
      try
      {
         internalCleanup();
      }
      catch (Exception e)
      {
         log.error("Caught exception in remove", e);
      }
   }
}
