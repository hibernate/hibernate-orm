/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.norevision;

import java.util.Arrays;
import java.util.List;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

@EnversTest
@Jpa(annotatedClasses = {Person.class, Name.class, Parent.class, Child.class, House.class},
		integrationSettings = @Setting(name = EnversSettings.REVISION_ON_COLLECTION_CHANGE, value = "true"))
public class CollectionChangeRevisionTest extends AbstractCollectionChangeTest {
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
