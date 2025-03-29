/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.query;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import org.hibernate.query.criteria.CriteriaDefinition;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = CriteriaDefinitionTest.Message.class)
@Jpa(annotatedClasses = CriteriaDefinitionTest.Message.class)
public class CriteriaDefinitionTest {

	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new Message(1L, "hello") );
			s.persist( new Message(2L, "bye") );
		});

		var factory = scope.getSessionFactory();

		var query1 = new CriteriaDefinition<>(factory, Object[].class) {{
			var message = from(Message.class);
			select(array(message.get("id"), message.get("text")));
			where(like(message.get("text"), "hell%"), message.get("id").equalTo(1));
			orderBy(asc(message.get("id")));
		}};

		var query2 = new CriteriaDefinition<>(factory, Message.class) {{
			var message = from(Message.class);
			where(like(message.get("text"), "hell%"), message.get("id").equalTo(1));
			orderBy(asc(message.get("id")));
		}};

		var query3 = new CriteriaDefinition<>(factory, Message.class, "from Msg") {{
			var message = (JpaRoot<Message>) getSelection();
			where(ilike(message.get("text"), "%e%"));
			orderBy(asc(message.get("text")));
		}};

		var query3prime = new CriteriaDefinition<>(factory, Message.class, "from Msg") {{
			var message = getRoot( 0, Message.class );
			where(ilike(message.get("text"), "%e%"));
			orderBy(asc(message.get("text")));
		}};

		var query4 = new CriteriaDefinition<>(factory, Message.class) {{
			var message = from(Message.class);
			restrict(like(message.get("text"), "hell%"));
			restrict(message.get("id").equalTo(1));
			orderBy(asc(message.get("id")));
		}};

		var query5 = new CriteriaDefinition<>(query4, Long.class) {{
			select(count());
			orderBy();
		}};

		scope.inSession(session -> {
			var idAndText = session.createSelectionQuery(query1).getSingleResult();
			assertNotNull(idAndText);
			assertEquals(1L,idAndText[0]);
			assertEquals("hello",idAndText[1]);

			var message = session.createSelectionQuery(query2).getSingleResult();
			assertNotNull(message);
			assertEquals(1L,message.id);
			assertEquals("hello",message.text);

			var messages = session.createSelectionQuery(query3).getResultList();
			assertEquals(2,messages.size());

			var messagesPrime = session.createSelectionQuery(query3prime).getResultList();
			assertEquals(2,messagesPrime.size());

			var msg = session.createSelectionQuery(query4).getSingleResult();
			assertNotNull(msg);
			assertEquals(1L,msg.id);
			assertEquals("hello",msg.text);

			long count = session.createSelectionQuery(query5).getSingleResult();
			assertEquals(1L,count);
		});
	}

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist( new Message(1L, "hello") );
			s.persist( new Message(2L, "bye") );
		});

		EntityManagerFactory factory = scope.getEntityManagerFactory();

		var query1 = new CriteriaDefinition<>(factory, Object[].class) {{
			var message = from(Message.class);
			select(array(message.get("id"), message.get("text")));
			where(like(message.get("text"), "hell%"), message.get("id").equalTo(1));
			orderBy(asc(message.get("id")));
		}};

		var query2 = new CriteriaDefinition<>(factory, Message.class) {{
			var message = from(Message.class);
			where(like(message.get("text"), "hell%"), message.get("id").equalTo(1));
			orderBy(asc(message.get("id")));
		}};

		var query3 = new CriteriaDefinition<>(factory, Message.class, "from Msg") {{
			var message = (JpaRoot<Message>) getSelection();
			where(ilike(message.get("text"), "%e%"));
			orderBy(asc(message.get("text")));
		}};

		var query4 = new CriteriaDefinition<>(factory, Message.class) {{
			var message = from(Message.class);
			restrict(like(message.get("text"), "hell%"));
			restrict(message.get("id").equalTo(1));
			orderBy(asc(message.get("id")));
		}};

		scope.inTransaction(entityManager -> {
			var idAndText = entityManager.createQuery(query1).getSingleResult();
			assertNotNull(idAndText);
			assertEquals(1L,idAndText[0]);
			assertEquals("hello",idAndText[1]);

			var message = entityManager.createQuery(query2).getSingleResult();
			assertNotNull(message);
			assertEquals(1L,message.id);
			assertEquals("hello",message.text);

			var messages = entityManager.createQuery(query3).getResultList();
			assertEquals(2,messages.size());

			var msg = entityManager.createQuery(query4).getSingleResult();
			assertNotNull(msg);
			assertEquals(1L,msg.id);
			assertEquals("hello",msg.text);
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
