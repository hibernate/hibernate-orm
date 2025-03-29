/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.norevision;

import java.util.Arrays;
import java.util.List;

public class CollectionChangeRevisionTest extends AbstractCollectionChangeTest {
	@Override
	protected String getCollectionChangeValue() {
		return "true";
	}

	@Override
	protected List<Integer> getExpectedPersonRevisions() {
		return Arrays.asList( 1, 3 );
	}

	@Override
	protected List<Integer> getExpectedParentRevisions() {
		return Arrays.asList( 4, 5 );
	}

	@Override
	protected List<Integer> getExpectedHouseRevisions() {
		return Arrays.asList( 6, 7 );
	}
}
