/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetTime;

import org.hibernate.type.descriptor.java.OffsetTimeJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimeJdbcTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class OffsetTimeType
		extends AbstractSingleColumnStandardBasicType<OffsetTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetTimeType INSTANCE = new OffsetTimeType();

	public OffsetTimeType() {
		super( TimeJdbcTypeDescriptor.INSTANCE, OffsetTimeJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return OffsetTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

}
