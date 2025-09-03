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
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.UUIDJavaType;

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

	private static final CoreMessageLogger log = CoreLogging.messageLogger( UUIDGenerator.class );

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
			final String strategyClassName = parameters.getProperty( UUID_GEN_STRATEGY_CLASS );
			if ( strategyClassName != null ) {
				try {
					final Class<?> strategyClass =
							creationContext.getServiceRegistry().requireService( ClassLoaderService.class )
									.classForName( strategyClassName );
					try {
						strategy = (UUIDGenerationStrategy) strategyClass.newInstance();
					}
					catch ( Exception e ) {
						log.unableToInstantiateUuidGenerationStrategy(e);
					}
				}
				catch ( ClassLoadingException ignore ) {
					log.unableToLocateUuidGenerationStrategy( strategyClassName );
				}
			}
		}

		if ( strategy == null ) {
			// lastly use the standard random generator
			strategy = StandardRandomStrategy.INSTANCE;
		}

		final Type type = creationContext.getType();
		if ( UUID.class.isAssignableFrom( type.getReturnedClass() ) ) {
			valueTransformer = UUIDJavaType.PassThroughTransformer.INSTANCE;
		}
		else if ( String.class.isAssignableFrom( type.getReturnedClass() ) ) {
			// todo (6.0) : allow for org.hibernate.type.descriptor.java.UUIDJavaType.NoDashesStringTransformer
			valueTransformer = UUIDJavaType.ToStringTransformer.INSTANCE;
		}
		else if ( byte[].class.isAssignableFrom( type.getReturnedClass() ) ) {
			valueTransformer = UUIDJavaType.ToBytesTransformer.INSTANCE;
		}
		else {
			throw new HibernateException( "Unanticipated return type [" + type.getReturnedClassName() + "] for UUID conversion" );
		}
	}

	public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		return valueTransformer.transform( strategy.generateUUID( session ) );
	}
}
