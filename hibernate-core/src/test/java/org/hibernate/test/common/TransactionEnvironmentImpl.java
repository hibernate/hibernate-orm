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
package org.hibernate.test.common;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.transaction.spi.TransactionEnvironment;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.stat.internal.ConcurrentStatisticsImpl;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * @author Steve Ebersole
 */
public class TransactionEnvironmentImpl implements TransactionEnvironment {
	private final ServiceRegistry serviceRegistry;
	private final ConcurrentStatisticsImpl statistics = new ConcurrentStatisticsImpl();

	public TransactionEnvironmentImpl(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		throw new NotYetImplementedException( "Not available in this context" );
	}

	@Override
	public JdbcServices getJdbcServices() {
		return serviceRegistry.getService( JdbcServices.class );
	}

	@Override
	public JtaPlatform getJtaPlatform() {
		return serviceRegistry.getService( JtaPlatform.class );
	}

	@Override
	public TransactionFactory getTransactionFactory() {
		return serviceRegistry.getService( TransactionFactory.class );
	}

	@Override
	public StatisticsImplementor getStatisticsImplementor() {
		return statistics;
	}
}
