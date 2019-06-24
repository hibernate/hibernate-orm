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

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.java.spi.EnumJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link javax.persistence.EnumType#ORDINAL} strategy (storing the ordinal)
 *
 * @author Steve Ebersole
 */
public class OrdinalEnumValueConverter<E extends Enum> implements EnumValueConverter<E,Byte>, Serializable {
	private static final Logger log = Logger.getLogger( OrdinalEnumValueConverter.class );

	private final EnumJavaDescriptor<E> enumJavaDescriptor;
	private final BasicJavaDescriptor<Byte> relationalJavaDescriptor;

	public OrdinalEnumValueConverter(
			EnumJavaDescriptor<E> enumJavaDescriptor,
			RuntimeModelCreationContext creationContext) {
		this(
				enumJavaDescriptor,
				creationContext.getTypeConfiguration()
						.getSqlTypeDescriptorRegistry()
						.getDescriptor( Types.TINYINT )
						.getJdbcRecommendedJavaTypeMapping( creationContext.getTypeConfiguration() )
		);
	}

	public OrdinalEnumValueConverter(EnumJavaDescriptor<E> enumJavaDescriptor, TypeConfiguration typeConfiguration) {
		this(
				enumJavaDescriptor,
				typeConfiguration.getSqlTypeDescriptorRegistry()
						.getDescriptor( Types.TINYINT )
						.getJdbcRecommendedJavaTypeMapping( typeConfiguration )
		);
	}

	public OrdinalEnumValueConverter(
			EnumJavaDescriptor<E> enumJavaDescriptor,
			BasicJavaDescriptor<Byte> relationalJavaDescriptor) {
		this.enumJavaDescriptor = enumJavaDescriptor;
		this.relationalJavaDescriptor = relationalJavaDescriptor;
	}

	@Override
	public E toDomainValue(Byte relationalForm, SharedSessionContractImplementor session) {
		return enumJavaDescriptor.fromOrdinal( relationalForm );
	}

	@Override
	public Byte toRelationalValue(E domainForm, SharedSessionContractImplementor session) {
		return enumJavaDescriptor.toOrdinal( domainForm );
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.TINYINT;
	}

	@Override
	public EnumJavaDescriptor<E> getDomainJavaDescriptor() {
		return enumJavaDescriptor;
	}

	@Override
	public BasicJavaDescriptor<Byte> getRelationalJavaDescriptor() {
		return relationalJavaDescriptor;
	}

	@Override
	public E readValue(
			ResultSet resultSet,
			String name,
			SharedSessionContractImplementor session) throws SQLException {
		final byte ordinal = resultSet.getByte( name );
		final boolean traceEnabled = log.isTraceEnabled();
		if ( resultSet.wasNull() ) {
			if ( traceEnabled ) {
				log.trace(String.format("Returning null as column [%s]", name));
			}
			return null;
		}

		final E enumValue = toDomainValue( ordinal, session );
		if ( traceEnabled ) {
			log.trace(String.format("Returning [%s] as column [%s]", enumValue, name));
		}

		return enumValue;
	}

	@Override
	public void writeValue(
			PreparedStatement statement,
			E value,
			int position,
			SharedSessionContractImplementor session) throws SQLException {
		final Byte jdbcValue = value == null ? null : toRelationalValue( value, session );

		final boolean traceEnabled = log.isTraceEnabled();
		if ( jdbcValue == null ) {
			if ( traceEnabled ) {
				log.tracef( "Binding null to parameter: [%s]", position );
			}
			statement.setNull( position, getJdbcTypeCode() );
			return;
		}

		if ( traceEnabled ) {
			log.tracef( "Binding [%s] to parameter: [%s]", jdbcValue.intValue(), position );
		}

		statement.setByte( position, jdbcValue );
	}

	@Override
	@SuppressWarnings("unchecked")
	public String toSqlLiteral(Object value) {
		return Integer.toString( ( (E) value ).ordinal() );
	}
}
