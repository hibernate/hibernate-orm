/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Date;

import javax.persistence.TemporalType;

import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
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
		implements IdentifierType<Date>, LiteralType<Date>, AllowableTemporalParameterType<Date> {

	public static final DateType INSTANCE = new DateType();

	public DateType() {
		super( org.hibernate.type.descriptor.sql.DateTypeDescriptor.INSTANCE, JdbcDateTypeDescriptor.INSTANCE );
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

	public String objectToSQLString(Date value, Dialect dialect) throws Exception {
		final java.sql.Date jdbcDate = java.sql.Date.class.isInstance( value )
				? ( java.sql.Date ) value
				: new java.sql.Date( value.getTime() );
		// TODO : use JDBC date literal escape syntax? -> {d 'date-string'} in yyyy-mm-dd format
		return StringType.INSTANCE.objectToSQLString( jdbcDate.toString(), dialect );
	}

	public Date stringToObject(String xml) {
		return fromString( xml );
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
