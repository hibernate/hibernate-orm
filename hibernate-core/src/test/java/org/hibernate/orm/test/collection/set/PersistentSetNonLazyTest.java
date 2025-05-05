/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.set;


/**
 * @author Gail Badner
 */

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/collection/set/MappingsNonLazy.xml",
		concurrencyStrategy = "nonstrict-read-write"
)
@SessionFactory(generateStatistics = true)
public class PersistentSetNonLazyTest extends PersistentSetTest {

	@Test
	@JiraKey("HHH-3799")
	@FailureExpected(reason = "known to fail with non-lazy collection using query cache")
	public void testLoadChildCheckParentContainsChildCache(SessionFactoryScope scope) {
		super.testLoadChildCheckParentContainsChildCache( scope );
	}
}
