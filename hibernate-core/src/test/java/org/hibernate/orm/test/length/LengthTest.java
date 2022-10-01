package org.hibernate.orm.test.length;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {WithLongStrings.class,WithLongTypeStrings.class})
public class LengthTest {
    @Test
    public void testLength(SessionFactoryScope scope) {
        WithLongStrings strings = new WithLongStrings();
        strings.longish = "hello world ".repeat(2500);
        strings.long16 = "hello world ".repeat(2700);
        strings.long32 = "hello world ".repeat(20000);
        scope.inTransaction(s->s.persist(strings));
        scope.inTransaction(s-> {
            WithLongStrings strs = s.find(WithLongStrings.class, strings.id);
            assertEquals(strs.longish, strings.longish);
            assertEquals(strs.long16, strings.long16);
            assertEquals(strs.long32, strings.long32);
        });
    }
    @Test
    public void testSqlType(SessionFactoryScope scope) {
        WithLongTypeStrings strings = new WithLongTypeStrings();
        strings.longish = "hello world ".repeat(2500);
        strings.long32 = "hello world ".repeat(20000);
        scope.inTransaction(s->s.persist(strings));
        scope.inTransaction(s-> {
            WithLongTypeStrings strs = s.find(WithLongTypeStrings.class, strings.id);
            assertEquals(strs.longish, strings.longish);
            assertEquals(strs.long32, strings.long32);
        });
    }
}
