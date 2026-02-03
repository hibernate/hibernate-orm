/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;

import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.TransactionIdentifierService;

import static org.hibernate.cfg.StateManagementSettings.TRANSACTION_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.USE_SERVER_TRANSACTION_TIMESTAMPS;
import static org.hibernate.internal.util.GenericsHelper.erasedType;
import static org.hibernate.internal.util.GenericsHelper.supertypeInstantiation;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/**
 * Default implementation of {@link TransactionIdentifierService}.
 *
 * @see StateManagementSettings#TRANSACTION_ID_SUPPLIER
 * @see StateManagementSettings#USE_SERVER_TRANSACTION_TIMESTAMPS
 *
 * @author Gavin King
 */
public class TransactionIdentifierServiceImpl implements TransactionIdentifierService, Supplier<Instant> {

	private final Class<?> identifierValueType;
	private final Supplier<?> identifierValueSupplier;
	private final boolean useServerTransactionTimestamps;

	public TransactionIdentifierServiceImpl(ServiceRegistry serviceRegistry) {
		final var settings =
				serviceRegistry.requireService( ConfigurationService.class )
						.getSettings();
		useServerTransactionTimestamps =
				getBoolean( USE_SERVER_TRANSACTION_TIMESTAMPS, settings );
		if ( useServerTransactionTimestamps ) {
			if ( settings.containsKey( TRANSACTION_ID_SUPPLIER ) ) {
				throw new MappingException( "Settings '"
						+ USE_SERVER_TRANSACTION_TIMESTAMPS + "' and '"
						+ TRANSACTION_ID_SUPPLIER + "' are mutually exclusive"
				);
			}
			identifierValueSupplier = null;
			identifierValueType = Instant.class;
		}
		else {
			identifierValueSupplier =
					transactionIdSupplier( settings,
							serviceRegistry.requireService( StrategySelector.class ) );
			@SuppressWarnings( "unchecked" ) // completely safe
			final var supplierClass = (Class<Supplier<?>>) identifierValueSupplier.getClass();
			identifierValueType = resolveSuppliedType( supplierClass );
		}
	}

	@Override
	public Instant get() {
		return Instant.now();
	}

	@Override
	public boolean isIdentifierTypeInstant() {
		return identifierValueType == Instant.class;
	}

	@Override
	public Class<?> getIdentifierType() {
		return identifierValueType;
	}

	@Override
	public Supplier<?> getIdentifierSupplier() {
		return identifierValueSupplier;
	}

	@Override
	public boolean isDisabled() {
		return useServerTransactionTimestamps;
	}

	private static Class<?> resolveSuppliedType(Class<? extends Supplier<?>> supplierClass) {
		final var supplierInstantiation = supertypeInstantiation( Supplier.class, supplierClass );
		if ( supplierInstantiation == null ) {
			return null;
		}
		else {
			final var typeArguments = supplierInstantiation.getActualTypeArguments();
			return typeArguments.length == 0 ? null : erasedType( typeArguments[0] );
		}
	}

	public Supplier<?> transactionIdSupplier(
			Map<String,Object> settings,
			StrategySelector strategySelector) {
		Object setting = settings.get( TRANSACTION_ID_SUPPLIER );
		if ( setting == null ) {
			return this;
		}
		else if ( setting instanceof Supplier<?> supplier ) {
			return supplier;
		}
		else if ( setting instanceof Class<?> clazz ) {
			if ( !Supplier.class.isAssignableFrom( clazz ) ) {
				throw new HibernateException(
						"Setting '" + TRANSACTION_ID_SUPPLIER + "' must specify a '"
								+ Supplier.class.getName() + "' or a class name"
				);
			}
			return strategySelector.resolveStrategy( Supplier.class, clazz );
		}
		else if ( setting instanceof String name ) {
			return strategySelector.resolveStrategy( Supplier.class, name );
		}
		else {
			throw new HibernateException(
					"Setting '" + TRANSACTION_ID_SUPPLIER + "' must specify a '"
							+ Supplier.class.getName() + "' or a class name"
			);
		}
	}
}
