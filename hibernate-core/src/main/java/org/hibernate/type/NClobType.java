/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.NClob;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.descriptor.java.NClobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link java.sql.Clob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class NClobType extends AbstractSingleColumnStandardBasicType<NClob> {
	public static final NClobType INSTANCE = new NClobType();

	public NClobType() {
		super( org.hibernate.type.descriptor.sql.NClobTypeDescriptor.DEFAULT, NClobTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "nclob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	protected NClob getReplacement(NClob original, NClob target, SessionImplementor session) {
		return session.getFactory().getDialect().getLobMergeStrategy().mergeNClob( original, target, session );
	}

}
