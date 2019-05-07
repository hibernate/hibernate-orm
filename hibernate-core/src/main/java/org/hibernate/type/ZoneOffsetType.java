/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import java.time.ZoneId;
import java.time.ZoneOffset;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.ZoneOffsetJavaDescriptor;
import org.hibernate.type.descriptor.sql.IntegerTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link ZoneId}.
 *
 * @author Steve Ebersole
 */
public class ZoneOffsetType
		extends AbstractSingleColumnStandardBasicType<ZoneOffset>
		implements LiteralType<ZoneOffset> {

	/**
	 * Singleton access
	 */
	public static final ZoneOffsetType INSTANCE = new ZoneOffsetType();

	public ZoneOffsetType() {
		super( IntegerTypeDescriptor.INSTANCE, ZoneOffsetJavaDescriptor.INSTANCE );
	}

	@Override
	public String objectToSQLString(ZoneOffset value, Dialect dialect) throws Exception {
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
