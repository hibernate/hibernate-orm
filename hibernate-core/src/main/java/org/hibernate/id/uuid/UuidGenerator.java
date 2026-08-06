/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.io.Serializable;
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
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.java.UuidCapableJavaType;

import static org.hibernate.annotations.UuidGenerator.Style.AUTO;
import static org.hibernate.annotations.UuidGenerator.Style.TIME;
import static org.hibernate.annotations.UuidGenerator.Style.VERSION_6;
import static org.hibernate.annotations.UuidGenerator.Style.VERSION_7;
import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;
import static org.hibernate.internal.util.ReflectHelper.getPropertyType;

/**
 * {@linkplain org.hibernate.generator.Generator} for producing {@link UUID} values.
 * <p>
 * Uses a {@linkplain UuidValueGenerator} and
 * {@linkplain UuidCapableJavaType.ValueTransformer value transformer} to
 * generate the values.
 *
 * @see org.hibernate.annotations.UuidGenerator
 */
public class UuidGenerator implements BeforeExecutionGenerator {
	private final UuidValueGenerator generator;
	private final UuidCapableJavaType.ValueTransformer<?> valueTransformer;
	private final Class<?> generatedType;

	/**
	 * This form is used when there is no {@code @UuidGenerator} but we know we want this generator
	 */
	@Internal
	public UuidGenerator(Class<?> memberType) {
		generator = StandardRandomStrategy.INSTANCE;
		final var javaType = determineProperJavaType( memberType );
		valueTransformer = javaType.getUuidValueTransformer();
		generatedType = javaType.getJavaTypeClass();
	}

	/**
	 * This form is used when there is no {@code @UuidGenerator} but we know we want this generator
	 */
	@Internal
	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			MemberDetails memberDetails) {
		generator = determineValueGenerator( config, memberDetails.getDeclaringType().getName(), memberDetails.getName() );
		final var javaType =
				determineProperJavaType( memberDetails.getType().determineRawClass().toJavaClass() );
		generatedType = javaType.getJavaTypeClass();
		valueTransformer = javaType.getUuidValueTransformer();
	}

	@Internal
	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			MemberDetails memberDetails,
			GeneratorCreationContext creationContext) {
		generator = determineValueGenerator( config, memberDetails.getDeclaringType().getName(), memberDetails.getName() );
		final var javaType = determineProperJavaType( creationContext );
		generatedType = javaType.getJavaTypeClass();
		valueTransformer = javaType.getUuidValueTransformer();
	}

	@Internal
	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			Member idMember) {
		generator = determineValueGenerator( config, idMember.getDeclaringClass().getName(), idMember.getName() );
		final var javaType = determineProperJavaType( getPropertyType( idMember ) );
		generatedType = javaType.getJavaTypeClass();
		valueTransformer = javaType.getUuidValueTransformer();
	}

	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			Member member,
			GeneratorCreationContext creationContext) {
		generator = determineValueGenerator( config, member.getDeclaringClass().getName(), member.getName() );
		final var javaType = determineProperJavaType( creationContext );
		generatedType = javaType.getJavaTypeClass();
		valueTransformer = javaType.getUuidValueTransformer();
	}

	/**
	 * @return {@link EventTypeSets#INSERT_ONLY}
	 */
	@Override
	public EnumSet<EventType> getEventTypes() {
		return INSERT_ONLY;
	}

	@Override
	public Class<?> getGeneratedType() {
		return generatedType;
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
	public UUIDJavaType.ValueTransformer getValueTransformer() {
		return new UUIDJavaType.ValueTransformer() {
			@Override
			public Serializable transform(UUID uuid) {
				return (Serializable) valueTransformer.transform( uuid );
			}

			@Override
			public UUID parse(Object value) {
				return parseTransformedValue( valueTransformer, value );
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static <T> UUID parseTransformedValue(
			UuidCapableJavaType.ValueTransformer<T> valueTransformer,
			Object value) {
		return valueTransformer.parse( (T) value );
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

	private static UuidCapableJavaType<?> determineProperJavaType(GeneratorCreationContext creationContext) {
		if ( creationContext.getType() instanceof BasicType<?> basicType
				&& basicType.getJavaTypeDescriptor() instanceof UuidCapableJavaType<?> uuidJavaType ) {
			return uuidJavaType;
		}
		return determineProperJavaType( creationContext.getType().getReturnedClass() );
	}

	private static UuidCapableJavaType<?> determineProperJavaType(Class<?> propertyType) {
		if ( UUID.class.isAssignableFrom( propertyType ) ) {
			return UUIDJavaType.INSTANCE;
		}
		else if ( String.class.isAssignableFrom( propertyType ) ) {
			return StringJavaType.INSTANCE;
		}
		else if ( byte[].class.isAssignableFrom( propertyType ) ) {
			return PrimitiveByteArrayJavaType.INSTANCE;
		}
		else {
			throw new HibernateException( "Unanticipated return type [" + propertyType.getName() + "] for UUID conversion" );
		}
	}
}
