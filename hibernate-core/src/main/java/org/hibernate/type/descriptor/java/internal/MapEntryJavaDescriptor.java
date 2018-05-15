/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Map;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class MapEntryJavaDescriptor extends AbstractBasicJavaDescriptor<Map.Entry> {
	/**
	 * Singleton access
	 */
	public static final MapEntryJavaDescriptor INSTANCE = new MapEntryJavaDescriptor();

	public MapEntryJavaDescriptor() {
		super( Map.Entry.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		throw new UnsupportedOperationException( "Unsupported attempt to resolve JDBC type for Map.Entry" );
	}

	@Override
	public <X> X unwrap(Map.Entry value, Class<X> type, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Unsupported attempt to unwrap Map.Entry value" );
	}

	@Override
	public <X> Map.Entry wrap(X value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Unsupported attempt to wrap Map.Entry value" );
	}
}
