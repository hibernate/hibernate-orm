/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
public class PostgreSQLJsonbJdbcType extends PostgreSQLPGObjectJdbcType {

	public static final PostgreSQLJsonbJdbcType INSTANCE = new PostgreSQLJsonbJdbcType();

	public PostgreSQLJsonbJdbcType() {
		super( "jsonb", SqlTypes.JSON );
	}

	@Override
	protected <X> X fromString(String string, JavaType<X> javaTypeDescriptor, WrapperOptions options) {
		return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().fromString(
				string,
				javaTypeDescriptor,
				options
		);
	}

	@Override
	protected <X> String toString(X value, JavaType<X> javaTypeDescriptor, WrapperOptions options) {
		return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().toString(
				value,
				javaTypeDescriptor,
				options
		);
	}
}
