/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType.ValueTransformer;

import static org.hibernate.annotations.UuidGenerator.Style.AUTO;
import static org.hibernate.annotations.UuidGenerator.Style.TIME;
import static org.hibernate.annotations.UuidGenerator.Style.VERSION_6;
import static org.hibernate.annotations.UuidGenerator.Style.VERSION_7;
import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;
import static org.hibernate.internal.util.ReflectHelper.getPropertyType;

/**
 * {@linkplain org.hibernate.generator.Generator} for producing {@link UUID} values.
 * <p/>
 * Uses a {@linkplain UuidValueGenerator} and {@linkplain ValueTransformer} to
 * generate the values.
 *
 * @see org.hibernate.annotations.UuidGenerator
 */
public class UuidGenerator implements BeforeExecutionGenerator {
	private final UuidValueGenerator generator;
	private final ValueTransformer valueTransformer;

	/**
	 * This form is used when there is no {@code @UuidGenerator} but we know we want this generator
	 */
	@Internal
	public UuidGenerator(Class<?> memberType) {
		generator = StandardRandomStrategy.INSTANCE;
		valueTransformer = determineProperTransformer( memberType );
	}

	/**
	 * This form is used when there is no {@code @UuidGenerator} but we know we want this generator
	 */
	@Internal
	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			MemberDetails memberDetails) {
		generator = determineValueGenerator( config, memberDetails.getDeclaringType().getName(), memberDetails.getName() );
		valueTransformer = determineProperTransformer( memberDetails.getType().determineRawClass().toJavaClass() );
	}

	@Internal
	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			Member idMember) {
		generator = determineValueGenerator( config, idMember.getDeclaringClass().getName(), idMember.getName() );
		valueTransformer = determineProperTransformer( getPropertyType( idMember ) );
	}

	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			Member member,
			GeneratorCreationContext creationContext) {
		this( config, member );
	}

	/**
	 * @return {@link EventTypeSets#INSERT_ONLY}
	 */
	@Override
	public EnumSet<EventType> getEventTypes() {
		return INSERT_ONLY;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
		return valueTransformer.transform( generator.generateUuid( session ) );
	}

	@Internal
	public UuidValueGenerator getValueGenerator() {
		return generator;
	}

	@Internal
	public ValueTransformer getValueTransformer() {
		return valueTransformer;
	}

	private static UuidValueGenerator determineValueGenerator(
			org.hibernate.annotations.UuidGenerator config,
			String memberDeclaringClassName,
			String memberName) {
		if ( config == null ) {
			return StandardRandomStrategy.INSTANCE;
		}
		else {
			// there is an annotation
			final var style = config.style();
			if ( config.algorithm() != UuidValueGenerator.class ) {
				// the annotation specified a custom algorithm
				if ( style != AUTO ) {
					throw new MappingException(
							String.format(
									Locale.ROOT,
									"Style [%s] should not be specified with custom UUID value generator: %s.%s",
									style.name(),
									memberDeclaringClassName,
									memberName
							)
					);
				}
				return instantiateCustomGenerator( config.algorithm() );
			}
			return switch ( style ) {
				case TIME -> new CustomVersionOneStrategy();
				case VERSION_6 -> UuidVersion6Strategy.INSTANCE;
				case VERSION_7 -> UuidVersion7Strategy.INSTANCE;
				default -> StandardRandomStrategy.INSTANCE;
			};
		}
	}

	private static UuidValueGenerator instantiateCustomGenerator(Class<? extends UuidValueGenerator> algorithmClass) {
		try {
			return algorithmClass.getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to instantiate " + algorithmClass.getName(), e );
		}
	}

	private ValueTransformer determineProperTransformer(Class<?> propertyType) {
		if ( UUID.class.isAssignableFrom( propertyType ) ) {
			return UUIDJavaType.PassThroughTransformer.INSTANCE;
		}
		else if ( String.class.isAssignableFrom( propertyType ) ) {
			return UUIDJavaType.ToStringTransformer.INSTANCE;
		}
		else if ( byte[].class.isAssignableFrom( propertyType ) ) {
			return UUIDJavaType.ToBytesTransformer.INSTANCE;
		}
		else {
			throw new HibernateException( "Unanticipated return type [" + propertyType.getName() + "] for UUID conversion" );
		}
	}
}
