/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

public class PrimitiveBasicTypeImpl<J> extends BasicTypeImpl<J> {
	private final Class<J> primitiveClass;

	public PrimitiveBasicTypeImpl(JavaType<J> javaType, JdbcType jdbcType, Class<J> primitiveClass) {
		super( javaType, jdbcType );
		assert primitiveClass.isPrimitive();
		this.primitiveClass = primitiveClass;
	}

	@Override
	public Class<J> getJavaType() {
		return primitiveClass;
	}
}
