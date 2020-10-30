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

import javax.persistence.AttributeConverter;

import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

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

	private final JavaTypeDescriptor<T> domainJtd;
	private final JavaTypeDescriptor<?> relationalJtd;
	private final JpaAttributeConverter<? extends T,?> attributeConverter;

	private final MutabilityPlan<T> mutabilityPlan;

	@SuppressWarnings("unchecked")
	public AttributeConverterTypeAdapter(
			String name,
			String description,
			JpaAttributeConverter<? extends T,?> attributeConverter,
			SqlTypeDescriptor std,
			JavaTypeDescriptor<?> relationalJtd,
			JavaTypeDescriptor<T> domainJtd,
			MutabilityPlan<T> mutabilityPlan) {
		//noinspection rawtypes
		super( std, (JavaTypeDescriptor) relationalJtd );
		this.name = name;
		this.description = description;
		this.domainJtd = domainJtd;
		this.relationalJtd = relationalJtd;
		this.attributeConverter = attributeConverter;

		if ( mutabilityPlan == null ) {
			this.mutabilityPlan = domainJtd.getMutabilityPlan().isMutable()
					? new AttributeConverterMutabilityPlanImpl<>( attributeConverter, true )
					: ImmutableMutabilityPlan.INSTANCE;
		}
		else {
			this.mutabilityPlan = mutabilityPlan;
		}

		log.debugf( "Created AttributeConverterTypeAdapter -> %s", name );
	}

	@Override
	public String getName() {
		return name;
	}

	public JavaTypeDescriptor<T> getDomainJtd() {
		return domainJtd;
	}

	public JavaTypeDescriptor<?> getRelationalJtd() {
		return relationalJtd;
	}

	public JpaAttributeConverter<? extends T,?> getAttributeConverter() {
		return attributeConverter;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void nullSafeSet(
			CallableStatement st,
			Object value,
			String name,
			SharedSessionContractImplementor session) throws SQLException {
		final AttributeConverter converter = attributeConverter.getConverterBean().getBeanInstance();
		final Object converted = converter.convertToDatabaseColumn( value );
		super.nullSafeSet( st, converted, name, session );
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options) throws SQLException {
		final AttributeConverter converter = attributeConverter.getConverterBean().getBeanInstance();
		final Object converted = converter.convertToDatabaseColumn( value );
		super.nullSafeSet( st, converted, index, options );
	}


	@Override
	protected MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public String toString() {
		return description;
	}
}
