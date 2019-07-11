/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Emmanuel Bernard
 */
public class BasicTypeImpl<J> implements BasicDomainType<J>, Serializable {
	private final JavaTypeDescriptor<J> javaTypeDescriptor;

	public BasicTypeImpl(JavaTypeDescriptor<J> javaTypeDescriptor) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public JavaTypeDescriptor<J> getExpressableJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public Class<J> getJavaType() {
		return getExpressableJavaTypeDescriptor().getJavaType();
	}

	@Override
	public boolean canDoExtraction() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public J extract(
			CallableStatement statement, int paramIndex, SharedSessionContractImplementor session) throws SQLException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public J extract(
			CallableStatement statement, String paramName, SharedSessionContractImplementor session)
			throws SQLException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
