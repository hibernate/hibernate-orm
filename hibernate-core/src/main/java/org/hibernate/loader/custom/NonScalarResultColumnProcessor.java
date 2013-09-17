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
package org.hibernate.loader.custom;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.Type;

/**
 * Represents non-scalar returns within the custom query.  Most of the heavy lifting for non-scalar results
 * is done within Loader itself.
 *
 * @author Steve Ebersole
 */
public class NonScalarResultColumnProcessor implements ResultColumnProcessor {
	private final int position;

	public NonScalarResultColumnProcessor(int position) {
		this.position = position;
	}

	@Override
	public void performDiscovery(JdbcResultMetadata metadata, List<Type> types, List<String> aliases) {
		// nothing to discover for non-scalar results
	}

	@Override
	public Object extract(Object[] data, ResultSet resultSet, SessionImplementor session)
			throws SQLException, HibernateException {
		return data[ position ];
	}

}
