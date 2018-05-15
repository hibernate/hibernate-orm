package org.hibernate.jpa.test.query;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaDelete;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Test
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
            assertEquals("ID", result.get(0).getAlias());
            assertEquals(String.class, result.get(1).getJavaType());
            assertEquals("FIRSTNAME", result.get(1).getAlias());
        });
    }

    @Test
    public void testPositionalGetterWithNamedNativeQueryShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0));
            assertEquals("Arnold", tuple.get(1));
        });
    }

    @Test
    public void testPositionalGetterWithNamedNativeQueryWithClassShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0, BigInteger.class));
            assertEquals("Arnold", tuple.get(1, String.class));
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryWithClassShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryWithClassShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testPositionalGetterWithNamedNativeQueryWithClassShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test
    public void testAliasGetterWithNamedNativeQueryWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID"));
            assertEquals("Arnold", tuple.get("FIRSTNAME"));
        });
    }

    @Test
    public void testAliasGetterWithNamedNativeQueryShouldWorkWithoutExplicitAliasWhenLowerCaseAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get("id");
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAliasGetterWithNamedNativeQueryShouldThrowExceptionWithoutExplicitAliasWhenWrongAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get("e");
        });
    }

    @Test
    public void testAliasGetterWithNamedNativeQueryWithClassWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID", BigInteger.class));
            assertEquals("Arnold", tuple.get("FIRSTNAME", String.class));
        });
    }


    @Test
    public void testAliasGetterWithNamedNativeQueryWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard_with_alias");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1"));
            assertEquals("Arnold", tuple.get("ALIAS2"));
        });
    }

    @Test
    public void testAliasGetterWithNamedNativeQueryWithClassWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleNamedResult(entityManager, "standard_with_alias");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1", BigInteger.class));
            assertEquals("Arnold", tuple.get("ALIAS2", String.class));
        });
    }

    @Test
    public void testToArrayShouldWithNamedNativeQueryWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = getTupleNamedResult(entityManager, "standard");
            Object[] result = tuples.get(0).toArray();
            assertArrayEquals(new Object[]{BigInteger.ONE, "Arnold"}, result);
        });
    }

    @Test
    public void testGetElementsWithNamedNativeQueryShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = getTupleNamedResult(entityManager, "standard");
            List<TupleElement<?>> result = tuples.get(0).getElements();
            assertEquals(2, result.size());
            assertEquals(BigInteger.class, result.get(0).getJavaType());
            assertEquals("ID", result.get(0).getAlias());
            assertEquals(String.class, result.get(1).getJavaType());
            assertEquals("FIRSTNAME", result.get(1).getAlias());
        });
    }

    @Test
    public void testStreamedPositionalGetterShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0));
            assertEquals("Arnold", tuple.get(1));
        });
    }

    @Test
    public void testStreamedPositionalGetterWithClassShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0, BigInteger.class));
            assertEquals("Arnold", tuple.get(1, String.class));
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterWithClassShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterWithClassShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterWithClassShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test
    public void testStreamedAliasGetterWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID"));
            assertEquals("Arnold", tuple.get("FIRSTNAME"));
        });
    }

    @Test
    public void testStreamedAliasGetterShouldWorkWithoutExplicitAliasWhenLowerCaseAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get("id");
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStreamedAliasGetterShouldThrowExceptionWithoutExplicitAliasWhenWrongAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            tuple.get("e");
        });
    }

    @Test
    public void testStreamedAliasGetterWithClassWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedTupleResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID", BigInteger.class));
            assertEquals("Arnold", tuple.get("FIRSTNAME", String.class));
        });
    }


    @Test
    public void testStreamedAliasGetterWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleAliasedResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1"));
            assertEquals("Arnold", tuple.get("ALIAS2"));
        });
    }

    @Test
    public void testStreamedAliasGetterWithClassWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getTupleAliasedResult(entityManager);
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1", BigInteger.class));
            assertEquals("Arnold", tuple.get("ALIAS2", String.class));
        });
    }

    @Test
    public void testStreamedToArrayShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = getStreamedTupleResult(entityManager);
            Object[] result = tuples.get(0).toArray();
            assertArrayEquals(new Object[]{BigInteger.ONE, "Arnold"}, result);
        });
    }

    @Test
    public void testStreamedGetElementsShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = getStreamedTupleResult(entityManager);
            List<TupleElement<?>> result = tuples.get(0).getElements();
            assertEquals(2, result.size());
            assertEquals(BigInteger.class, result.get(0).getJavaType());
            assertEquals("ID", result.get(0).getAlias());
            assertEquals(String.class, result.get(1).getJavaType());
            assertEquals("FIRSTNAME", result.get(1).getAlias());
        });
    }

    @Test
    public void testStreamedPositionalGetterWithNamedNativeQueryShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0));
            assertEquals("Arnold", tuple.get(1));
        });
    }

    @Test
    public void testStreamedPositionalGetterWithNamedNativeQueryWithClassShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get(0, BigInteger.class));
            assertEquals("Arnold", tuple.get(1, String.class));
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterWithNamedNativeQueryWithClassShouldThrowExceptionWhenLessThanZeroGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(-1);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterWithNamedNativeQueryWithClassShouldThrowExceptionWhenTupleSizePositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(2);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterWithNamedNativeQueryShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test(expected = IllegalArgumentException.class)
    public void testStreamedPositionalGetterWithNamedNativeQueryWithClassShouldThrowExceptionWhenExceedingPositionGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get(3);
        });
    }


    @Test
    public void testStreamedAliasGetterWithNamedNativeQueryWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID"));
            assertEquals("Arnold", tuple.get("FIRSTNAME"));
        });
    }

    @Test
    public void testStreamedAliasGetterWithNamedNativeQueryShouldWorkWithoutExplicitAliasWhenLowerCaseAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get("id");
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStreamedAliasGetterWithNamedNativeQueryShouldThrowExceptionWithoutExplicitAliasWhenWrongAliasGiven() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            tuple.get("e");
        });
    }

    @Test
    public void testStreamedAliasGetterWithNamedNativeQueryWithClassWithoutExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ID", BigInteger.class));
            assertEquals("Arnold", tuple.get("FIRSTNAME", String.class));
        });
    }


    @Test
    public void testStreamedAliasGetterWithNamedNativeQueryWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard_with_alias");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1"));
            assertEquals("Arnold", tuple.get("ALIAS2"));
        });
    }

    @Test
    public void testStreamedAliasGetterWithNamedNativeQueryWithClassWithExplicitAliasShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> result = getStreamedNamedTupleResult(entityManager, "standard_with_alias");
            Tuple tuple = result.get(0);
            assertEquals(BigInteger.ONE, tuple.get("ALIAS1", BigInteger.class));
            assertEquals("Arnold", tuple.get("ALIAS2", String.class));
        });
    }

    @Test
    public void testStreamedToArrayShouldWithNamedNativeQueryWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = getStreamedNamedTupleResult(entityManager, "standard");
            Object[] result = tuples.get(0).toArray();
            assertArrayEquals(new Object[]{BigInteger.ONE, "Arnold"}, result);
        });
    }

    @Test
    public void testStreamedGetElementsWithNamedNativeQueryShouldWorkProperly() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = getStreamedNamedTupleResult(entityManager, "standard");
            List<TupleElement<?>> result = tuples.get(0).getElements();
            assertEquals(2, result.size());
            assertEquals(BigInteger.class, result.get(0).getJavaType());
            assertEquals("ID", result.get(0).getAlias());
            assertEquals(String.class, result.get(1).getJavaType());
            assertEquals("FIRSTNAME", result.get(1).getAlias());
        });
    }

    @Test
    @TestForIssue(jiraKey = "HHH-11897")
    public void testGetElementsShouldNotThrowExceptionWhenResultContainsNullValue() {
        doInJPA(this::entityManagerFactory, entityManager -> {
            User user = entityManager.find(User.class, 1L);
            user.firstName = null;
        });
        doInJPA(this::entityManagerFactory, entityManager -> {
            List<Tuple> tuples = getTupleResult(entityManager);
            final Tuple tuple = tuples.get(0);
            List<TupleElement<?>> result = tuple.getElements();
            assertEquals(2, result.size());
            final TupleElement<?> firstTupleElement = result.get(0);
            assertEquals(BigInteger.class, firstTupleElement.getJavaType());
            assertEquals("ID", firstTupleElement.getAlias());
            assertEquals(BigInteger.valueOf(1L), tuple.get(firstTupleElement.getAlias()));
            final TupleElement<?> secondTupleElement = result.get(1);
            assertEquals(Object.class, secondTupleElement.getJavaType());
            assertEquals("FIRSTNAME", secondTupleElement.getAlias());
            assertNull(tuple.get(secondTupleElement.getAlias()));
        });
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> getTupleAliasedResult(EntityManager entityManager) {
        Query query = entityManager.createNativeQuery("SELECT id AS alias1, firstname AS alias2 FROM users", Tuple.class);
        return (List<Tuple>) query.getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> getStreamedTupleAliasedResult(EntityManager entityManager) {
        NativeQueryImplementor query = (NativeQueryImplementor) entityManager.createNativeQuery("SELECT id AS alias1, firstname AS alias2 FROM users", Tuple.class);
        return (List<Tuple>) query.stream().collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> getTupleResult(EntityManager entityManager) {
        Query query = entityManager.createNativeQuery("SELECT id, firstname FROM users", Tuple.class);
        return (List<Tuple>) query.getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> getTupleNamedResult(EntityManager entityManager, String name) {
        return entityManager.createNamedQuery(name, Tuple.class).getResultList();
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> getStreamedTupleResult(EntityManager entityManager) {
        NativeQueryImplementor query = (NativeQueryImplementor) entityManager.createNativeQuery("SELECT id, firstname FROM users", Tuple.class);
        return (List<Tuple>) query.stream().collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<Tuple> getStreamedNamedTupleResult(EntityManager entityManager, String name) {
        return (List<Tuple>)((NativeQueryImplementor) entityManager.createNamedQuery(name, Tuple.class)).stream().collect(Collectors.toList());
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
