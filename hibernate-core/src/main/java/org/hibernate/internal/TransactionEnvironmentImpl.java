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
package org.hibernate.internal;

import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.spi.TransactionEnvironment;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * @author Steve Ebersole
 */
public class TransactionEnvironmentImpl implements TransactionEnvironment {
    private final SessionFactoryImpl sessionFactory;
    private final transient StatisticsImplementor statisticsImplementor;
    private final transient ServiceRegistry serviceRegistry;
    private final transient JdbcServices jdbcServices;
    private final transient JtaPlatform jtaPlatform;
    private final transient TransactionFactory transactionFactory;

    public TransactionEnvironmentImpl(SessionFactoryImpl sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.statisticsImplementor = sessionFactory.getStatisticsImplementor();
        this.serviceRegistry = sessionFactory.getServiceRegistry();
        this.jdbcServices = serviceRegistry.getService( JdbcServices.class );
        this.jtaPlatform = serviceRegistry.getService( JtaPlatform.class );
        this.transactionFactory = serviceRegistry.getService( TransactionFactory.class );
    }

    @Override
    public SessionFactoryImplementor getSessionFactory() {
        return sessionFactory;
    }

    protected ServiceRegistry serviceRegistry() {
        return serviceRegistry;
    }

    @Override
    public JdbcServices getJdbcServices() {
        return jdbcServices;
    }

    @Override
    public JtaPlatform getJtaPlatform() {
        return jtaPlatform;
    }

    @Override
    public TransactionFactory getTransactionFactory() {
        return transactionFactory;
    }

    @Override
    public StatisticsImplementor getStatisticsImplementor() {
        return statisticsImplementor;
    }
}
