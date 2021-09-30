/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.OffsetTime;
import jakarta.persistence.TemporalType;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaDescriptor;
import org.hibernate.type.descriptor.jdbc.TimeTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class OffsetTimeType
		extends AbstractSingleColumnStandardBasicType<OffsetTime>
		implements LiteralType<OffsetTime>, AllowableTemporalParameterType<OffsetTime> {

	/**
	 * Singleton access
	 */
	public static final OffsetTimeType INSTANCE = new OffsetTimeType();

	public OffsetTimeType() {
		super( TimeTypeDescriptor.INSTANCE, OffsetTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return OffsetTime.class.getSimpleName();
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
			case TIME: {
				return this;
			}
			case TIMESTAMP: {
				return OffsetDateTimeType.INSTANCE;
			}
			default: {
				throw new QueryException( "OffsetTime type cannot be treated using `" + temporalPrecision.name() + "` precision" );
			}
		}
	}
}
