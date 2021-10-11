/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetDateTime;

import org.hibernate.type.descriptor.java.OffsetDateTimeJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneJdbcTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetDateTimeType
		extends AbstractSingleColumnStandardBasicType<OffsetDateTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetDateTimeType INSTANCE = new OffsetDateTimeType();

	public OffsetDateTimeType() {
		super( TimestampWithTimeZoneJdbcTypeDescriptor.INSTANCE, OffsetDateTimeJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return OffsetDateTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
