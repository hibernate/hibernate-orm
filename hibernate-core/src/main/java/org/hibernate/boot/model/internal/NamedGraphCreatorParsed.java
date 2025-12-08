/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.GraphParserMode;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.boot.model.NamedGraphCreator;
import org.hibernate.cfg.GraphParserSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.grammars.graph.ModernGraphLanguageLexer;
import org.hibernate.grammars.graph.ModernGraphLanguageParser;
import org.hibernate.grammars.graph.legacy.GraphLanguageLexer;
import org.hibernate.grammars.graph.legacy.GraphLanguageParser;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.internal.parse.EntityNameResolver;
import org.hibernate.graph.internal.parse.GraphParsing;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.service.ServiceRegistry;

import java.util.function.Function;

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
	public <T> RootGraphImplementor<T> createEntityGraph(
			Function<Class<T>, EntityDomainType<?>> entityDomainClassResolver,
			Function<String, EntityDomainType<?>> entityDomainNameResolver,
			ServiceRegistry serviceRegistry) {

		final ConfigurationService configurationService = serviceRegistry.getService( ConfigurationService.class );

		final GraphParserMode graphParserMode = configurationService.getSetting(
				GraphParserSettings.GRAPH_PARSER_MODE,
				GraphParserMode::interpret
		);

		final EntityNameResolver entityNameResolver = new EntityNameResolver() {
			@Override
			public <T> EntityDomainType<T> resolveEntityName(String entityName) {
				//noinspection unchecked
				final EntityDomainType<T> entityDomainType = (EntityDomainType<T>) entityDomainNameResolver.apply(
						entityName );
				if ( entityDomainType != null ) {
					return entityDomainType;
				}
				throw new UnknownEntityTypeException( entityName );
			}
		};

		if ( graphParserMode.equals( GraphParserMode.LEGACY ) ) {
			return parseGraphForLegacyParsingMode( entityDomainClassResolver, entityDomainNameResolver, entityNameResolver );
		}

		return parseGraphForModernParsingMode( entityDomainClassResolver, entityNameResolver );
	}

	private <T> RootGraphImplementor<T> parseGraphForLegacyParsingMode(
			Function<Class<T>, EntityDomainType<?>> entityDomainClassResolver,
			Function<String, EntityDomainType<?>> entityDomainNameResolver,
			EntityNameResolver entityNameResolver) {
		final GraphLanguageLexer lexer = new GraphLanguageLexer( CharStreams.fromString( annotation.graph() ) );
		final GraphLanguageParser parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		final GraphLanguageParser.GraphContext graphContext = parser.graph();

		final EntityDomainType<T> entityDomainType = resolveEntityDomainTypeForLegacyParsingMode(
				graphContext,
				entityDomainClassResolver,
				entityDomainNameResolver
		);

		final String graphName = this.name == null ? entityDomainType.getName() : this.name;

		return GraphParsing.parse( graphName, entityDomainType, graphContext.attributeList(), entityNameResolver );
	}

	private <T> RootGraphImplementor<T> parseGraphForModernParsingMode(
			Function<Class<T>, EntityDomainType<?>> entityDomainClassResolver,
			EntityNameResolver entityNameResolver) {
		final EntityDomainType<T> entityDomainType = resolveEntityDomainTypeFromAnnotation(
				entityDomainClassResolver
		);

		final ModernGraphLanguageLexer lexer = new ModernGraphLanguageLexer(
				CharStreams.fromString( annotation.graph() ) );
		final ModernGraphLanguageParser parser = new ModernGraphLanguageParser( new CommonTokenStream( lexer ) );
		final ModernGraphLanguageParser.GraphContext graphContext = parser.graph();

		final String graphName = this.name == null ? entityDomainType.getName() : this.name;

		return GraphParsing.parse( graphName, entityDomainType, graphContext.graphElementList(), entityNameResolver );
	}

	private <T> EntityDomainType<T> resolveEntityDomainTypeForLegacyParsingMode(
			GraphLanguageParser.GraphContext graphContext,
			Function<Class<T>, EntityDomainType<?>> entityDomainClassResolver,
			Function<String, EntityDomainType<?>> entityDomainNameResolver) {

		final var typeIndicator = graphContext.typeIndicator();
		if ( typeIndicator != null ) {
			if ( entityType != null ) {
				throw new InvalidGraphException(
						"Expecting graph text to not include an entity name : " + annotation.graph() );
			}

			DeprecationLogger.DEPRECATION_LOGGER.deprecatedNamedEntityGraphTextThatContainTypeIndicator();

			//noinspection unchecked
			return (EntityDomainType<T>) entityDomainNameResolver.apply(
					typeIndicator.TYPE_NAME().toString() );
		}

		return resolveEntityDomainTypeFromAnnotation( entityDomainClassResolver );
	}

	private <T> EntityDomainType<T> resolveEntityDomainTypeFromAnnotation(Function<Class<T>, EntityDomainType<?>> entityDomainClassResolver) {
		final Class<?> annotationRootAttribute = annotation.root();
		final boolean isAnnotationRootAttributeVoid = void.class.equals( annotationRootAttribute );

		if ( entityType == null ) {
			if ( isAnnotationRootAttributeVoid ) {
				throw new InvalidNamedEntityGraphParameterException(
						"The 'root' parameter of the @NamedEntityGraph should be passed. Graph : " + annotation.name()
				);
			}

			//noinspection unchecked
			return (EntityDomainType<T>) entityDomainClassResolver.apply( (Class<T>) annotationRootAttribute );
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
			return (EntityDomainType<T>) entityDomainClassResolver.apply( (Class<T>) annotationRootAttribute );
		}

		//noinspection unchecked
		return (EntityDomainType<T>) entityDomainClassResolver.apply( (Class<T>) entityType );
	}

}
