/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

/**
 * Basic implementation of the {@link PostInsertIdentifierGenerator} contract.
 *
 * @deprecated Subclasses should now directly inherit {@link PostInsertIdentifierGenerator} and
 *             {@link BulkInsertionCapableIdentifierGenerator}, or even better, simply implement
 *             {@link org.hibernate.generator.OnExecutionGenerator} directly.
 *
 * @author Gavin King
 */
@Deprecated(forRemoval = true, since = "6.2")
public abstract class AbstractPostInsertGenerator
		implements PostInsertIdentifierGenerator, BulkInsertionCapableIdentifierGenerator {}
