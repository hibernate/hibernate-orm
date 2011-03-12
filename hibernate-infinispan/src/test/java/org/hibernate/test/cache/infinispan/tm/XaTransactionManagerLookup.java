/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or it's affiliates, and individual contributors
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
package org.hibernate.test.cache.infinispan.tm;

import java.util.Properties;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.transaction.TransactionManagerLookup;

/**
 * XaResourceCapableTransactionManagerLookup.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class XaTransactionManagerLookup implements TransactionManagerLookup {

   public Object getTransactionIdentifier(Transaction transaction) {
      return transaction;
   }

   public TransactionManager getTransactionManager(Properties props) throws HibernateException {
      return XaTransactionManagerImpl.getInstance();
   }

   public String getUserTransactionName() {
      throw new UnsupportedOperationException( "jndi currently not implemented for these tests" );
   }

}
