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

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.java.internal.EnumJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link javax.persistence.EnumType#STRING} strategy (storing the name)
 *
 * @author Steve Ebersole
 */
public class NamedEnumValueConverter<E extends Enum> implements EnumValueConverter<E,String>, Serializable {
	private static final Logger log = Logger.getLogger( NamedEnumValueConverter.class );

	private final EnumJavaDescriptor<E> enumJavaDescriptor;
	private final BasicJavaDescriptor<String> relationalJavaDescriptor;

	public NamedEnumValueConverter(
			EnumJavaDescriptor<E> enumJavaDescriptor,
			RuntimeModelCreationContext creationContext) {
		this.enumJavaDescriptor = enumJavaDescriptor;

		final TypeConfiguration typeConfiguration = creationContext.getTypeConfiguration();
		this.relationalJavaDescriptor = typeConfiguration.getSqlTypeDescriptorRegistry()
				.getDescriptor( getJdbcTypeCode() )
				.getJdbcRecommendedJavaTypeMapping( typeConfiguration );
	}

	@Override
	public E toDomainValue(String relationalForm, SharedSessionContractImplementor session) {
		return enumJavaDescriptor.fromName( relationalForm );
	}

	@Override
	public String toRelationalValue(E domainForm, SharedSessionContractImplementor session) {
		return enumJavaDescriptor.toName( domainForm );
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.VARCHAR;
	}

	@Override
	public EnumJavaDescriptor<E> getDomainJavaDescriptor() {
		return enumJavaDescriptor;
	}

	@Override
	public BasicJavaDescriptor<String> getRelationalJavaDescriptor() {
		return relationalJavaDescriptor;
	}

	@Override
	public E readValue(
			ResultSet resultSet,
			String name,
			SharedSessionContractImplementor session) throws SQLException {
		final String value = resultSet.getString( name );

		final boolean traceEnabled = log.isTraceEnabled();
		if ( resultSet.wasNull() ) {
			if ( traceEnabled ) {
				log.trace( String.format( "Returning null as column [%s]", name ) );
			}
			return null;
		}

		final E enumValue = toDomainValue( value, session );
		if ( traceEnabled ) {
			log.trace( String.format( "Returning [%s] as column [%s]", enumValue, name ) );
		}

		return enumValue;
	}

	@Override
	public void writeValue(
			PreparedStatement statement,
			E value,
			int position,
			SharedSessionContractImplementor session) throws SQLException {
		final String jdbcValue = value == null ? null : toRelationalValue( value, session );

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
