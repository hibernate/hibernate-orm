/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.set;


/**
 * @author Gail Badner
 */

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(
		xmlMappings = "org/hibernate/orm/test/collection/set/Mappings.hbm.xml",
		concurrencyStrategy = "nonstrict-read-write"
)
@SessionFactory(generateStatistics = true)
public class PersistentSetNonLazyTest extends PersistentSetTest {

	@Test
//	@FailureExpected(
//			jiraKey = "HHH-3799",
//			reason = "known to fail with non-lazy collection using query cache"
//	)
	public void testLoadChildCheckParentContainsChildCache(SessionFactoryScope scope) {
		super.testLoadChildCheckParentContainsChildCache( scope );
	}
}
