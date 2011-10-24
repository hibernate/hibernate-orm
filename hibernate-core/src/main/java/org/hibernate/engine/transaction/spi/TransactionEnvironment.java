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
package org.hibernate.engine.transaction.spi;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * Provides access to transactional services.
 *
 * @author Steve Ebersole
 */
public interface TransactionEnvironment {
	/**
	 * Retrieve the session factory for this environment.
	 *
	 * @return The session factory
	 */
	public SessionFactoryImplementor getSessionFactory();

	/**
	 * Retrieve the JDBC services for this environment.
	 *
	 * @return The JDBC services
	 */
	public JdbcServices getJdbcServices();

	/**
	 * Retrieve the JTA platform for this environment.
	 *
	 * @return The JTA platform
	 */
	public JtaPlatform getJtaPlatform();

	/**
	 * Retrieve the transaction factory for this environment.
	 *
	 * @return The transaction factory
	 */
	public TransactionFactory getTransactionFactory();

	/**
	 * Get access to the statistics collector
	 *
	 * @return The statistics collector
	 */
	public StatisticsImplementor getStatisticsImplementor();
}
