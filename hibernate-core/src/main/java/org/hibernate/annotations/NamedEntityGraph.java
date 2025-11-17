/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.graph.GraphParser;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a named {@linkplain EntityGraph entity graph}
 * based on Hibernate's {@linkplain org.hibernate.graph.GraphParser entity graph language}.
 * <p/>
 * When applied to a root entity class, the root entity name is implied - e.g. {@code "title, isbn, author(name, books)"}
 * <p/>
 * When applied to a package, the root entity name must be specified - e.g. {@code "Book: title, isbn, author(name, books)"}
 *
 * @see EntityManager#getEntityGraph(String)
 * @see org.hibernate.SessionFactory#parseEntityGraph(CharSequence)
 * @see GraphParser#parse(CharSequence, SessionFactory)
 *
 * @see org.hibernate.graph.GraphParser
 * @see jakarta.persistence.NamedEntityGraph
 *
 * @since 7.0
 * @author Steve Ebersole
 */
@Target({TYPE, PACKAGE})
@Retention(RUNTIME)
@Repeatable(NamedEntityGraphs.class)
public @interface NamedEntityGraph {
	/**
	 * The name used to identify the entity graph in calls to
	 * {@linkplain org.hibernate.Session#getEntityGraph(String)}.
	 * Entity graph names must be unique within the persistence unit.
	 * <p/>
	 * When applied to a root entity class, the name is optional and
	 * defaults to the entity-name of that entity.
	 */
	String name() default "";

	/**
	 * The textual representation of the graph.
	 * <p/>
	 * When applied to a package, the syntax requires the entity name - e.g., {@code "Book: title, isbn, author(name, books)"}.
	 * <p/>
	 * When applied to an entity, the entity name should be omitted - e.g., {@code "title, isbn, author(name, books)"}.
	 * <p/>
	 * See {@linkplain org.hibernate.graph.GraphParser} for details about the syntax.
	 */
	String graph();
}
