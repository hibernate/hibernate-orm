/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 *
 */
package org.hibernate.transaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.hibernate.HibernateException;
import org.hibernate.util.NamingHelper;

/**
 * TransactionManagerLookup implementation intended for use with WebSphere
 * Application Server (WAS).
 * <p/>
 * WAS, unlike every other app server on the planet, does not allow direct
 * access to the JTS TransactionManager.  Instead, for common transaction-
 * related tasks users must utilize a proprietary API known as
 * ExtendedJTATransaction.
 * <p/>
 * Even more unfortunate, the exact TransactionManagerLookup to use inside of
 * WAS is highly dependent upon (1) WAS version as well as (2) the WAS
 * container in which Hibernate will be utilized.
 * <p/>
 * WebSphereExtendedJTATransactionLookup is reported to work on WAS version 6
 * in any of the standard J2EE/JEE component containers.
 *
 * @author Gavin King
 * @author <a href="mailto:jesper@udby.com>Jesper Udby</a>
 */
public class WebSphereExtendedJTATransactionLookup implements TransactionManagerLookup {

	/**
	 * {@inheritDoc}
	 */
	public TransactionManager getTransactionManager(Properties props) {
		return new TransactionManagerAdapter( props );
	}

	/**
	 * {@inheritDoc}
	 */
	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getTransactionIdentifier(Transaction transaction) {
		// WebSphere, however, is not a sane JEE/JTA container...
		return new Integer( transaction.hashCode() );
	}

	public static class TransactionManagerAdapter implements TransactionManager {
		private final Properties properties;
		private final Class synchronizationCallbackClass;
		private final Method registerSynchronizationMethod;
		private final Method getLocalIdMethod;
		private Object extendedJTATransaction;

		private TransactionManagerAdapter(Properties props) throws HibernateException {
			this.properties = props;
			try {
				synchronizationCallbackClass = Class.forName( "com.ibm.websphere.jtaextensions.SynchronizationCallback" );
				Class extendedJTATransactionClass = Class.forName( "com.ibm.websphere.jtaextensions.ExtendedJTATransaction" );
				registerSynchronizationMethod = extendedJTATransactionClass.getMethod(
						"registerSynchronizationCallbackForCurrentTran",
						new Class[] { synchronizationCallbackClass }
				);
				getLocalIdMethod = extendedJTATransactionClass.getMethod( "getLocalId", null );

			}
			catch ( ClassNotFoundException cnfe ) {
				throw new HibernateException( cnfe );
			}
			catch ( NoSuchMethodException nsme ) {
				throw new HibernateException( nsme );
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public void begin() throws NotSupportedException, SystemException {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void commit() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public int getStatus() throws SystemException {
			return getTransaction() == null ? Status.STATUS_NO_TRANSACTION : getTransaction().getStatus();
		}

		/**
		 * {@inheritDoc}
		 */
		public Transaction getTransaction() throws SystemException {
			return new TransactionAdapter( properties );
		}

		/**
		 * {@inheritDoc}
		 */
		public void resume(Transaction txn) throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void rollback() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setRollbackOnly() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public void setTransactionTimeout(int i) throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		/**
		 * {@inheritDoc}
		 */
		public Transaction suspend() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		public class TransactionAdapter implements Transaction {

			private TransactionAdapter(Properties props) {
				try {
					if ( extendedJTATransaction == null ) {
						extendedJTATransaction = NamingHelper.getInitialContext( props )
								.lookup( "java:comp/websphere/ExtendedJTATransaction" );
					}
				}
				catch (NamingException ne) {
					throw new HibernateException(ne);
				}
			}

			/**
			 * {@inheritDoc}
			 */
			public void registerSynchronization(final Synchronization synchronization)
					throws RollbackException, IllegalStateException,
					SystemException {

				final InvocationHandler ih = new InvocationHandler() {

					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ( "afterCompletion".equals( method.getName() ) ) {
							int status = args[2].equals(Boolean.TRUE) ?
									Status.STATUS_COMMITTED :
									Status.STATUS_UNKNOWN;
							synchronization.afterCompletion(status);
						}
						else if ( "beforeCompletion".equals( method.getName() ) ) {
							synchronization.beforeCompletion();
						}
						else if ( "toString".equals( method.getName() ) ) {
							return synchronization.toString();
						}
						return null;
					}

				};

				final Object synchronizationCallback = Proxy.newProxyInstance(
						getClass().getClassLoader(),
						new Class[] { synchronizationCallbackClass },
						ih
					);

				try {
					registerSynchronizationMethod.invoke(
							extendedJTATransaction,
							new Object[] { synchronizationCallback }
						);
				}
				catch (Exception e) {
					throw new HibernateException(e);
				}

			}

			/**
			 * {@inheritDoc}
			 */
			public int hashCode() {
				return getLocalId().hashCode();
			}

			/**
			 * {@inheritDoc}
			 */
			public boolean equals(Object other) {
				if ( !(other instanceof TransactionAdapter) ) return false;
				TransactionAdapter that = (TransactionAdapter) other;
				return getLocalId().equals( that.getLocalId() );
			}

			/**
			 * Getter for property 'localId'.
			 *
			 * @return Value for property 'localId'.
			 */
			private Object getLocalId() throws HibernateException {
				try {
					return getLocalIdMethod.invoke( extendedJTATransaction, null );
				}
				catch ( Exception e ) {
					throw new HibernateException( e );
				}
			}

			/**
			 * {@inheritDoc}
			 */
			public void commit() throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			/**
			 * {@inheritDoc}
			 */
			public boolean delistResource(XAResource resource, int i) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			/**
			 * {@inheritDoc}
			 */
			public boolean enlistResource(XAResource resource) throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			/**
			 * {@inheritDoc}
			 */
			public int getStatus() {
				return new Integer(0).equals( getLocalId() ) ?
						Status.STATUS_NO_TRANSACTION : Status.STATUS_ACTIVE;
			}

			/**
			 * {@inheritDoc}
			 */
			public void rollback() throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}

			/**
			 * {@inheritDoc}
			 */
			public void setRollbackOnly() throws UnsupportedOperationException {
				throw new UnsupportedOperationException();
			}
		}

	}

}
