/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;

import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = CriteriaFromHqlTest.Message.class)
public class CriteriaFromHqlTest {

	@BeforeEach
	void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new Message(1L, "hello") );
			s.persist( new Message(2L, "bye") );
		} );
	}

	@AfterEach
	void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void test(SessionFactoryScope scope) {
		scope.inSession( s -> {
			JpaCriteriaQuery<Object[]> query =
					s.getFactory().getCriteriaBuilder()
							.createQuery("select id, text from Msg order by id", Object[].class);
			assertEquals(2, query.getSelection().getSelectionItems().size());
			assertEquals(1, query.getOrderList().size());
			assertEquals(1, query.getRoots().size());
			List<Object[]> list = s.createSelectionQuery(query).getResultList();
			assertEquals(2, list.size());
		});
	}

	@Test
	void testCreateQueryWithNamedParameters(SessionFactoryScope scope) {
		scope.inSession( s -> {
			final var criteriaBuilder = s.getCriteriaBuilder();
			final CriteriaQuery<Message> query =
					criteriaBuilder.createQuery( Message.class,
							"select m from Msg m where m.id = :id and m.text = :text" );

			assertEquals( 2, query.getParameters().size() );
			assertEquals( Message.class, query.getResultType() );

			final Message message =
					s.createSelectionQuery( query )
							.setParameter( "id", 1L )
							.setParameter( "text", "hello" )
							.getSingleResult();

			assertEquals( 1L, message.id );
			assertEquals( "hello", message.text );
		} );
	}

	@Test
	void testCreateCriteriaUpdateWithNamedParameters(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final var criteriaBuilder = s.getCriteriaBuilder();
			final CriteriaUpdate<Message> update =
					criteriaBuilder.createCriteriaUpdate( Message.class,
							"update Msg m set m.text = :newText where m.id = :id and m.text = :oldText" );

			assertEquals( 3, update.getParameters().size() );

			final int updateCount =
					s.createMutationQuery( update )
							.setParameter( "newText", "updated" )
							.setParameter( "id", 1L )
							.setParameter( "oldText", "hello" )
							.executeUpdate();

			assertEquals( 1, updateCount );
			assertEquals( "updated", s.find( Message.class, 1L ).text );
		} );
	}

	@Test
	void testCreateCriteriaDeleteWithNamedParameters(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final var criteriaBuilder = s.getCriteriaBuilder();
			final CriteriaDelete<Message> delete =
					criteriaBuilder.createCriteriaDelete( Message.class,
							"delete from Msg m where m.id = :id and m.text = :text" );

			assertEquals( 2, delete.getParameters().size() );

			final int deleteCount =
					s.createMutationQuery( delete )
							.setParameter( "id", 1L )
							.setParameter( "text", "hello" )
							.executeUpdate();

			assertEquals( 1, deleteCount );
			assertEquals( 1L, s.createQuery( "select count(m) from Msg m", Long.class ).getSingleResult() );
		} );
	}

	@Entity(name="Msg")
	static class Message {
		public Message(Long id, String text) {
			this.id = id;
			this.text = text;
		}
		Message() {}
		@Id
		Long id;
		String text;
	}
}
