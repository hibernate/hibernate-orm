/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType.ValueTransformer;

import static org.hibernate.annotations.UuidGenerator.Style.AUTO;
import static org.hibernate.annotations.UuidGenerator.Style.TIME;
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

	private UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			Member idMember) {
		if ( config.algorithm() != UuidValueGenerator.class ) {
			if ( config.style() != AUTO ) {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Style [%s] should not be specified with custom UUID value generator : %s.%s",
								config.style().name(),
								idMember.getDeclaringClass().getName(),
								idMember.getName()
						)
				);
			}
			generator = instantiateCustomGenerator( config.algorithm() );
		}
		else if ( config.style() == TIME ) {
			generator = new CustomVersionOneStrategy();
		}
		else {
			generator = StandardRandomStrategy.INSTANCE;
		}

		final Class<?> propertyType = getPropertyType( idMember );
		this.valueTransformer = determineProperTransformer( propertyType );
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

		if ( String.class.isAssignableFrom( propertyType ) ) {
			return UUIDJavaType.ToStringTransformer.INSTANCE;
		}

		if ( byte[].class.isAssignableFrom( propertyType ) ) {
			return UUIDJavaType.ToBytesTransformer.INSTANCE;
		}

		throw new HibernateException( "Unanticipated return type [" + propertyType.getName() + "] for UUID conversion" );
	}

	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			Member idMember,
			CustomIdGeneratorCreationContext creationContext) {
		this(config, idMember);
	}

	public UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			Member member,
			GeneratorCreationContext creationContext) {
		this(config, member);
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
}
