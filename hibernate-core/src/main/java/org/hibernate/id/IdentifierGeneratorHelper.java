/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.Internal;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.TransientObjectException;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.ImplicitDatabaseObjectNamingStrategy;
import org.hibernate.id.enhanced.StandardNamingStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.EntityType;

import static org.hibernate.cfg.MappingSettings.ID_DB_STRUCTURE_NAMING_STRATEGY;
import static org.hibernate.engine.internal.ForeignKeys.getEntityIdentifierIfNotUnsaved;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.log.IncubationLogger.INCUBATION_LOGGER;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;
import static org.hibernate.spi.NavigablePath.IDENTIFIER_MAPPER_PROPERTY;

/**
 * Factory and helper methods for {@link IdentifierGenerator} framework.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@Internal
public final class IdentifierGeneratorHelper {

	/**
	 * Marker object returned from {@link IdentifierGenerator#generate} to indicate that we should
	 * short-circuit any continued generated id checking. Currently, this is only used in the case of the
	 * {@linkplain ForeignGenerator foreign} generator as a way to signal that we should use the associated
	 * entity's id value.
	 *
	 * @deprecated This is not an elegant way to do anything
	 */
	@Deprecated(forRemoval = true, since = "6.2")
	public static final Serializable SHORT_CIRCUIT_INDICATOR = new Serializable() {
		@Override
		public String toString() {
			return "SHORT_CIRCUIT_INDICATOR";
		}
	};

	public static Number makeIntegralValue(long value, Class<?> integralType) {
		if ( integralType == Long.class
				|| integralType == Long.TYPE ) {
			return value;
		}
		else if ( integralType == Integer.class
				|| integralType == Integer.TYPE ) {
			return (int) value;
		}
		else if ( integralType == Short.class
				|| integralType == Short.TYPE ) {
			return (short) value;
		}
		else if ( integralType == BigInteger.class ) {
			return BigInteger.valueOf( value );
		}
		else if ( integralType == BigDecimal.class ) {
			return BigDecimal.valueOf( value );
		}
		else {
			throw new IdentifierGenerationException(
					"Unknown integral data type for ids : " + integralType.getName()
			);
		}
	}

	public static long extractLong(ResultSet resultSet, long defaultValue) throws SQLException {
		final long value = resultSet.getLong( 1 );
		return resultSet.wasNull() ? defaultValue : value;
	}

	public static void bindLong(PreparedStatement preparedStatement, int position, long value) throws SQLException {
		CORE_LOGGER.tracef( "binding parameter [%s] - [%s]", position, value );
		preparedStatement.setLong( position, value );
	}

	public static Object getForeignId(
			String entityName, String propertyName, SharedSessionContractImplementor sessionImplementor, Object object) {
		if ( sessionImplementor.isManaged( object ) ) {
			//abort the save (the object is already saved by a circular cascade)
			return SHORT_CIRCUIT_INDICATOR;
			//throw new IdentifierGenerationException("save associated object first, or disable cascade for inverse association");
		}
		else {
			final var persister =
					sessionImplementor.getFactory().getMappingMetamodel()
							.getEntityDescriptor( entityName );
			return identifier( sessionImplementor, entityType( propertyName, persister ),
					associatedEntity( entityName, propertyName, object, persister ) );
		}
	}

	private static Object associatedEntity(
			String entityName, String propertyName, Object object, EntityPersister entityDescriptor) {
		final Object associatedObject = entityDescriptor.getPropertyValue( object, propertyName );
		if ( associatedObject == null ) {
			throw new IdentifierGenerationException( "Could not assign id from null association '" + propertyName
					+ "' of entity '" + entityName + "'" );
		}
		return associatedObject;
	}

	private static Object identifier(
			SharedSessionContractImplementor session,
			EntityType foreignValueSourceType,
			Object associatedEntity) {
		final String associatedEntityName = foreignValueSourceType.getAssociatedEntityName();
		try {
			return getEntityIdentifierIfNotUnsaved( associatedEntityName, associatedEntity, session );
		}
		catch (TransientObjectException toe) {
			if ( session instanceof Session statefulSession ) {
				statefulSession.persist( associatedEntityName, associatedEntity );
				return session.getContextEntityIdentifier( associatedEntity );
			}
			else if ( session instanceof StatelessSession statelessSession ) {
				return statelessSession.insert( associatedEntityName, associatedEntity );
			}
			else {
				throw new IdentifierGenerationException("sessionImplementor is neither Session nor StatelessSession");
			}
		}
	}

	private static EntityType entityType(String propertyName, EntityPersister entityDescriptor) {
		if ( entityDescriptor.getPropertyType( propertyName ) instanceof EntityType entityType ) {
			// the normal case
			return entityType;
		}
		else {
			// try identifier mapper
			final String mapperPropertyName = IDENTIFIER_MAPPER_PROPERTY + "." + propertyName;
			return (EntityType) entityDescriptor.getPropertyType( mapperPropertyName );
		}
	}

	public static ImplicitDatabaseObjectNamingStrategy getNamingStrategy(Properties params, ServiceRegistry serviceRegistry) {
		final String namingStrategySetting = coalesceSuppliedValues(
				() -> {
					final String localSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, params );
					if ( localSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return localSetting;
				},
				() -> {
					final var settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();
					final String globalSetting = getString( ID_DB_STRUCTURE_NAMING_STRATEGY, settings );
					if ( globalSetting != null ) {
						INCUBATION_LOGGER.incubatingSetting( ID_DB_STRUCTURE_NAMING_STRATEGY );
					}
					return globalSetting;
				},
				StandardNamingStrategy.class::getName
		);
		return serviceRegistry.requireService( StrategySelector.class )
				.resolveStrategy( ImplicitDatabaseObjectNamingStrategy.class, namingStrategySetting );
	}

	/**
	 * Disallow instantiation of IdentifierGeneratorHelper.
	 */
	private IdentifierGeneratorHelper() {
	}
}
