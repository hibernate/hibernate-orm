package org.hibernate.orm.test.annotations.usertype;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = MyEntity.class)
public class EnhancedUserTypeTest {

    @Test
    void test(SessionFactoryScope scope) {
        scope.inTransaction(session -> session.persist(new MyEntity(new MyId("x1"), "hello world")));
    }

}
