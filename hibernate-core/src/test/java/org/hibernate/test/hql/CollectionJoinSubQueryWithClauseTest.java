/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * Implementation of WithClauseTest.
 *
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.COLLECTION_JOIN_SUBQUERY, value = "false" )
)
@DomainModel(
		xmlMappings = {
				"org/hibernate/test/hql/Animal.hbm.xml",
				"org/hibernate/test/hql/SimpleEntityWithAssociation.hbm.xml",
		}
)
@SessionFactory
public class CollectionJoinSubQueryWithClauseTest {
	private final WithClauseTest.TestData data = new WithClauseTest.TestData();

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		data.prepare( scope );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		data.cleanup( scope );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11157")
	public void testWithClauseAsNonSubqueryWithKey(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// Since family has a join table, we will first left join all family members and then do the WITH clause on the target entity table join
					// Normally this produces 2 results which is wrong and can only be circumvented by converting the join table and target entity table join to a subquery
					final String qry = "from Human h left join h.family as f with key(f) like 'son1' where h.description = 'father'";
					List list = session.createQuery( qry ).list();
					assertEquals( "sub-query rewriting of join table was not disabled", 2, list.size() );
				}
		);
	}
}
