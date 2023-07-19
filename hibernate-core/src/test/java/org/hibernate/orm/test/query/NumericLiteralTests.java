/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.query;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = EntityOfBasics.class)
@SessionFactory(useCollectingStatementInspector = true)
public class NumericLiteralTests {
	@Test
	void testIntegerLiteral(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Long count = session.createSelectionQuery(
					"select count(1) from EntityOfBasics where theInt = 123456",
					Long.class
			).getSingleResult();
			assertThat( count ).isEqualTo( 1L );
		} );

		scope.inTransaction( (session) -> {
			final Long count = session.createSelectionQuery(
					"select count(1) from EntityOfBasics where theInteger = 987654",
					Long.class
			).getSingleResult();
			assertThat( count ).isEqualTo( 1L );
		} );
	}

	@Test
	void testLongLiteral(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Long count = session.createSelectionQuery(
					"select count(1) from EntityOfBasics where theInt = 123456L",
					Long.class
			).getSingleResult();
			assertThat( count ).isEqualTo( 1L );
		} );

		scope.inTransaction( (session) -> {
			final Long count = session.createSelectionQuery(
					"select count(1) from EntityOfBasics where theInteger = 987654L",
					Long.class
			).getSingleResult();
			assertThat( count ).isEqualTo( 1L );
		} );
	}

	@Test
	void testDoubleLiteral(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Long count = session.createSelectionQuery(
					"select count(1) from EntityOfBasics where theDouble = 123.456",
					Long.class
			).getSingleResult();
			assertThat( count ).isEqualTo( 1L );
		} );
	}

	@Test
	void testFloatLiteral(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Long count = session.createSelectionQuery(
					"select count(1) from EntityOfBasics where theDouble = 123.456F",
					Long.class
			).getSingleResult();
			assertThat( count ).isEqualTo( 1L );
		} );
	}

	@BeforeEach
	void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityOfBasics entity = new EntityOfBasics( 1 );
			entity.setTheDouble( 123.456 );
			entity.setTheInt( 123456 );
			entity.setTheInteger( 987654 );
			session.persist( entity );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete EntityOfBasics" ).executeUpdate();
		} );
	}
}
