/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bulkid;

import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

/**
 * @author Vlad Mihalcea
 */
public class DefaultMutationStrategyCompositeIdTest extends AbstractMutationStrategyCompositeIdTest {

	@Override
	protected Class<? extends SqmMultiTableMutationStrategy> getMultiTableBulkIdStrategyClass() {
		return null;
	}
}
