/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.ObjectSqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * JavaTypeDescriptor for Object.  Intended for use in cases where we do not know
 * a more specific Java type
 *
 * @author Steve Ebersole
 */
public class ObjectJavaDescriptor<T> extends AbstractBasicJavaDescriptor<T> implements BasicJavaDescriptor<T> {
	/**
	 * Singleton access
	 */
	public static final ObjectJavaDescriptor INSTANCE = new ObjectJavaDescriptor();

	@SuppressWarnings("unchecked")
	private ObjectJavaDescriptor() {
		this( (Class<T>) Object.class );
	}

	@SuppressWarnings("WeakerAccess")
	public ObjectJavaDescriptor(Class<T> type) {
		super( type );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
		return ObjectSqlTypeDescriptor.INSTANCE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(T value, Class<X> type, SharedSessionContractImplementor session) {
		return (X) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> T wrap(X value, SharedSessionContractImplementor session) {
		return (T) value;
	}
}
