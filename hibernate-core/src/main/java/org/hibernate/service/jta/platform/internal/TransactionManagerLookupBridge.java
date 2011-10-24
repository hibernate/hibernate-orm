/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.service.jta.platform.internal;

import java.util.Properties;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.service.jndi.spi.JndiService;
import org.hibernate.transaction.TransactionManagerLookup;

/**
 * @author Steve Ebersole
 */
public class TransactionManagerLookupBridge extends AbstractJtaPlatform {
	private final TransactionManagerLookup lookup;
	private final Properties jndiProperties;

	public TransactionManagerLookupBridge(TransactionManagerLookup lookup, Properties jndiProperties) {
		this.lookup = lookup;
		this.jndiProperties = jndiProperties;
	}

	@Override
	protected TransactionManager locateTransactionManager() {
		return lookup.getTransactionManager( jndiProperties );
	}

	@Override
	protected UserTransaction locateUserTransaction() {
		return (UserTransaction) serviceRegistry().getService( JndiService.class ).locate( lookup.getUserTransactionName() );
	}

	@Override
	public Object getTransactionIdentifier(Transaction transaction) {
		return lookup.getTransactionIdentifier( transaction );
	}
}
