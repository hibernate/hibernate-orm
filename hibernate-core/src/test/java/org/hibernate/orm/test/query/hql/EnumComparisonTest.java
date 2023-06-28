package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.query.SemanticException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = EnumComparisonTest.WithEnum.class)
public class EnumComparisonTest {
    @Test
    void test(SessionFactoryScope scope) {
        scope.inTransaction(session -> {
            session.persist(new WithEnum());
            assertEquals(1,
                    session.createSelectionQuery("from WithEnum where stringEnum > X").getResultList().size());
            assertEquals(1,
                    session.createSelectionQuery("from WithEnum where ordinalEnum > X").getResultList().size());
            assertEquals(1,
                    session.createSelectionQuery("from WithEnum where stringEnum > 'X'").getResultList().size());
            assertEquals(1,
                    session.createSelectionQuery("from WithEnum where ordinalEnum > 1").getResultList().size());
            try {
                session.createSelectionQuery("from WithEnum where ordinalEnum > 'X'").getResultList();
                fail();
            }
            catch (SemanticException se) {
            }
            try {
                session.createSelectionQuery("from WithEnum where stringEnum > 1").getResultList();
                fail();
            }
            catch (SemanticException se) {
            }
            session.createSelectionQuery("select max(ordinalEnum) from WithEnum").getSingleResult();
            session.createSelectionQuery("select max(stringEnum) from WithEnum").getSingleResult();
        });
    }

    enum Enum { X, Y, Z }

    @Entity(name = "WithEnum")
    static class WithEnum {
        @Id
        @GeneratedValue
        long id;

        @Enumerated(EnumType.STRING)
        Enum stringEnum = Enum.Y;

        @Enumerated(EnumType.ORDINAL)
        Enum ordinalEnum = Enum.Z;
    }
}
