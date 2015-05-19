/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import java.sql.Clob;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.descriptor.java.ClobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link Clob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ClobType extends AbstractSingleColumnStandardBasicType<Clob> {
	public static final ClobType INSTANCE = new ClobType();

	public ClobType() {
		super( org.hibernate.type.descriptor.sql.ClobTypeDescriptor.DEFAULT, ClobTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "clob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	protected Clob getReplacement(Clob original, Clob target, SessionImplementor session) {
		return session.getFactory().getDialect().getLobMergeStrategy().mergeClob( original, target, session );
	}

}
