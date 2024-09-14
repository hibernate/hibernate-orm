/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id.usertype.inet;

import org.hibernate.dialect.PostgreSQLInetJdbcType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Vlad Mihalcea
 */
public class InetJdbcType extends PostgreSQLInetJdbcType {

	public static final InetJdbcType INSTANCE = new InetJdbcType();

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().getDescriptor( Inet.class );
	}

	@Override
	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) {
		return javaType.wrap( string, options );
	}
}
