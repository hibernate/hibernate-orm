/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
			@Nonnull String discriminatorName,
			@Nonnull JavaType<O> domainJavaType,
			@Nonnull JavaType<R> relationalJavaType) {
		this.discriminatorName = discriminatorName;
		this.domainJavaType = domainJavaType;
		this.relationalJavaType = relationalJavaType;
	}

	@Nonnull
	public String getDiscriminatorName() {
		return discriminatorName;
	}

	@Nonnull
	@Override
	public JavaType<O> getDomainJavaType() {
		return domainJavaType;
	}

	@Nonnull
	@Override
	public JavaType<R> getRelationalJavaType() {
		return relationalJavaType;
	}

	@Nullable
	public DiscriminatorValueDetails getDetailsForRelationalForm(@Nullable R relationalForm) {
		return getDetailsForDiscriminatorValue( relationalForm );
	}

	@Nonnull
	@Override
	public O toDomainValue(@Nullable R relationalForm) {
		assert relationalForm == null || relationalJavaType.isInstance( relationalForm );
		final var matchingValueDetails = getDetailsForRelationalForm( relationalForm );
		if ( matchingValueDetails == null ) {
			throw new IllegalStateException( "Could not resolve discriminator value" );
		}

		final var indicatedEntity = matchingValueDetails.getIndicatedEntity();
		//noinspection unchecked
		return indicatedEntity.getRepresentationStrategy().getMode() == RepresentationMode.POJO
			&& indicatedEntity.getEntityName().equals( indicatedEntity.getJavaType().getJavaTypeClass().getName() )
				? (O) indicatedEntity.getJavaType().getJavaTypeClass()
				: (O) indicatedEntity.getEntityName();
	}

	@Nullable
	@Override
	public R toRelationalValue(@Nullable O domainForm) {
		final String entityName = getEntityName( domainForm );
		if ( entityName == null ) {
			return null;
		}
		else {
			final var value = getDetailsForEntityName( entityName ).getValue();
			//noinspection unchecked
			return (R) value;
		}
	}

	@Nullable
	protected abstract String getEntityName(@Nullable O domainForm);

	@Nullable
	public abstract DiscriminatorValueDetails getDetailsForDiscriminatorValue(@Nullable Object relationalValue);

	@Nonnull
	public abstract DiscriminatorValueDetails getDetailsForEntityName(@Nonnull String entityName);

	@Nonnull
	@Override
	public String toString() {
		return "DiscriminatorConverter(" + discriminatorName + ")";
	}

	public abstract void forEachValueDetail(@Nonnull Consumer<DiscriminatorValueDetails> consumer);

	/**
	 * Find and return the first DiscriminatorValueDetails which matches the given {@code handler}
	 */
	@Nullable
	public abstract <X> X fromValueDetails(@Nonnull Function<DiscriminatorValueDetails,X> handler);
}
