/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import java.util.Map;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * JavaType for dynamic models
 *
 * @author Steve Ebersole
 */
public class DynamicModelJavaType implements JavaType<Map<?,?>> {

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<?,?> fromString(CharSequence string) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> X unwrap(Map<?,?> value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> Map<?,?> wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<Map<?,?>> getJavaTypeClass() {
		//noinspection unchecked,rawtypes
		return (Class) Map.class;
	}
}
