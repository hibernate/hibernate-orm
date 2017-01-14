/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Collection;

import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionJavaDescriptor extends AbstractBasicJavaDescriptor<Collection> {
	/**
	 * Singleton access
	 */
	public static final CollectionJavaDescriptor INSTANCE = new CollectionJavaDescriptor();

	private CollectionJavaDescriptor() {
		super( Collection.class );
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
	public String toString(Collection value) {
		return "{java.util.Collection}";
	}

	@Override
	public Collection fromString(String string) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> X unwrap(Collection value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> Collection wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(  );
	}
}
