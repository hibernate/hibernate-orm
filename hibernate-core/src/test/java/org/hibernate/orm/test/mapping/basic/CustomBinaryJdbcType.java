/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.basic;

import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;

/**
 * JdbcType for documentation
 *
 * @author Steve Ebersole
 */
public class CustomBinaryJdbcType implements JdbcType {
	@Override
	public int getJdbcTypeCode() {
		return Types.VARBINARY;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return VarbinaryJdbcType.INSTANCE.getBinder( javaType );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return VarbinaryJdbcType.INSTANCE.getExtractor( javaType );
	}
}
