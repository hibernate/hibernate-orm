/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.ZonedDateTime;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.query.CastType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * @author Steve Ebersole
 */
public class ZonedDateTimeType
		extends AbstractSingleColumnStandardBasicType<ZonedDateTime>
		implements AllowableTemporalParameterType<ZonedDateTime> {

	/**
	 * Singleton access
	 */
	public static final ZonedDateTimeType INSTANCE = new ZonedDateTimeType();

	public ZonedDateTimeType() {
		super( TimestampWithTimeZoneDescriptor.INSTANCE, ZonedDateTimeJavaTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return ZonedDateTime.class.getSimpleName();
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public AllowableTemporalParameterType resolveTemporalPrecision(
			TemporalType temporalPrecision,
			TypeConfiguration typeConfiguration) {
		switch ( temporalPrecision ) {
			case TIMESTAMP: {
				return this;
			}
			default: {
				throw new QueryException( "ZonedDateTime type cannot be treated using `" + temporalPrecision.name() + "` precision" );
			}
		}
	}

	@Override
	public CastType getCastType() {
		return CastType.ZONE_TIMESTAMP;
	}
}
