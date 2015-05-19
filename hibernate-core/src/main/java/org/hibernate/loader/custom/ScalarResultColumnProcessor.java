/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
