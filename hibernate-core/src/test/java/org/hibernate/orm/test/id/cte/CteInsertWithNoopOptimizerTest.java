/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.cte;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests sequence ID conflicts between entity persists and CTE batch inserts with {@link NoopOptimizer},
 * ensuring proper ID allocation and prevention of duplicates across both operations.
 *
 * @author Kowsar Atazadeh
 */
@JiraKey("HHH-18818")
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.PREFERRED_POOLED_OPTIMIZER, value = "none"))
@DomainModel(annotatedClasses = Dummy.class)
public class CteInsertWithNoopOptimizerTest {
	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( var id = 1; id <= 3; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}
		} );

		scope.inTransaction( session -> {
			session.createMutationQuery( "INSERT INTO Dummy (name) SELECT d.name FROM Dummy d" ).
					executeUpdate();
			var inserted = session.createSelectionQuery(
							"SELECT d.id FROM Dummy d WHERE d.id > 3 ORDER BY d.id", Long.class )
					.getResultList();
			assertEquals( 3, inserted.size() );
			for ( int i = 0; i < inserted.size(); i++ ) {
				assertEquals( 4 + i, inserted.get( i ) );
			}
		} );

		scope.inTransaction( session -> {
			for ( var i = 7; i <= 9; i++ ) {
				Dummy d = new Dummy( "d" + i );
				session.persist( d );
				assertEquals( i, d.getId() );
			}
		} );
	}
}
