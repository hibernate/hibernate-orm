/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetDateTime;
import java.util.Comparator;

import jakarta.persistence.TemporalType;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.query.CastType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampWithTimeZoneDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class OffsetDateTimeType
		extends AbstractSingleColumnStandardBasicType<OffsetDateTime>
		implements VersionType<OffsetDateTime>, AllowableTemporalParameterType<OffsetDateTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetDateTimeType INSTANCE = new OffsetDateTimeType();

	public OffsetDateTimeType() {
		super( TimestampWithTimeZoneDescriptor.INSTANCE, OffsetDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public OffsetDateTime seed(SharedSessionContractImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	public OffsetDateTime next(OffsetDateTime current, SharedSessionContractImplementor session) {
		return OffsetDateTime.now();
	}

	@Override
	public Comparator<OffsetDateTime> getComparator() {
		return OffsetDateTime.timeLineOrder();
	}

	@Override
	public String getName() {
		return OffsetDateTime.class.getSimpleName();
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
			case TIME: {
				return OffsetTimeType.INSTANCE;
			}
			case DATE: {
				return DateType.INSTANCE;
			}
			default: {
				// should never happen, but switch requires this branch so...
				throw new QueryException( "OffsetDateTime type cannot be treated using `" + temporalPrecision.name() + "` precision" );
			}
		}
	}

	@Override
	public CastType getCastType() {
		return CastType.OFFSET_TIMESTAMP;
	}
}
