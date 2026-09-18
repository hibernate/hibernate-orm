/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.AccessorSettings;
import org.hibernate.property.access.spi.PropertyAccessorService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hibernate.cfg.AccessorSettings.ACCESSOR_STRATEGY;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;

/**
 * {@link StandardServiceInitiator} for {@link PropertyAccessorService}.
 *
 * <p>Defaults to the ByteBuddy-based implementation. Quarkus and similar
 * frameworks replace via {@link java.util.ServiceLoader} discovery.
 */
public class PropertyAccessorServiceInitiator implements StandardServiceInitiator<PropertyAccessorService> {

	public static final PropertyAccessorServiceInitiator INSTANCE = new PropertyAccessorServiceInitiator();

	@Nonnull
	@Override
	public Class<PropertyAccessorService> getServiceInitiated() {
		return PropertyAccessorService.class;
	}

	@Override
	public PropertyAccessorService initiateService(
			@Nonnull Map<String, Object> configurationValues,
			@Nonnull ServiceRegistryImplementor registry) {
		final AccessorStrategy strategy = AccessorStrategy.interpret(
				getString( ACCESSOR_STRATEGY, configurationValues, AccessorStrategy.GENERATED.getConfigValue() )
		);
		return switch ( strategy ) {
			case GENERATED -> new ByteBuddyPropertyAccessorService( configurationValues );
			case REFLECTION -> new DelegatingPropertyAccessorService( new ReflectionNoMultiAccessorFactory() );
		};
	}

	/**
	 * Enumeration of the available property accessor strategies, as configured via
	 * {@link AccessorSettings#ACCESSOR_STRATEGY}.
	 */
	private enum AccessorStrategy {
		/**
		 * Generates bytecode via ByteBuddy for both individual and multi-value accessors (default).
		 */
		GENERATED( "generated" ),

		/**
		 * Uses plain {@code java.lang.reflect} for all accessors, no multi-value.
		 */
		REFLECTION( "reflection" );

		private final String configValue;

		AccessorStrategy(String configValue) {
			this.configValue = configValue;
		}

		public String getConfigValue() {
			return configValue;
		}

		/**
		 * Interpret the configured value.
		 *
		 * @param strategy configured {@link AccessorStrategy} representation; may be {@code null}
		 * @return the associated {@link AccessorStrategy}, or a {@code default} if none was specified
		 */
		public static AccessorStrategy interpret(Object strategy) {
			if ( strategy == null ) {
				return AccessorStrategy.GENERATED;
			}
			else if ( strategy instanceof AccessorStrategy accessorStrategy ) {
				return accessorStrategy;
			}
			else if ( strategy instanceof String string ) {
				final String normalized = string.trim().toLowerCase( Locale.ROOT );
				for ( AccessorStrategy value : values() ) {
					if ( value.name().toLowerCase( Locale.ROOT ).equals( normalized )
						|| value.configValue.equals( normalized ) ) {
						return value;
					}
				}
			}
			throw new HibernateException(
					"Unrecognized value for " + ACCESSOR_STRATEGY + ": " + strategy
					+ "; valid values are: " + Arrays.stream( values() )
							.map( AccessorStrategy::getConfigValue )
							.collect( Collectors.joining( ", " ) )
			);
		}
	}
}
