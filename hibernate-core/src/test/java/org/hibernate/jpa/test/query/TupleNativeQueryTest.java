package org.hibernate.jpa.test.query;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.RequiresDialect;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaDelete;
import java.math.BigInteger;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RequiresDialect(H2Dialect.class)
public class TupleNativeQueryTest extends BaseEntityManagerFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[]{User.class};
    }

    @Before
    public void setUp() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            User user = new User("Arnold");
            entityManager.persist(user);
        });
    }

    @After
    public void tearDown() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            CriteriaDelete<User> delete = entityManager.getCriteriaBuilder().createCriteriaDelete(User.class);
            delete.from(User.class);
            entityManager.createQuery(delete).executeUpdate();
        });
    }

    @Test
    public void testPositionalGetterShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0));
            assertEquals("Arnold", tuple.get(1));
        });
    }

    @Test
    public void testPositionalGetterWithClassShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0, BigInteger.class));
            assertEquals("Arnold", tuple.get(1, String.class));
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithClassShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithClassShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithClassShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test
    public void testAliasGetterWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID"));
            assertEquals("Arnold", tuple.get("FIRSTNAME"));
        });
    }


    public void testAliasGetterShouldWorkWithoutExplicitAliasWhenLowerCaseAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get("id");
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAliasGetterShouldThrowExceptionWithoutExplicitAliasWhenWrongAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get("e");
        });
    }

    @Test
    public void testAliasGetterWithClassWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID", BigInteger.class));
            assertEquals("Arnold", tuple.get("FIRSTNAME", String.class));
        });
    }


    @Test
    public void testAliasGetterWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleAliasedResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1"));
            assertEquals("Arnold", tuple.get("ALIAS2"));
        });
    }

    @Test
    public void testAliasGetterWithClassWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleAliasedResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1", BigInteger.class));
            assertEquals("Arnold", tuple.get("ALIAS2", String.class));
        });
    }

    @Test
    public void testToArrayShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = getTupleResult(entityManager);
            Object[] result = tuples.get(0).toArray();
            assertArrayEquals(new Object[]{BigInteger.ONE, "Arnold"}, result);
        });
    }

    @Test
    public void testGetElementsShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = getTupleResult(entityManager);
            List<TupleElement<?>> result = tuples.get(0).getElements();
            assertEquals(2, result.size());
            assertEquals(BigInteger.class, result.get(0).getJavaType());
            assertEquals("id", result.get(0).getAlias());
            assertEquals(String.class, result.get(1).getJavaType());
            assertEquals("firstname", result.get(1).getAlias());
        });
    }

    @Test
    public void testPositionalGetterWithNamedNativeQueryShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0));
            assertEquals("Arnold", tuple.get(1));
        });
    }

    @Test
    public void testPositionalGetterWithNamedNativeQueryWithClassShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0, BigInteger.class));
            assertEquals("Arnold", tuple.get(1, String.class));
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryWithClassShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryWithClassShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryWithClassShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test
    public void testAliasGetterWithNamedNativeQueryWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID"));
            assertEquals("Arnold", tuple.get("FIRSTNAME"));
        });
    }


    public void testAliasGetterWithNamedNativeQueryShouldWorkWithoutExplicitAliasWhenLowerCaseAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            tuple.get("id");
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAliasGetterWithNamedNativeQueryShouldThrowExceptionWithoutExplicitAliasWhenWrongAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            tuple.get("e");
        });
    }

    @Test
    public void testAliasGetterWithNamedNativeQueryWithClassWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID", BigInteger.class));
            assertEquals("Arnold", tuple.get("FIRSTNAME", String.class));
        });
    }


    @Test
    public void testAliasGetterWithNamedNativeQueryWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard_with_alias", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1"));
            assertEquals("Arnold", tuple.get("ALIAS2"));
        });
    }

    @Test
    public void testAliasGetterWithNamedNativeQueryWithClassWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = entityManager.createNamedQuery("standard_with_alias", Tuple.class).getResultList();
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1", BigInteger.class));
            assertEquals("Arnold", tuple.get("ALIAS2", String.class));
        });
    }

    @Test
    public void testToArrayShouldWithNamedNativeQueryWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            Object[] result = tuples.get(0).toArray();
            assertArrayEquals(new Object[]{BigInteger.ONE, "Arnold"}, result);
        });
    }

    @Test
    public void testGetElementsWithNamedNativeQueryShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = entityManager.createNamedQuery("standard", Tuple.class).getResultList();
            List<TupleElement<?>> result = tuples.get(0).getElements();
            assertEquals(2, result.size());
            assertEquals(BigInteger.class, result.get(0).getJavaType());
            assertEquals("id", result.get(0).getAlias());
            assertEquals(String.class, result.get(1).getJavaType());
            assertEquals("firstname", result.get(1).getAlias());
        });
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> getTupleAliasedResult(EntityManager entityManager) {
        Query query = entityManager.createNativeQuery("SELECT id AS alias1, firstname AS alias2 FROM users", Tuple.class);
        return (List<Tuple>) query.getResultList();
    }


    @SuppressWarnings("unchecked")
    private List<Tuple> getTupleResult(EntityManager entityManager) {
        Query query = entityManager.createNativeQuery("SELECT id, firstname FROM users", Tuple.class);
        return (List<Tuple>) query.getResultList();
    }

    @Entity
    @Table(name = "users")
    @NamedNativeQueries({
            @NamedNativeQuery(
                    name = "standard",
                    query = "SELECT id, firstname FROM users"
            ),
            @NamedNativeQuery(
                    name = "standard_with_alias",
                    query = "SELECT id AS alias1, firstname AS alias2 FROM users"
            )
    })
    public static class User {
        @Id
        private long id;

        private String firstName;

        public User() {
        }

        public User(String firstName) {
            this.id = 1L;
            this.firstName = firstName;
        }
    }
}
