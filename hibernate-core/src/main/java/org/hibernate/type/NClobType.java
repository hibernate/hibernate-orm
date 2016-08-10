/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.NClob;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.NClobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#NCLOB NCLOB} and {@link java.sql.NClob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class NClobType extends AbstractSingleColumnStandardBasicType<NClob> {
	public static final NClobType INSTANCE = new NClobType();

	protected NClobType() {
		super( org.hibernate.type.spi.descriptor.sql.NClobTypeDescriptor.DEFAULT, NClobTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "nclob";
	}

	@Override
	public NClob getReplacement(NClob original, NClob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeNClob( original, target, session );
	}

	@Override
	public JdbcLiteralFormatter<NClob> getJdbcLiteralFormatter() {
		// no literal support for NCLOB
		return null;
	}
}
