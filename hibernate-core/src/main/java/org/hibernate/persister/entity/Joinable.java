/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity;

/**
 * Anything that can be loaded by outer join - namely
 * persisters for classes or collections.
 *
 * @author Gavin King
 *
 * @deprecated Use {@link org.hibernate.metamodel.mapping.ModelPartContainer},
 * {@link org.hibernate.sql.ast.tree.from.TableGroupProducer} and/or
 * {@link org.hibernate.sql.ast.tree.from.TableGroupJoinProducer} instead
 * depending on need
 */
@Deprecated(since = "6", forRemoval = true)
public interface Joinable {
}
