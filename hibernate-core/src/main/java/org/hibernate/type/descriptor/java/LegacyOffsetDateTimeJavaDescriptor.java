/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import javax.persistence.TemporalType;

import org.hibernate.type.descriptor.jdbc.DateTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.descriptor.jdbc.TimeTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampTypeDescriptor;

public class LegacyOffsetDateTimeJavaDescriptor extends OffsetDateTimeJavaDescriptor{

	public static LegacyOffsetDateTimeJavaDescriptor INSTANCE = new LegacyOffsetDateTimeJavaDescriptor();

	@Override
	public JdbcTypeDescriptor getRecommendedJdbcType(JdbcTypeDescriptorIndicators stdIndicators) {
		final TemporalType temporalPrecision = stdIndicators.getTemporalPrecision();

		if ( temporalPrecision == null || temporalPrecision == TemporalType.TIMESTAMP ) {
			return TimestampTypeDescriptor.INSTANCE;
		}

		switch ( temporalPrecision ) {
			case TIME: {
				return TimeTypeDescriptor.INSTANCE;
			}
			case DATE: {
				return DateTypeDescriptor.INSTANCE;
			}
			default: {
				throw new IllegalArgumentException( "Unexpected javax.persistence.TemporalType : " + temporalPrecision );
			}
		}
	}
}
