/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
