/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.LocalDateTime;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.jdbc.TimestampTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class LocalDateTimeType
		extends AbstractSingleColumnStandardBasicType<LocalDateTime>
		implements AllowableTemporalParameterType<LocalDateTime> {
	/**
	 * Singleton access
	 */
	public static final LocalDateTimeType INSTANCE = new LocalDateTimeType();

	public LocalDateTimeType() {
		super( TimestampTypeDescriptor.INSTANCE, LocalDateTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LocalDateTime.class.getSimpleName();
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
			case DATE: {
				return LocalDateType.INSTANCE;
			}
			default: {
				throw new QueryException( "LocalDateTime type cannot be treated using `" + temporalPrecision.name() + "` precision" );
			}
		}
	}
}
