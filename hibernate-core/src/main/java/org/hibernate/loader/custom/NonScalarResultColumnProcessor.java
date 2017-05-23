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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
	public Object extract(Object[] data, ResultSet resultSet, SharedSessionContractImplementor session)
			throws SQLException, HibernateException {
		return data[ position ];
	}

}
