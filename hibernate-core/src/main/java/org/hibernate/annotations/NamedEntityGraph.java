/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Same as {@linkplain jakarta.persistence.NamedEntityGraph}, but leveraging
 * Hibernate's ability to specify the graph as text.
 *
 * @see org.hibernate.graph.GraphParser
 *
 * @since 7.0
 * @author Steve Ebersole
 */
@Target({TYPE, PACKAGE, ANNOTATION_TYPE})
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
	 * The entity-name of the root of the entity {@linkplain #graph graph}.
	 * <p/>
	 * When applied to a root entity class, this is optional and
	 * defaults to the entity-name of that entity.
	 *
	 * @apiNote Entity-name is used here rather than entity-class to allow
	 * for dynamic models.
	 */
	String rootEntityName() default "";

	/**
	 * The textual representation of the graph, relative to the named
	 * {@linkplain #rootEntityName() root entity}.
	 * See {@linkplain org.hibernate.graph.GraphParser} for details.
	 */
	String graph();
}
