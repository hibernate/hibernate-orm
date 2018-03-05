/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.convert.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link javax.persistence.EnumType#STRING} strategy (storing the name)
 *
 * @author Steve Ebersole
 */
public class NamedEnumValueConverter<E extends Enum> implements EnumValueConverter<E,String>, Serializable {
	private static final Logger log = Logger.getLogger( NamedEnumValueConverter.class );

	private final EnumJavaTypeDescriptor<E> enumJavaDescriptor;

	public NamedEnumValueConverter(EnumJavaTypeDescriptor<E> enumJavaDescriptor) {
		this.enumJavaDescriptor = enumJavaDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E toDomainValue(String relationalForm) {
		return enumJavaDescriptor.fromName( relationalForm );
	}

	@Override
	public String toRelationalValue(E domainForm) {
		return enumJavaDescriptor.toName( domainForm );
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public EnumJavaTypeDescriptor<E> getJavaDescriptor() {
		return enumJavaDescriptor;
	}

	@Override
	public E readValue(ResultSet resultSet, String name) throws SQLException {
		final String value = resultSet.getString( name );

		final boolean traceEnabled = log.isTraceEnabled();
		if ( resultSet.wasNull() ) {
			if ( traceEnabled ) {
				log.trace( String.format( "Returning null as column [%s]", name ) );
			}
			return null;
		}

		final E enumValue = toDomainValue( value );
		if ( traceEnabled ) {
			log.trace( String.format( "Returning [%s] as column [%s]", enumValue, name ) );
		}

		return enumValue;
	}

	@Override
	public void writeValue(PreparedStatement statement, E value, int position) throws SQLException {
		final String jdbcValue = value == null ? null : toRelationalValue( value );

		final boolean traceEnabled = log.isTraceEnabled();
		if ( jdbcValue == null ) {
			if ( traceEnabled ) {
				log.tracef( "Binding null to parameter: [%s]", position );
			}
			statement.setNull( position, getJdbcTypeCode() );
			return;
		}

		if ( traceEnabled ) {
			log.tracef( "Binding [%s] to parameter: [%s]", jdbcValue, position );
		}

		statement.setString( position, jdbcValue );
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toSqlLiteral(Object value) {
		return String.format( Locale.ROOT, "'%s'", ( (E) value ).name() );
	}
}
