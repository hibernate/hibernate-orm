/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.mapping.internal.EmbeddableDiscriminatorValueDetailsImpl;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Handles conversion of discriminator values for embeddable subtype classes
 * to their domain typed form.
 *
 * @author Marco Belladelli
 * @see EmbeddableDiscriminatorMapping
 */
public class EmbeddableDiscriminatorConverter<O, R> extends DiscriminatorConverter<O, R> {
	public static <O, R> EmbeddableDiscriminatorConverter<O, R> fromValueMappings(
			String discriminatedType,
			JavaType<O> domainJavaType,
			BasicType<R> underlyingJdbcMapping,
			Map<Object, String> valueMappings,
			ServiceRegistry serviceRegistry) {
		final List<EmbeddableDiscriminatorValueDetailsImpl> valueDetailsList = new ArrayList<>( valueMappings.size() );
		final var classLoaderService = serviceRegistry.requireService( ClassLoaderService.class );
		valueMappings.forEach( (value, embeddableClassName) ->
				valueDetailsList.add( new EmbeddableDiscriminatorValueDetailsImpl( value,
						classLoaderService.classForName( embeddableClassName ) ) ) );
		return new EmbeddableDiscriminatorConverter<>(
				discriminatedType,
				domainJavaType,
				underlyingJdbcMapping.getJavaTypeDescriptor(),
				valueDetailsList
		);
	}

	private final Map<Object, EmbeddableDiscriminatorValueDetailsImpl> discriminatorValueToDetailsMap;
	private final Map<String, EmbeddableDiscriminatorValueDetailsImpl> embeddableClassNameToDetailsMap;

	public EmbeddableDiscriminatorConverter(
			String discriminatorName,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			List<EmbeddableDiscriminatorValueDetailsImpl> valueMappings) {
		super( discriminatorName, domainJavaType, relationalJavaType );
		discriminatorValueToDetailsMap = new HashMap<>( valueMappings.size() );
		embeddableClassNameToDetailsMap = new HashMap<>( valueMappings.size() );
		valueMappings.forEach( valueDetails -> {
			discriminatorValueToDetailsMap.put( valueDetails.getValue(), valueDetails );
			embeddableClassNameToDetailsMap.put( valueDetails.getIndicatedEntityName(), valueDetails );
		} );
	}

	@Override
	public O toDomainValue(R relationalForm) {
		assert relationalForm == null || getRelationalJavaType().isInstance( relationalForm );
		final var matchingValueDetails = getDetailsForDiscriminatorValue( relationalForm );
		if ( matchingValueDetails == null ) {
			throw new IllegalStateException( "Could not resolve discriminator value" );
		}
		//noinspection unchecked
		return (O) matchingValueDetails.getEmbeddableClass();
	}

	@Override
	public EmbeddableDiscriminatorValueDetailsImpl getDetailsForDiscriminatorValue(Object relationalValue) {
		final var valueMatch = discriminatorValueToDetailsMap.get( relationalValue );
		if ( valueMatch != null ) {
			return valueMatch;
		}
		throw new HibernateException( "Unrecognized discriminator value: " + relationalValue );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String embeddableClassName) {
		final var valueDetails = embeddableClassNameToDetailsMap.get( embeddableClassName );
		if ( valueDetails != null ) {
			return valueDetails;
		}
		throw new AssertionFailure( "Unrecognized embeddable class: " + embeddableClassName );
	}

	@Override
	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
		discriminatorValueToDetailsMap.forEach( (value, detail) -> consumer.accept( detail ) );
	}

	@Override
	public <X> X fromValueDetails(Function<DiscriminatorValueDetails, X> handler) {
		for ( var detail : discriminatorValueToDetailsMap.values() ) {
			final X result = handler.apply( detail );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	@Override
	protected String getEntityName(O domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		else if ( domainForm instanceof Class<?> clazz ) {
			return clazz.getName();
		}
		else if ( domainForm instanceof String name ) {
			return name;
		}
		else {
			throw new IllegalArgumentException( "Illegal discriminator value: " + domainForm );
		}
	}
}
