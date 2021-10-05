/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Date;

import jakarta.persistence.TemporalType;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.type.descriptor.java.JdbcDateTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type that maps between {@link java.sql.Types#DATE DATE} and {@link java.sql.Date}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DateType
		extends AbstractSingleColumnStandardBasicType<Date>
		implements IdentifierType<Date>, AllowableTemporalParameterType<Date> {

	public static final DateType INSTANCE = new DateType();

	public DateType() {
		super( org.hibernate.type.descriptor.jdbc.DateTypeDescriptor.INSTANCE, JdbcDateTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "date";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] {
				getName(),
				java.sql.Date.class.getName()
		};
	}

//	@Override
//	protected boolean registerUnderJavaType() {
//		return true;
//	}

	public Date stringToObject(CharSequence sequence) {
		return fromString( sequence );
	}

	@Override
	public AllowableTemporalParameterType resolveTemporalPrecision(
			TemporalType temporalPrecision,
			TypeConfiguration typeConfiguration) {
		switch ( temporalPrecision ) {
			case DATE: {
				return this;
			}
			case TIME: {
				return TimeType.INSTANCE;
			}
			case TIMESTAMP: {
				return TimestampType.INSTANCE;
			}
		}
		throw new QueryException( "Date type cannot be treated using `" + temporalPrecision.name() + "` precision" );
	}
}
