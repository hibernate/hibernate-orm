/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.cte;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.PooledLoOptimizer;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests sequence ID conflicts between entity persists and CTE batch inserts with {@link PooledLoOptimizer},
 * ensuring proper ID allocation and prevention of duplicates across both operations.
 *
 * @author Kowsar Atazadeh
 */
@JiraKey("HHH-18818")
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.PREFERRED_POOLED_OPTIMIZER, value = "pooled-lo"))
@DomainModel(annotatedClasses = Dummy.class)
public class CteInsertWithPooledLoOptimizerTest {
	@Test
	void test(SessionFactoryScope scope) {
		// 9 rows inserted with IDs 1-9
		// Database sequence calls:
		// - First returns 1 (allocates IDs 1-5)
		// - Second returns 6 (allocates IDs 6-10)
		// ID 10 reserved from current allocation
		scope.inTransaction( session -> {
			for ( var id = 1; id <= 9; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}
		} );

		// 9 rows inserted with IDs 11-19
		// Database sequence calls:
		// - First returns 11 (allocates IDs 11-15)
		// - Second returns 16 (allocates IDs 16-20)
		// ID 20 reserved from current allocation
		scope.inTransaction( session -> {
			session.createMutationQuery( "INSERT INTO Dummy (name) SELECT d.name FROM Dummy d" ).
					executeUpdate();
			var inserted = session.createSelectionQuery(
							"SELECT d.id FROM Dummy d WHERE d.id > 9 ORDER BY d.id", Long.class )
					.getResultList();
			assertEquals( 9, inserted.size() );
			for ( int i = 0; i < inserted.size(); i++ ) {
				assertEquals( 11 + i, inserted.get( i ) );
			}
		} );

		// 1 row inserted with ID 10
		// Using previously reserved ID
		scope.inTransaction( session -> {
			Dummy d = new Dummy( "d10" );
			session.persist( d );
			assertEquals( 10, d.getId() );
		} );

		// 1 row inserted with ID 21
		// Database sequence call returns 21 (allocates IDs 21-25)
		// IDs 22-25 reserved from current allocation
		scope.inTransaction( session -> {
			Dummy d = new Dummy( "d21" );
			session.persist( d );
			assertEquals( 21, d.getId() );
		} );
	}
}
