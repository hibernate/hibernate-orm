/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.boot.model.NamedGraphCreator;
import org.hibernate.grammars.graph.GraphLanguageLexer;
import org.hibernate.grammars.graph.GraphLanguageParser;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.InvalidNamedEntityGraphParameterException;
import org.hibernate.graph.internal.parse.EntityNameResolver;
import org.hibernate.graph.internal.parse.GraphParsing;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.metamodel.model.domain.EntityDomainType;

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
			Function<String, EntityDomainType<?>> entityDomainNameResolver) {
		final GraphLanguageLexer lexer = new GraphLanguageLexer( CharStreams.fromString( annotation.graph() ) );
		final GraphLanguageParser parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );
		final GraphLanguageParser.GraphContext graphContext = parser.graph();

		final EntityNameResolver entityNameResolver = new EntityNameResolver() {
			@Override
			public <T> EntityDomainType<T> resolveEntityName(String entityName) {
				//noinspection unchecked
				final EntityDomainType<T> entityDomainType = (EntityDomainType<T>) entityDomainNameResolver.apply( entityName );
				if ( entityDomainType != null ) {
					return entityDomainType;
				}
				throw new UnknownEntityTypeException( entityName );
			}
		};

		final EntityDomainType<T> entityDomainType = resolveEntityDomainType(
				graphContext,
				entityDomainClassResolver,
				entityDomainNameResolver
		);

		final String name = this.name == null ? entityDomainType.getName() : this.name;

		return GraphParsing.parse( name, entityDomainType, graphContext.graphElementList(), entityNameResolver );

	}

	private <T> EntityDomainType<T> resolveEntityDomainType(
			GraphLanguageParser.GraphContext graphContext,
			Function<Class<T>, EntityDomainType<?>> entityDomainClassResolver,
			Function<String, EntityDomainType<?>> entityDomainNameResolver) {
		var typeIndicator = graphContext.typeIndicator();
		final Class<?> annotationRootAttribute = annotation.root();

		if ( entityType == null ) {

			if ( typeIndicator == null && void.class.equals( annotationRootAttribute ) ) {
				throw new InvalidNamedEntityGraphParameterException(
						"The 'root' parameter of the @NamedEntityGraph should be passed. Graph : " + annotation.name()
				);
			}

			if ( typeIndicator != null ) {
				DeprecationLogger.DEPRECATION_LOGGER.deprecatedNamedEntityGraphTextThatContainTypeIndicator();

				//noinspection unchecked
				return (EntityDomainType<T>) entityDomainNameResolver.apply(
						typeIndicator.TYPE_NAME().toString() );

			}

			//noinspection unchecked
			return (EntityDomainType<T>) entityDomainClassResolver.apply( (Class<T>) annotationRootAttribute );
		}


		if ( typeIndicator != null ) {
			throw new InvalidGraphException( "Expecting graph text to not include an entity name : " + annotation.graph() );
		}

		if ( !void.class.equals( annotationRootAttribute ) ) {

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
