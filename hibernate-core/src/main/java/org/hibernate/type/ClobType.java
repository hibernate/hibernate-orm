/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Clob;
import java.sql.Types;

import org.hibernate.type.descriptor.java.ClobJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;

/**
 * A type that maps between {@link Types#CLOB CLOB} and {@link Clob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ClobType extends AbstractSingleColumnStandardBasicType<Clob> implements AdjustableBasicType<Clob> {
	public static final ClobType INSTANCE = new ClobType();

	public ClobType() {
		super( ClobJdbcType.DEFAULT, ClobJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "clob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
