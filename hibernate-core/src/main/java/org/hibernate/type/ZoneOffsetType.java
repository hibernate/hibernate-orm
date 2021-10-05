/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.ZoneOffsetJavaDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharTypeDescriptor;

import java.time.ZoneOffset;

/**
 * A type mapping {@link java.sql.Types#VARCHAR VARCHAR} and {@link ZoneOffset}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ZoneOffsetType
		extends AbstractSingleColumnStandardBasicType<ZoneOffset> {

	public static final ZoneOffsetType INSTANCE = new ZoneOffsetType();

	public ZoneOffsetType() {
		super( VarcharTypeDescriptor.INSTANCE, ZoneOffsetJavaDescriptor.INSTANCE );
	}

	public String getName() {
		return ZoneOffset.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

}
