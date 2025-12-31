/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yoobin Yoon
 */
@JiraKey("HHH-18040")
@RequiresDialect(MySQLDialect.class)
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class MySQLDmlSubqueryTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityOfBasics e1 = new EntityOfBasics( 1 );
			e1.setTheString( "A" );
			e1.setTheInteger( 100 );
			session.persist( e1 );

			EntityOfBasics e2 = new EntityOfBasics( 2 );
			e2.setTheString( "B" );
			e2.setTheInteger( 200 );
			session.persist( e2 );

			EntityOfBasics e3 = new EntityOfBasics( 3 );
			e3.setTheString( "C" );
			e3.setTheInteger( 300 );
			session.persist( e3 );

			EntityOfBasics e4 = new EntityOfBasics( 4 );
			e4.setTheString( "D" );
			e4.setTheInteger( 150 );
			session.persist( e4 );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityOfBasics" ).executeUpdate();
		} );
	}

	@Test
	void testDeleteWithExistsSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where exists (select 1 from EntityOfBasics e2 where e2.theInteger > e.theInteger)"
			).executeUpdate();

			assertEquals( 3, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 1, count );
		} );
	}

	@Test
	void testDeleteWithNotExistsSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where not exists (select 1 from EntityOfBasics e2 where e2.theInteger > e.theInteger)"
			).executeUpdate();

			assertEquals( 1, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 3, count );
		} );
	}

	@Test
	void testUpdateWithExistsSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int updated = session.createMutationQuery(
					"update EntityOfBasics e set e.theInteger = e.theInteger * 2 where exists (select 1 from EntityOfBasics e2 where e2.theInteger < e.theInteger)"
			).executeUpdate();

			assertEquals( 3, updated );
		} );

		scope.inTransaction( session -> {
			Integer value100 = session.createSelectionQuery(
					"select e.theInteger from EntityOfBasics e where e.id = 1", Integer.class
			).getSingleResult();
			assertEquals( 100, value100 );

			Integer value200 = session.createSelectionQuery(
					"select e.theInteger from EntityOfBasics e where e.id = 2", Integer.class
			).getSingleResult();
			assertEquals( 400, value200 );
		} );
	}

	@Test
	void testDeleteWithInSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where e.theInteger in (select e2.theInteger from EntityOfBasics e2 where e2.theInteger < 200)"
			).executeUpdate();

			assertEquals( 2, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 2, count );
		} );
	}

	@Test
	void testDeleteWithNotInSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where e.theInteger not in (select e2.theInteger from EntityOfBasics e2 where e2.theInteger < 200)"
			).executeUpdate();

			assertEquals( 2, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 2, count );
		} );
	}

	@Test
	void testUpdateWithInSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int updated = session.createMutationQuery(
					"update EntityOfBasics e set e.theInteger = 0 where e.theString in (select e2.theString from EntityOfBasics e2 where e2.theInteger >= 200)"
			).executeUpdate();

			assertEquals( 2, updated );
		} );

		scope.inTransaction( session -> {
			Long countZero = session.createSelectionQuery(
					"select count(*) from EntityOfBasics e where e.theInteger = 0", Long.class
			).getSingleResult();
			assertEquals( 2, countZero );
		} );
	}

	@Test
	void testDeleteWithScalarSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where e.theInteger < (select avg(e2.theInteger) from EntityOfBasics e2)"
			).executeUpdate();

			assertEquals( 2, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 2, count );
		} );
	}

	@Test
	void testDeleteWithAnySubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where e.theInteger > any (select e2.theInteger from EntityOfBasics e2 where e2.theInteger < 200)"
			).executeUpdate();

			assertEquals( 3, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 1, count );
		} );
	}

	@Test
	void testDeleteWithAllSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where e.theInteger > all (select e2.theInteger from EntityOfBasics e2 where e2.theInteger < 200)"
			).executeUpdate();

			assertEquals( 2, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 2, count );
		} );
	}

	@Test
	void testDeleteWithCorrelatedSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where e.theInteger = (select max(e2.theInteger) from EntityOfBasics e2 where e2.theString = e.theString)"
			).executeUpdate();

			assertEquals( 4, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 0, count );
		} );
	}

	@Test
	void testDeleteWithTupleInSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where (e.id, e.theInteger) in (select e2.id, e2.theInteger from EntityOfBasics e2 where e2.theInteger < 200)"
			).executeUpdate();

			assertEquals( 2, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 2, count );
		} );
	}

	@Test
	void testDeleteWithTupleInGroupBySubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where (e.theString, e.theInteger) in (" +
							"  select e2.theString, max(e2.theInteger) from EntityOfBasics e2 " +
							"  group by e2.theString" +
							")"
			).executeUpdate();

			assertEquals( 4, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 0, count );
		} );
	}

	@Test
	void testDeleteWithTupleInGroupByHavingSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityOfBasics e5 = new EntityOfBasics( 5 );
			e5.setTheString( "A" );
			e5.setTheInteger( 50 );
			session.persist( e5 );
		} );

		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where (e.theString, e.theInteger) in (" +
							"  select e2.theString, max(e2.theInteger) from EntityOfBasics e2 " +
							"  group by e2.theString " +
							"  having count(*) > 1" +
							")"
			).executeUpdate();

			assertEquals( 1, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 4, count );
		} );
	}

	@Test
	void testDeleteWithTupleComparisonSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where (e.id, e.theInteger) < all (select e2.id, e2.theInteger from EntityOfBasics e2 where e2.theInteger > 200)"
			).executeUpdate();

			assertEquals( 2, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 2, count );
		} );
	}

	@Test
	void testUpdateWithTupleInSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int updated = session.createMutationQuery(
					"update EntityOfBasics e set e.theInteger = 0 where (e.theString, e.theInteger) in (select e2.theString, e2.theInteger from EntityOfBasics e2 where e2.theInteger >= 200)"
			).executeUpdate();

			assertEquals( 2, updated );
		} );

		scope.inTransaction( session -> {
			Long countZero = session.createSelectionQuery(
					"select count(*) from EntityOfBasics e where e.theInteger = 0", Long.class
			).getSingleResult();
			assertEquals( 2, countZero );
		} );
	}

	@Test
	void testUpdateWithSetClauseSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int updated = session.createMutationQuery(
					"update EntityOfBasics e set e.theInteger = (select max(e2.theInteger) from EntityOfBasics e2) where e.theInteger < 200"
			).executeUpdate();

			assertEquals( 2, updated );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery(
					"select count(*) from EntityOfBasics where theInteger = 300", Long.class
			).getSingleResult();
			assertEquals( 3, count );
		} );
	}

	@Test
	void testDeleteWithNestedSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int deleted = session.createMutationQuery(
					"delete from EntityOfBasics e where e.theInteger > (" +
							"  select avg(e2.theInteger) from EntityOfBasics e2 " +
							"  where e2.theInteger < (select max(e3.theInteger) from EntityOfBasics e3)" +
							")"
			).executeUpdate();

			assertEquals( 2, deleted );
		} );

		scope.inTransaction( session -> {
			long count = session.createSelectionQuery( "select count(*) from EntityOfBasics", Long.class )
					.getSingleResult();
			assertEquals( 2, count );
		} );
	}

	@Test
	void testUpdateWithNestedSubqueryReferencingSameTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			int updated = session.createMutationQuery(
					"update EntityOfBasics e set e.theInteger = 104 where e.theInteger > (" +
							"  select avg(e2.theInteger) from EntityOfBasics e2 " +
							"  where e2.theInteger < (select max(e3.theInteger) from EntityOfBasics e3)" +
							")"
			).executeUpdate();

			assertEquals( 2, updated );
		} );

		scope.inTransaction( session -> {
			Long count104 = session.createSelectionQuery(
					"select count(*) from EntityOfBasics e where e.theInteger = 104", Long.class
			).getSingleResult();
			assertEquals( 2, count104 );
		} );
	}

}
