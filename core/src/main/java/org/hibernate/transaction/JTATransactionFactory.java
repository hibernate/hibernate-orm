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

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.cfg.Environment;
import org.hibernate.util.NamingHelper;
import org.hibernate.util.JTAHelper;

/**
 * Factory for {@link JTATransaction} instances.
 * <p/>
 * To be completely accurate to the JTA spec, JTA implementations should
 * publish their contextual {@link UserTransaction} reference into JNDI.
 * However, in practice there are quite a few <tt>stand-alone</tt>
 * implementations intended for use outside of J2EE/JEE containers and
 * which therefore do not publish their {@link UserTransaction} references
 * into JNDI but which otherwise follow the aspects of the JTA specification.
 * This {@link TransactionFactory} implementation can support both models.
 * <p/>
 * For complete JTA implementations (including dependence on JNDI), the
 * {@link UserTransaction} reference is obtained by a call to
 * {@link #resolveInitialContext}.  Hibernate will then attempt to locate the
 * {@link UserTransaction} within this resolved
 * {@link InitialContext} based on the namespace returned by
 * {@link #resolveUserTransactionName}.
 * <p/>
 * For the so-called <tt>stand-alone</tt> implementations, we do not care at
 * all about the JNDI aspects just described.  Here, the implementation would
 * have a specific manner to obtain a reference to its contextual
 * {@link UserTransaction}; usually this would be a static code reference, but
 * again it varies.  Anyway, for each implementation the integration would need
 * to override the {@link #getUserTransaction} method and return the appropriate
 * thing.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Les Hazlewood
 */
public class JTATransactionFactory implements TransactionFactory {
	public static final String DEFAULT_USER_TRANSACTION_NAME = "java:comp/UserTransaction";
	private static final Logger log = LoggerFactory.getLogger( JTATransactionFactory.class );

	protected InitialContext initialContext;
	protected String userTransactionName;

	/**
	 * Configure this transaction factory.  Specifically here we are attempting to
	 * resolve both an {@link #getInitialContext InitialContext} as well as the
	 * {@link #getUserTransactionName() JNDI namespace} for the {@link UserTransaction}.
	 *
	 * @param props The configuration properties
	 *
	 * @exception HibernateException
	 */
	public void configure(Properties props) throws HibernateException {
		this.initialContext = resolveInitialContext( props );
		this.userTransactionName = resolveUserTransactionName( props );
		log.trace( "Configured JTATransactionFactory to use [{}] for UserTransaction JDNI namespace", userTransactionName );
	}

	/**
	 * Given the lot of Hibernate configuration properties, resolve appropriate
	 * reference to JNDI {@link InitialContext}.
	 * <p/>
	 * In general, the properties in which we are interested here all begin with
	 * <tt>hibernate.jndi</tt>.  Especially important depending on your
	 * environment are {@link Environment#JNDI_URL hibernate.jndi.url} and
	 *  {@link Environment#JNDI_CLASS hibernate.jndi.class}
	 *
	 * @param properties The Hibernate config properties.
	 * @return The resolved InitialContext.
	 */
	protected final InitialContext resolveInitialContext(Properties properties) {
		try {
			return NamingHelper.getInitialContext( properties );
		}
		catch ( NamingException ne ) {
			throw new HibernateException( "Could not obtain initial context", ne );
		}
	}

	/**
	 * Given the lot of Hibernate configuration properties, resolve appropriate
	 * JNDI namespace to use for {@link UserTransaction} resolution.
	 * <p/>
	 * We determine the namespace to use by<ol>
	 * <li>Any specified {@link Environment#USER_TRANSACTION jta.UserTransaction} config property</li>
	 * <li>If a {@link TransactionManagerLookup} was indicated, use its
	 * {@link TransactionManagerLookup#getUserTransactionName}</li>
	 * <li>finally, as a last resort, we use {@link #DEFAULT_USER_TRANSACTION_NAME}</li>
	 * </ol>
	 *
	 * @param properties The Hibernate config properties.
	 * @return The resolved {@link UserTransaction} namespace
	 */
	protected final String resolveUserTransactionName(Properties properties) {
		String utName = properties.getProperty( Environment.USER_TRANSACTION );
		if ( utName == null ) {
			TransactionManagerLookup lookup = TransactionManagerLookupFactory.getTransactionManagerLookup( properties );
			if ( lookup != null ) {
				utName = lookup.getUserTransactionName();
			}
		}
		return utName == null ? DEFAULT_USER_TRANSACTION_NAME : utName;
	}

	/**
	 * {@inheritDoc}
	 */
	public Transaction createTransaction(JDBCContext jdbcContext, Context transactionContext)
			throws HibernateException {
		UserTransaction ut = getUserTransaction();
		return new JTATransaction( ut, jdbcContext, transactionContext );
	}

	/**
	 * Get the {@link UserTransaction} reference.
	 *
	 * @return The appropriate {@link UserTransaction} reference.
	 */
	protected UserTransaction getUserTransaction() {
		final String utName = getUserTransactionName();
		log.trace( "Attempting to locate UserTransaction via JNDI [{}]", utName );

		try {
			UserTransaction ut = ( UserTransaction ) getInitialContext().lookup( utName );
			if ( ut == null ) {
				throw new TransactionException( "Naming service lookup for UserTransaction returned null [" + utName +"]" );
			}

			log.trace( "Obtained UserTransaction" );

			return ut;
		}
		catch ( NamingException ne ) {
			throw new TransactionException( "Could not find UserTransaction in JNDI [" + utName + "]", ne );
		}
	}

	/**
	 * Getter for property 'initialContext'.
	 *
	 * @return Value for property 'initialContext'.
	 */
	protected InitialContext getInitialContext() {
		return initialContext;
	}

	/**
	 * Getter for property 'userTransactionName'.
	 * The algorithm here is
	 *
	 * @return Value for property 'userTransactionName'.
	 */
	protected String getUserTransactionName() {
		return userTransactionName;
	}

	/**
	 * {@inheritDoc}
	 */
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.AFTER_STATEMENT;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTransactionManagerRequired() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean areCallbacksLocalToHibernateTransactions() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTransactionInProgress(
			JDBCContext jdbcContext,
			Context transactionContext,
			Transaction transaction) {
		try {
			// Essentially:
			// 1) If we have a local (Hibernate) transaction in progress
			//      and it already has the UserTransaction cached, use that
			//      UserTransaction to determine the status.
			// 2) If a transaction manager has been located, use
			//      that transaction manager to determine the status.
			// 3) Finally, as the last resort, try to lookup the
			//      UserTransaction via JNDI and use that to determine the
			//      status.
			if ( transaction != null ) {
				UserTransaction ut = ( ( JTATransaction ) transaction ).getUserTransaction();
				if ( ut != null ) {
					return JTAHelper.isInProgress( ut.getStatus() );
				}
			}

			if ( jdbcContext.getFactory().getTransactionManager() != null ) {
				return JTAHelper.isInProgress( jdbcContext.getFactory().getTransactionManager().getStatus() );
			}
			else {
				UserTransaction ut = getUserTransaction();
				return ut != null && JTAHelper.isInProgress( ut.getStatus() );
			}
		}
		catch ( SystemException se ) {
			throw new TransactionException( "Unable to check transaction status", se );
		}
	}

}
