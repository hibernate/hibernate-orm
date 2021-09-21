/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.time.LocalTime;
import jakarta.persistence.TemporalType;

import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.type.descriptor.java.LocalTimeJavaDescriptor;
import org.hibernate.type.descriptor.jdbc.TimeTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link java.sql.Types#TIMESTAMP TIMESTAMP} and {@link java.time.LocalDateTime}.
 *
 * @author Steve Ebersole
 */
public class LocalTimeType
		extends AbstractSingleColumnStandardBasicType<LocalTime>
		implements LiteralType<LocalTime>, AllowableTemporalParameterType<LocalTime> {
	/**
	 * Singleton access
	 */
	public static final LocalTimeType INSTANCE = new LocalTimeType();

	public LocalTimeType() {
		super( TimeTypeDescriptor.INSTANCE, LocalTimeJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return LocalTime.class.getSimpleName();
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
				return LocalDateTimeType.INSTANCE;
			}
			case DATE: {
				return LocalDateType.INSTANCE;
			}
		}
		// Why Java?  Why?
		return null;
	}
}
