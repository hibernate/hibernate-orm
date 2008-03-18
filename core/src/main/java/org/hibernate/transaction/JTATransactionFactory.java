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
 *
 * @author Gavin King
 */
public class JTATransactionFactory implements TransactionFactory {

	private static final Logger log = LoggerFactory.getLogger( JTATransactionFactory.class );
	private static final String DEFAULT_USER_TRANSACTION_NAME = "java:comp/UserTransaction";

	protected InitialContext context;
	protected String utName;

	public void configure(Properties props) throws HibernateException {
		try {
			context = NamingHelper.getInitialContext( props );
		}
		catch ( NamingException ne ) {
			log.error( "Could not obtain initial context", ne );
			throw new HibernateException( "Could not obtain initial context", ne );
		}

		utName = props.getProperty( Environment.USER_TRANSACTION );

		if ( utName == null ) {
			TransactionManagerLookup lookup = TransactionManagerLookupFactory.getTransactionManagerLookup( props );
			if ( lookup != null ) {
				utName = lookup.getUserTransactionName();
			}
		}

		if ( utName == null ) {
			utName = DEFAULT_USER_TRANSACTION_NAME;
		}
	}

	public Transaction createTransaction(JDBCContext jdbcContext, Context transactionContext)
			throws HibernateException {
		return new JTATransaction( context, utName, jdbcContext, transactionContext );
	}

	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.AFTER_STATEMENT;
	}

	public boolean isTransactionManagerRequired() {
		return false;
	}

	public boolean areCallbacksLocalToHibernateTransactions() {
		return false;
	}

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
				try {
					UserTransaction ut = ( UserTransaction ) context.lookup( utName );
					return ut != null && JTAHelper.isInProgress( ut.getStatus() );
				}
				catch ( NamingException ne ) {
					throw new TransactionException( "Unable to locate UserTransaction to check status", ne );
				}
			}
		}
		catch ( SystemException se ) {
			throw new TransactionException( "Unable to check transaction status", se );
		}
	}

}
