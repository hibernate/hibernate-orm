/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * An object capable of determining the entity name for a given entity instance.
 * <p>
 * A single Java class may be mapped as an entity multiple times, with each mapping
 * distinguished by a distinct <em>entity name</em>. This is not currently possible
 * using annotations, but it may be achieved using XML-based mappings. If a class
 * is mapped multiple times, Hibernate needs a way to determine which entity is
 * represented by a given instance of the class. There are two ways to provide this
 * information:
 * <ul>
 * <li>by passing the entity name explicitly to methods of {@link Session}, for
 *     example, by calling {@link Session#persist(String, Object)}, but this can
 *     be verbose and lacking in typesafety, or
 * <li>by having the entity instance itself carry around the information, for
 *     example, by giving it a field holding the entity name, and supplying an
 *     {@code EntityNameResolver} which is capable of extracting the information
 *     from the entity instance.
 * </ul>
 *
 * @see org.hibernate.boot.SessionFactoryBuilder#addEntityNameResolver(EntityNameResolver...)
 * @see org.hibernate.cfg.Configuration#addEntityNameResolver(EntityNameResolver)
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface EntityNameResolver {
	/**
	 * Given an entity instance, determine its entity name.
	 *
	 * @param entity The entity instance.
	 *
	 * @return The corresponding entity name, or {@code null} if this implementation
	 *         does not know how to perform resolution for the given entity instance.
	 */
	String resolveEntityName(Object entity);
}
