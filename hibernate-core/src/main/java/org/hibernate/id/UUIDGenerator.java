/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.util.Properties;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.UUIDJavaType;

import static org.hibernate.id.UUIDLogger.UUID_MESSAGE_LOGGER;

/**
 * An {@link IdentifierGenerator} which generates {@link UUID} values using a pluggable
 * {@linkplain UUIDGenerationStrategy generation strategy}. The values this generator
 * can return include {@link UUID}, {@link String} and {@code byte[16]}.
 * <p>
 * Accepts two configuration parameters:<ul>
 * <li>{@value #UUID_GEN_STRATEGY} - names the {@link UUIDGenerationStrategy} instance to use</li>
 * <li>{@value #UUID_GEN_STRATEGY_CLASS} - names the {@link UUIDGenerationStrategy} class to use</li>
 * </ul>
 * <p>
 * There are two standard implementations of {@link UUIDGenerationStrategy}:<ul>
 * <li>{@link StandardRandomStrategy} (the default, if none specified)</li>
 * <li>{@link org.hibernate.id.uuid.CustomVersionOneStrategy}</li>
 * </ul>
 *
 * @deprecated use {@link org.hibernate.id.uuid.UuidGenerator} and
 * {@link org.hibernate.annotations.UuidGenerator} instead
 */
@Deprecated(since = "6.0")
public class UUIDGenerator implements IdentifierGenerator {

	public static final String UUID_GEN_STRATEGY = "uuid_gen_strategy";
	public static final String UUID_GEN_STRATEGY_CLASS = "uuid_gen_strategy_class";

	private UUIDGenerationStrategy strategy;
	private UUIDJavaType.ValueTransformer valueTransformer;

	@Override
	public void configure(GeneratorCreationContext creationContext, Properties parameters) throws MappingException {
		// check first for an explicit strategy instance
		strategy = (UUIDGenerationStrategy) parameters.get( UUID_GEN_STRATEGY );
		if ( strategy == null ) {
			// next check for an explicit strategy class
			strategy = strategy( creationContext, parameters );
		}
		valueTransformer = valueTransformer( creationContext );
	}

	private UUIDJavaType.ValueTransformer valueTransformer(GeneratorCreationContext creationContext) {
		final Type type = creationContext.getType();
		if ( UUID.class.isAssignableFrom( type.getReturnedClass() ) ) {
			return UUIDJavaType.PassThroughTransformer.INSTANCE;
		}
		else if ( String.class.isAssignableFrom( type.getReturnedClass() ) ) {
			// todo (6.0) : allow for org.hibernate.type.descriptor.java.UUIDJavaType.NoDashesStringTransformer
			return UUIDJavaType.ToStringTransformer.INSTANCE;
		}
		else if ( byte[].class.isAssignableFrom( type.getReturnedClass() ) ) {
			return UUIDJavaType.ToBytesTransformer.INSTANCE;
		}
		else {
			throw new HibernateException( "Unanticipated return type [" + type.getReturnedClassName() + "] for UUID conversion" );
		}
	}

	private UUIDGenerationStrategy strategy(GeneratorCreationContext creationContext, Properties parameters) {
		final String strategyClassName = parameters.getProperty( UUID_GEN_STRATEGY_CLASS );
		if ( strategyClassName != null ) {
			final var classLoaderService =
					creationContext.getServiceRegistry()
							.requireService( ClassLoaderService.class );
			try {
				final var strategyClass = classLoaderService.classForName( strategyClassName );
				try {
					return (UUIDGenerationStrategy) strategyClass.newInstance();
				}
				catch ( Exception exception ) {
					UUID_MESSAGE_LOGGER.unableToInstantiateUuidGenerationStrategy( exception );
				}
			}
			catch ( ClassLoadingException ignore ) {
				UUID_MESSAGE_LOGGER.unableToLocateUuidGenerationStrategy( strategyClassName );
			}
		}
		return StandardRandomStrategy.INSTANCE;
	}

	public Object generate(SharedSessionContractImplementor session, Object object) {
		return valueTransformer.transform( strategy.generateUUID( session ) );
	}
}
