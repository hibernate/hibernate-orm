/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.cte;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.PooledLoThreadLocalOptimizer;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * Tests sequence ID conflicts between entity persists and CTE batch inserts with {@link PooledLoThreadLocalOptimizer},
 * ensuring proper ID allocation and prevention of duplicates across both operations.
 *
 * @author Kowsar Atazadeh
 */
@JiraKey("HHH-18818")
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.PREFERRED_POOLED_OPTIMIZER, value = "pooled-lotl"))
@DomainModel(annotatedClasses = Dummy.class)
public class CteInsertWithPooledLoThreadLocalOptimizerTest {
	@Test
	void test(SessionFactoryScope scope) {
		new CteInsertWithPooledLoOptimizerTest().test( scope );
	}
}
