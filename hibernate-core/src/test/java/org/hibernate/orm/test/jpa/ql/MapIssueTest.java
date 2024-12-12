/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ql;

import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.orm.test.jpa.model.MapContent;
import org.hibernate.orm.test.jpa.model.MapOwner;
import org.hibernate.orm.test.jpa.model.Relationship;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@TestForIssue(jiraKey = "HHH-14279")
@DomainModel(
		annotatedClasses = {
				MapOwner.class, MapContent.class, Relationship.class
		})
@SessionFactory(useCollectingStatementInspector = true)
public class MapIssueTest {

	@Test
	@RequiresDialect(value = PostgreSQLDialect.class, comment = "Requires support for using a correlated column in a join condition which H2 apparently does not support. For simplicity just run this on PostgreSQL")
	public void testWhereSubqueryMapKeyIsEntityWhereWithKey(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.createQuery( "select r from Relationship r where exists (select 1 from MapOwner as o left join o.contents c with key(c) = r)" ).list();
				}
		);
	}

	@Test
	public void testOnlyCollectionTableJoined(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select 1 from MapOwner as o left join o.contents c where c.id is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert only the collection table is joined
					statementInspector.assertNumberOfJoins( 0, 1 );
				}
		);
	}

	@Test
	public void testMapKeyJoinIsNotOmitted(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select c from MapOwner as o join o.contents c join c.relationship r where r.id is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert 3 joins, collection table, collection element and collection key (relationship)
					statementInspector.assertNumberOfJoins( 0, 3 );
				}
		);
	}

	@Test
	public void testMapKeyJoinIsOmitted2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select c from MapOwner as o join o.contents c where c.relationship.id is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert 2 joins, collection table and collection element. No need to join the relationship because it is not nullable
					statementInspector.assertNumberOfJoins( 0, 2 );
				}
		);
	}

	@Test
	public void testMapKeyDeReferenceDoesNotCauseJoin(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select c from MapOwner as o left join o.contents c where key(c).id is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert 2 joins, collection table and collection element
					statementInspector.assertNumberOfJoins( 0, 2 );
				}
		);
	}

	@Test
	public void testMapKeyJoinIsReused(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select key(c), c from MapOwner as o left join o.contents c join c.relationship r where r.name is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert 3 joins, collection table, collection element and relationship
					statementInspector.assertNumberOfJoins( 0, 3 );
				}
		);
	}

	@Test
	public void testMapKeyJoinIsReusedForFurtherJoin(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select key(c), c from MapOwner as o left join o.contents c join c.relationship r join r.self s where s.name is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert 3 joins, collection table, collection element, relationship and self
					statementInspector.assertNumberOfJoins( 0, 4 );
				}
		);
	}

	@Test
	public void testMapKeyJoinIsReusedForFurtherJoinAndElementJoinIsProperlyOrdered(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select key(c), c from MapOwner as o left join o.contents c join c.relationship r join r.self s join c.relationship2 where s.name is not null" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert 3 joins, collection table, collection element, relationship, relationship2 and self
					statementInspector.assertNumberOfJoins( 0, 5 );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-15357")
	public void testSelectMapKeyFk(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				s -> {
					s.createQuery( "select key(c).id from MapOwner as o left join o.contents c" ).list();
					statementInspector.assertExecutedCount( 1 );
					// Assert that only the collection table and element table are joined
					statementInspector.assertNumberOfJoins( 0, 2 );
				}
		);
	}
}
