/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;


import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.JavaObjectTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Specific adaptation of the "any" type to the old deprecated "object" type
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ObjectType extends AnyType implements BasicType {
	/**
	 * Singleton access
	 */
	public static final ObjectType INSTANCE = new ObjectType();

	private ObjectType() {
		super( StringType.INSTANCE, SerializableType.INSTANCE );
	}

	@Override
	public String getName() {
		return "object";
	}

	@Override
	public JavaTypeDescriptor getExpressableJavaTypeDescriptor() {
		return JavaObjectTypeDescriptor.INSTANCE;
	}

	@Override
	public Class getJavaType() {
		return Object.class;
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), Object.class.getName() };
	}

	@Override
	public boolean canDoExtraction() {
		return false;
	}

	@Override
	public Object extract(
			CallableStatement statement,
			String paramName,
			SharedSessionContractImplementor session) throws SQLException {
		throw new UnsupportedOperationException( "Cannot extract ANY-valued data from CallableStatements" );
	}

	@Override
	public Object extract(
			CallableStatement statement,
			int paramIndex,
			SharedSessionContractImplementor session) throws SQLException {
		throw new UnsupportedOperationException( "Cannot extract ANY-valued data from CallableStatements" );
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		throw new UnsupportedOperationException( "Cannot extract ANY-valued data from CallableStatements" );
	}
}
