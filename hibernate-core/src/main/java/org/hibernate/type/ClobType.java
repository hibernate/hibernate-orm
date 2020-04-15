/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Clob;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link Clob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ClobType extends AbstractSingleColumnStandardBasicType<Clob> implements SqlTypeDescriptorIndicatorCapable<Clob> {
	public static final ClobType INSTANCE = new ClobType();

	public ClobType() {
		super( org.hibernate.type.descriptor.sql.ClobTypeDescriptor.DEFAULT, ClobTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "clob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	protected Clob getReplacement(Clob original, Clob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeClob( original, target, session );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public BasicType resolveIndicatedType(SqlTypeDescriptorIndicators indicators) {
		// todo (6.0) : Support a "wrapped clob"?  This would be a (N)VARCHAR column we handle as a Clob in-memory
		//		- might be especially interesting for streaming based (N)VARCHAR reading

		if ( indicators.isNationalized() ) {
			return NClobType.INSTANCE;
		}

		return this;
	}
}
