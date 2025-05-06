/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = CriteriaFromHqlTest.Message.class)
public class CriteriaFromHqlTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new Message(1L, "hello") );
			s.persist( new Message(2L, "bye") );
		});

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
