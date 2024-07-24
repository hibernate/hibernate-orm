/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.uuid;

import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType.ValueTransformer;

import static org.hibernate.annotations.UuidGenerator.Style.TIME;
import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;
import static org.hibernate.internal.util.ReflectHelper.getPropertyType;

/**
 * Generates {@link UUID}s.
 *
 * @see org.hibernate.annotations.UuidGenerator
 */
public class UuidGenerator implements BeforeExecutionGenerator {

	private final UuidValueGenerator generator;
	private final ValueTransformer valueTransformer;

	private UuidGenerator(
			org.hibernate.annotations.UuidGenerator config,
			Member idMember) {
		if ( config.style() == TIME ) {
			generator = new CustomVersionOneStrategy();
		}
		else if ( config.valueGenerator() == UuidValueGenerator.class ) {
			generator = StandardRandomStrategy.INSTANCE;
		}
		else {
			try {
				generator = config.valueGenerator().getConstructor(  ).newInstance(  );
			}
			catch (final ReflectiveOperationException e) {
				throw new HibernateException(
						"Could not instantiate UUID value generator of type '" + config.valueGenerator().getName() + "'",
						e
				);
			}
		}

		final Class<?> propertyType = getPropertyType( idMember );

		if ( UUID.class.isAssignableFrom( propertyType ) ) {
			valueTransformer = UUIDJavaType.PassThroughTransformer.INSTANCE;
		}
		else if ( String.class.isAssignableFrom( propertyType ) ) {
			valueTransformer = UUIDJavaType.ToStringTransformer.INSTANCE;
		}
		else if ( byte[].class.isAssignableFrom( propertyType ) ) {
			valueTransformer = UUIDJavaType.ToBytesTransformer.INSTANCE;
		}
		else {
			throw new HibernateException( "Unanticipated return type [" + propertyType.getName() + "] for UUID conversion" );
		}
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
}
