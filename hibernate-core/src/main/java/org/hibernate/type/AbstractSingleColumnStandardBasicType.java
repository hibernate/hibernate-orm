/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSingleColumnStandardBasicType<T>
		extends BasicTypeImpl<T,Object>
		implements SingleColumnType<T> {

	public AbstractSingleColumnStandardBasicType(SqlTypeDescriptor sqlTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		super(
				javaTypeDescriptor,
				sqlTypeDescriptor,
				javaTypeDescriptor.getMutabilityPlan(),
				javaTypeDescriptor.getComparator()
		);
	}

	@Override
	public final int sqlType() {
		return getSqlTypeDescriptor().getSqlType();
	}

	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return getColumnMapping().getSqlTypeDescriptor();
	}

	@Override
	public final void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}
}
