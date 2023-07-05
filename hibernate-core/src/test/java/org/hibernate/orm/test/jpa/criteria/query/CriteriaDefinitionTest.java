package org.hibernate.orm.test.jpa.criteria.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.query.criteria.CriteriaDefinition;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = CriteriaDefinitionTest.Message.class)
public class CriteriaDefinitionTest {

    @Test void test(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            s.persist( new Message(1L, "hello") );
            s.persist( new Message(2L, "bye") );
        });

        scope.inSession(session -> {
            var idAndText = new CriteriaDefinition<>(session, Object[].class) {
                public void define() {
                    var m = from(Message.class);
                    select(array(m.get("id"), m.get("text")));
                    where(like(m.get("text"), "hell%"), m.get("id").equalTo(1));
                    orderBy(asc(m.get("id")));
                }
            }.createSelectionQuery()
                    .getSingleResult();

            assertNotNull(idAndText);
            assertEquals(1L,idAndText[0]);
            assertEquals("hello",idAndText[1]);

            var message = new CriteriaDefinition<>(session, Message.class) {
                public void define() {
                    var m = from(Message.class);
                    where(like(m.get("text"), "hell%"), m.get("id").equalTo(1));
                    orderBy(asc(m.get("id")));
                }
            }.createSelectionQuery()
                    .getSingleResult();

            assertNotNull(message);
            assertEquals(1L,message.id);
            assertEquals("hello",message.text);
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
