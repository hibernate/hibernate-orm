/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import java.time.ZoneId;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.ZoneIdJavaDescriptor;
import org.hibernate.type.descriptor.sql.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link ZoneId}.
 *
 * @author Steve Ebersole
 */
public class ZoneIdType
		extends AbstractSingleColumnStandardBasicType<ZoneId>
		implements LiteralType<ZoneId> {

	/**
	 * Singleton access
	 */
	public static final ZoneIdType INSTANCE = new ZoneIdType();

	public ZoneIdType() {
		super( VarcharTypeDescriptor.INSTANCE, ZoneIdJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(ZoneId value, Dialect dialect) throws Exception {
		return String.valueOf( value.getId() );
	}

	@Override
	public String getName() {
		return ZoneId.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
