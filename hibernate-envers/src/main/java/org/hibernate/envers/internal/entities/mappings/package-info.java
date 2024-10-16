/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Contains empty implementations that are the {@link jakarta.persistence.Entity entity}
 * subtypes, simply inheriting from the default revision entities which are mapped as
 * {@link jakarta.persistence.MappedSuperclass mapped-superclass}es.
 * <p>
 * This is needed to allow for the correct injection of the static-metamodel {@code class_}
 * meta-type field of both the default revision entities and the mapped-superclass from which
 * custom ones might extend from.
 *
 * @author Marco Belladelli
 */
package org.hibernate.envers.internal.entities.mappings;
