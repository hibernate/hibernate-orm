/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Defines a {@linkplain org.hibernate.service.Service service} for creating
 * id generators.
 * <p>
 * This is an older mechanism for instantiating and configuring id generators
 * which predates the existence of {@link org.hibernate.generator.Generator}.
 * It is used when id generators are identified by stringly-typed names.
 *
 * @see org.hibernate.id.factory.IdentifierGeneratorFactory
 *
 * @deprecated Use new {@link org.hibernate.generator.Generator} infrastructure
 */
@Deprecated(since = "7.0")
package org.hibernate.id.factory;
