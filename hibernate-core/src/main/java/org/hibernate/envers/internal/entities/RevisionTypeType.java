/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.type.descriptor.sql.spi.TinyIntSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.BasicType;

/**
 * A hibernate type for the {@link RevisionType} enum.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class RevisionTypeType extends BasicTypeImpl<RevisionType> {
	public static final RevisionTypeType INSTANCE = new RevisionTypeType();

	public RevisionTypeType() {
		super( RevisionTypeJavaDescriptor.INSTANCE, TinyIntSqlDescriptor.INSTANCE );
	}

	@Override
	public String asLoggableText() {
		return getTypeName();
	}

	@Override
	public Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException, SQLException {
		final BasicType<Integer> integerType = getIntegerType( session );
		final Integer value = integerType.nullSafeGet( rs, names[0], session );
		return value != null ? RevisionType.fromRepresentation( value.byteValue() ) : null;
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws HibernateException, SQLException {
		final BasicType<Integer> integerType = getIntegerType( session );
		integerType.nullSafeSet(
				st,
				( value == null ? null : ( (RevisionType) value ).getRepresentation().intValue() ),
				index,
				session
		);
	}

	private BasicType<Integer> getIntegerType(SharedSessionContractImplementor session) {
		return session.getFactory().getTypeConfiguration().getBasicTypeRegistry().getBasicType( Integer.class );
	}

	@Override
	public RevisionType replace(RevisionType original,
			RevisionType target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache) throws HibernateException {
		return null;
	}
}
