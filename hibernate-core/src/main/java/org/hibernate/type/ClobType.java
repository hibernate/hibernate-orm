/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Clob;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.ClobJavaDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link Clob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ClobType extends BasicTypeImpl<Clob> {
	public static final ClobType INSTANCE = new ClobType();

	public ClobType() {
		super( ClobJavaDescriptor.INSTANCE, ClobSqlDescriptor.DEFAULT );
	}

	@Override
	public String getName() {
		return "clob";
	}

	@Override
	public Clob getReplacement(Clob original, Clob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeClob( original, target, session );
	}

	@Override
	public JdbcLiteralFormatter<Clob> getJdbcLiteralFormatter() {
		// no support for CLOB literals
		return null;
	}
}
