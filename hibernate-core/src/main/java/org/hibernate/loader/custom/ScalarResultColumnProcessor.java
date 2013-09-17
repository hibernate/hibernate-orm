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
 * Represents a scalar result within the custom query
 *
 * @author Steve Ebersole
 */
public class ScalarResultColumnProcessor implements ResultColumnProcessor {
	private int position = -1;
	private String alias;
	private Type type;

	public ScalarResultColumnProcessor(int position) {
		this.position = position;
	}

	public ScalarResultColumnProcessor(String alias, Type type) {
		this.alias = alias;
		this.type = type;
	}

	@Override
	public void performDiscovery(JdbcResultMetadata metadata, List<Type> types, List<String> aliases) throws SQLException {
		if ( alias == null ) {
			alias = metadata.getColumnName( position );
		}
		else if ( position < 0 ) {
			position = metadata.resolveColumnPosition( alias );
		}
		if ( type == null ) {
			type = metadata.getHibernateType( position );
		}
		types.add( type );
		aliases.add( alias );
	}

	@Override
	public Object extract(Object[] data, ResultSet resultSet, SessionImplementor session)
			throws SQLException, HibernateException {
		return type.nullSafeGet( resultSet, alias, session, null );
	}
}
