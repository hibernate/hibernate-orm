package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.QuerySettings.PORTABLE_INTEGER_DIVISION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel
@SessionFactory
@ServiceRegistry(settings = @Setting(name = PORTABLE_INTEGER_DIVISION, value = "true"))
public class IntegerDivisionTest {
    @Test
    public void testIntegerDivision(SessionFactoryScope scope) {
        scope.inTransaction(s -> {
            assertFalse( s.createQuery("select 1 where 1/2 = 0 and 4/3 = 1", Integer.class)
                    .getResultList().isEmpty() );
            assertEquals( 1, s.createQuery("select 4/3", Integer.class)
                    .getSingleResult() );
        });
    }
}
