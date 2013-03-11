/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.internal;

import java.sql.ResultSet;

import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.spi.OnDemandResultSetProcessor;

/**
 * @author Steve Ebersole
 */
public class OnDemandResultSetProcessorImpl implements OnDemandResultSetProcessor {
	@Override
	public Object extractSingleRow(
			ResultSet resultSet,
			SessionImplementor session,
			QueryParameters queryParameters) {
		return null;
	}

	@Override
	public Object extractSequentialRowsForward(
			ResultSet resultSet, SessionImplementor session, QueryParameters queryParameters) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Object extractSequentialRowsReverse(
			ResultSet resultSet,
			SessionImplementor session,
			QueryParameters queryParameters,
			boolean isLogicallyAfterLast) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
