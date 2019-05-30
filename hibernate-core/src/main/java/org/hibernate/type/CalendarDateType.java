/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Calendar;

import javax.persistence.TemporalType;

import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.AllowableTemporalParameterType;
import org.hibernate.type.descriptor.java.CalendarDateTypeDescriptor;
import org.hibernate.type.descriptor.sql.DateTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A type mapping {@link java.sql.Types#DATE DATE} and {@link Calendar}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CalendarDateType
		extends AbstractSingleColumnStandardBasicType<Calendar>
		implements AllowableTemporalParameterType {
	public static final CalendarDateType INSTANCE = new CalendarDateType();

	public CalendarDateType() {
		super( DateTypeDescriptor.INSTANCE, CalendarDateTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "calendar_date";
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
				return CalendarTimeType.INSTANCE;
			}
			case TIMESTAMP: {
				return CalendarType.INSTANCE;
			}
		}

		throw new QueryException( "Calendar-date type cannot be treated using `" + temporalPrecision.name() + "` precision" );
	}
}
