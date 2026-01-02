/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.GraphParserMode;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.boot.model.NamedGraphCreator;
import org.hibernate.cfg.GraphParserSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.spi.GraphParserEntityClassResolver;
import org.hibernate.graph.spi.GraphParserEntityNameResolver;
import org.hibernate.graph.internal.parse.GraphParsing;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.service.ServiceRegistry;

import static org.hibernate.graph.internal.parse.GraphParsing.parseLegacyGraphText;
import static org.hibernate.graph.internal.parse.GraphParsing.parseText;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;

/**
 * @author Steve Ebersole
 */
class NamedGraphCreatorParsed implements NamedGraphCreator {
	private final @Nullable String name;
	private final @Nullable Class<?> entityType;
	private final NamedEntityGraph annotation;

	NamedGraphCreatorParsed(NamedEntityGraph annotation) {
		this( null, annotation );
	}

	NamedGraphCreatorParsed(@Nullable Class<?> entityType, NamedEntityGraph annotation) {
		this.name = nullIfEmpty( annotation.name() );
		this.entityType = entityType;
		this.annotation = annotation;
	}

	@Override
	public RootGraphImplementor<?> createEntityGraph(
			GraphParserEntityClassResolver entityDomainClassResolver,
			GraphParserEntityNameResolver entityDomainNameResolver,
			ServiceRegistry serviceRegistry) {

		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );

		final GraphParserMode graphParserMode = configurationService == null ? null : configurationService.getSetting(
				GraphParserSettings.GRAPH_PARSER_MODE,
				GraphParserMode::interpret,
				GraphParserMode.LEGACY
		);

		if ( graphParserMode != null && graphParserMode.equals( GraphParserMode.MODERN ) ) {
			return parseGraph( entityDomainClassResolver, entityDomainNameResolver );
		}

		return parseGraphForLegacyParsingMode( entityDomainClassResolver, entityDomainNameResolver );
	}

	private static @NonNull EntityDomainType<?> resolve(
			String entityName, GraphParserEntityNameResolver entityDomainNameResolver) {
		final var entityDomainType =
				(EntityDomainType<?>)
						entityDomainNameResolver.resolveEntityName( entityName );
		if ( entityDomainType == null ) {
			throw new UnknownEntityTypeException( entityName );
		}
		else {
			return entityDomainType;
		}
	}

	private <T> RootGraphImplementor<T> parseGraph(
			GraphParserEntityClassResolver entityDomainClassResolver,
			GraphParserEntityNameResolver entityDomainNameResolver) {
		final EntityDomainType<T> entityDomainType = resolveEntityDomainTypeFromAnnotation(
				entityDomainClassResolver
		);

		final var graphContext = parseText( annotation.graph() );

		final String graphName = this.name == null ? entityDomainType.getName() : this.name;

		return GraphParsing.visit( graphName, entityDomainType, graphContext.graphElementList(),
				entityName -> resolve( entityName, entityDomainNameResolver ) );
	}


	private <T> RootGraphImplementor<T> parseGraphForLegacyParsingMode(
			GraphParserEntityClassResolver entityDomainClassResolver,
			GraphParserEntityNameResolver entityDomainNameResolver) {

		final var graphContext = parseLegacyGraphText( annotation.graph() );


		EntityDomainType<T> entityDomainType;

		final var typeIndicator = graphContext.typeIndicator();

		if ( typeIndicator != null ) {
			if ( entityType != null ) {
				throw new InvalidGraphException(
						"Expecting graph text to not include an entity name : " + annotation.graph() );
			}

			DeprecationLogger.DEPRECATION_LOGGER.deprecatedNamedEntityGraphTextThatContainTypeIndicator();

			entityDomainType = (EntityDomainType<T>) resolve( typeIndicator.TYPE_NAME().toString(), entityDomainNameResolver );
		}
		else {
			entityDomainType = resolveEntityDomainTypeFromAnnotation( entityDomainClassResolver );
		}


		final String graphName = this.name == null ? entityDomainType.getName() : this.name;

		return GraphParsing.visit( graphName, entityDomainType, graphContext.attributeList(),
				entityName -> resolve( entityName, entityDomainNameResolver ) );
	}


	private <T> EntityDomainType<T> resolveEntityDomainTypeFromAnnotation(GraphParserEntityClassResolver entityDomainClassResolver) {
		final Class<?> annotationRootAttribute = annotation.root();
		final boolean isAnnotationRootAttributeVoid = void.class.equals( annotationRootAttribute );

		if ( entityType == null ) {
			if ( isAnnotationRootAttributeVoid ) {
				throw new InvalidNamedEntityGraphParameterException(
						"The 'root' parameter of the @NamedEntityGraph should be passed. Graph : " + annotation.name()
				);
			}

			//noinspection unchecked
			return (EntityDomainType<T>) entityDomainClassResolver.resolveEntityClass( annotationRootAttribute );
		}

		if ( !isAnnotationRootAttributeVoid ) {
			if ( !annotationRootAttribute.equals( entityType ) ) {
				throw new InvalidNamedEntityGraphParameterException(
						"The 'root' parameter of the @NamedEntityGraph annotation must reference the entity '"
						+ entityType.getName()
						+ "', but '" + annotationRootAttribute.getName() + "' was provided."
						+ " Graph :" + annotation.name()
				);
			}

			//noinspection unchecked
			return (EntityDomainType<T>) entityDomainClassResolver.resolveEntityClass( annotationRootAttribute );
		}

		//noinspection unchecked
		return (EntityDomainType<T>) entityDomainClassResolver.resolveEntityClass( entityType );
	}

}
