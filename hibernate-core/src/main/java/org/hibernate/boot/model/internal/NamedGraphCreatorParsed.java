/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.annotations.NamedEntityGraph;
import org.hibernate.boot.model.NamedGraphCreator;
import org.hibernate.grammars.graph.GraphLanguageParser;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.spi.GraphParserEntityClassResolver;
import org.hibernate.graph.spi.GraphParserEntityNameResolver;
import org.hibernate.graph.internal.parse.GraphParsing;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

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
			GraphParserEntityNameResolver entityDomainNameResolver) {
		final var graphContext = parseText( annotation.graph() );
		final var typeIndicator = graphContext.typeIndicator();
		final EntityDomainType<?> entityDomainType;
		final String jpaEntityName;
		if ( entityType == null ) {
			if ( typeIndicator == null ) {
				throw new InvalidGraphException( "Expecting graph text to include an entity name: " + annotation.graph() );
			}
			jpaEntityName = typeIndicator.TYPE_NAME().toString();
			entityDomainType = entityDomainNameResolver.resolveEntityName( jpaEntityName );
		}
		else {
			if ( typeIndicator != null ) {
				throw new InvalidGraphException( "Expecting graph text to not include an entity name: " + annotation.graph() );
			}
			entityDomainType = entityDomainClassResolver.resolveEntityClass( entityType );
			jpaEntityName = entityDomainType.getName();
		}
		return visit( name == null ? jpaEntityName : name,
				entityDomainType, entityDomainNameResolver, graphContext );
	}

	private static @NonNull RootGraphImplementor<?> visit(
			String name,
			EntityDomainType<?> entityDomainType, GraphParserEntityNameResolver entityDomainNameResolver,
			GraphLanguageParser.GraphContext graphContext) {
		return GraphParsing.visit( name, entityDomainType, graphContext.attributeList(),
				entityName -> resolve( entityName, entityDomainNameResolver ) );
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
}
