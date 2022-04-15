/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.converter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.PersistenceException;

import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.jboss.logging.Logger;

/**
 * Adapts the Hibernate Type contract to incorporate JPA AttributeConverter calls.
 *
 * @author Steve Ebersole
 */
public class AttributeConverterTypeAdapter<T> extends AbstractSingleColumnStandardBasicType<T> {
	private static final Logger log = Logger.getLogger( AttributeConverterTypeAdapter.class );

	@SuppressWarnings("unused")
	public static final String NAME_PREFIX = ConverterDescriptor.TYPE_NAME_PREFIX;

	private final String name;
	private final String description;

	private final JavaType<T> domainJtd;
	private final JavaType<?> relationalJtd;
	private final JpaAttributeConverter<T, Object> attributeConverter;

	private final MutabilityPlan<T> mutabilityPlan;
	private final ValueBinder<Object> valueBinder;

	@SuppressWarnings("unchecked")
	public AttributeConverterTypeAdapter(
			String name,
			String description,
			JpaAttributeConverter<? extends T, ?> attributeConverter,
			JdbcType std,
			JavaType<?> relationalJtd,
			JavaType<T> domainJtd,
			MutabilityPlan<T> mutabilityPlan) {
		super( std, (JavaType<T>) relationalJtd );
		this.name = name;
		this.description = description;
		this.domainJtd = domainJtd;
		this.relationalJtd = relationalJtd;
		this.attributeConverter = (JpaAttributeConverter<T, Object>) attributeConverter;

		// NOTE : the way that JpaAttributeConverter get built, their "domain JTD" already
		// contains the proper MutabilityPlan based on whether the `@Immutable` is present
		if ( mutabilityPlan == null ) {
			this.mutabilityPlan = (MutabilityPlan<T>) attributeConverter.getDomainJavaType().getMutabilityPlan();
		}
		else {
			this.mutabilityPlan = mutabilityPlan;
		}
		this.valueBinder = getJdbcType().getBinder( (JavaType<Object>) relationalJtd );

		log.debugf( "Created AttributeConverterTypeAdapter -> %s", name );
	}

	@Override
	public String getName() {
		return name;
	}

	public JavaType<T> getDomainJtd() {
		return domainJtd;
	}

	public JavaType<?> getRelationalJtd() {
		return relationalJtd;
	}

	public JpaAttributeConverter<? extends T, ?> getAttributeConverter() {
		return attributeConverter;
	}

	@Override
	public void nullSafeSet(
			CallableStatement st,
			T value,
			String name,
			SharedSessionContractImplementor session) throws SQLException {
		final AttributeConverter<T, Object> converter = attributeConverter.getConverterBean().getBeanInstance();
		final Object converted = getConvertedValue( converter, value );
		valueBinder.bind( st, converted, name, session );
	}

	@Override
	protected void nullSafeSet(PreparedStatement st, T value, int index, WrapperOptions options) throws SQLException {
		final AttributeConverter<T, Object> converter = attributeConverter.getConverterBean().getBeanInstance();
		final Object converted = getConvertedValue( converter, value );
		valueBinder.bind( st, converted, index, options );
	}

	@Override
	protected MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public boolean isEqual(Object one, Object another) {
		//noinspection unchecked
		return ( (JavaType<Object>) getDomainJtd() ).areEqual( one, another );
	}

	@Override
	public int getHashCode(Object x) {
		//noinspection unchecked
		return getDomainJtd().extractHashCode( (T) x );
	}

	@Override
	public String toString() {
		return description;
	}

	private Object getConvertedValue(AttributeConverter<T, Object> converter, T value) {
		try {
			return converter.convertToDatabaseColumn( value );
		}
		catch (PersistenceException pe) {
			throw pe;
		}
		catch (RuntimeException re) {
			throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
		}
	}
}
