/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.List;

import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ListJavaDescriptor extends AbstractBasicJavaDescriptor<List> {
	/**
	 * Singleton access
	 */
	public static final ListJavaDescriptor INSTANCE = new ListJavaDescriptor();

	private ListJavaDescriptor() {
		super( List.class );
	}

	@Override
	public Class<List> getJavaType() {
		return List.class;
	}

	@Override
	public String getTypeName() {
		return getJavaType().getName();
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		// none
		return null;
	}

	@Override
	public String extractLoggableRepresentation(List value) {
		return "{list}";
	}

	@Override
	public String toString(List value) {
		return "{list}";
	}

	@Override
	public List fromString(String string) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> X unwrap(List value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> List wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}
}
