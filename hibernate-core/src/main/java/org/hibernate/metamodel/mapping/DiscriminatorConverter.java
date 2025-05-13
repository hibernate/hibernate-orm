/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public abstract class DiscriminatorConverter<O,R> implements BasicValueConverter<O,R> {
	private final String discriminatorName;
	private final JavaType<O> domainJavaType;
	private final JavaType<R> relationalJavaType;

	public DiscriminatorConverter(
			String discriminatorName,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType) {
		this.discriminatorName = discriminatorName;
		this.domainJavaType = domainJavaType;
		this.relationalJavaType = relationalJavaType;
	}

	public String getDiscriminatorName() {
		return discriminatorName;
	}

	@Override
	public JavaType<O> getDomainJavaType() {
		return domainJavaType;
	}

	@Override
	public JavaType<R> getRelationalJavaType() {
		return relationalJavaType;
	}

	public DiscriminatorValueDetails getDetailsForRelationalForm(R relationalForm) {
		return getDetailsForDiscriminatorValue( relationalForm );
	}

	@Override
	public O toDomainValue(R relationalForm) {
		assert relationalForm == null || relationalJavaType.isInstance( relationalForm );

		final DiscriminatorValueDetails matchingValueDetails = getDetailsForRelationalForm( relationalForm );
		if ( matchingValueDetails == null ) {
			throw new IllegalStateException( "Could not resolve discriminator value" );
		}

		final EntityMappingType indicatedEntity = matchingValueDetails.getIndicatedEntity();
		//noinspection unchecked
		return indicatedEntity.getRepresentationStrategy().getMode() == RepresentationMode.POJO
			&& indicatedEntity.getEntityName().equals( indicatedEntity.getJavaType().getJavaTypeClass().getName() )
				? (O) indicatedEntity.getJavaType().getJavaTypeClass()
				: (O) indicatedEntity.getEntityName();
	}

	@Override
	public R toRelationalValue(O domainForm) {
		final String entityName;
		if ( domainForm == null ) {
			return null;
		}
		else if ( domainForm instanceof Class<?> clazz ) {
			entityName = clazz.getName();
		}
		else if ( domainForm instanceof String name ) {
			entityName = name;
		}
		else {
			throw new IllegalArgumentException( "Illegal discriminator value: " + domainForm );
		}

		final DiscriminatorValueDetails discriminatorValueDetails = getDetailsForEntityName( entityName );
		//noinspection unchecked
		return (R) discriminatorValueDetails.getValue();
	}

	public abstract DiscriminatorValueDetails getDetailsForDiscriminatorValue(Object relationalValue);

	public abstract DiscriminatorValueDetails getDetailsForEntityName(String entityName);

	@Override
	public String toString() {
		return "DiscriminatorConverter(" + discriminatorName + ")";
	}

	public abstract void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer);

	/**
	 * Find and return the first DiscriminatorValueDetails which matches the given {@code handler}
	 */
	public abstract <X> X fromValueDetails(Function<DiscriminatorValueDetails,X> handler);
}
