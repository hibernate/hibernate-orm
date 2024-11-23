package org.hibernate.jpa.test.query;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity_;
import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

@TestForIssue( jiraKey = "HHH-15454" )
public class TupleQueryRetrievePrimitiveTest extends AbstractMetamodelSpecificTest {

    public static final int QUANTITY_OF_THING = 3;
    private EntityManager em;


    @Before
    public void createThingWithQuantity() {
        em = getOrCreateEntityManager();
        em.getTransaction().begin();

        ThingWithQuantity thing = new ThingWithQuantity();
        thing.setId( "thingWithQuantity3" );
        thing.setName( "3 Things" );
        thing.setQuantity(QUANTITY_OF_THING);
        em.persist( thing );

        em.getTransaction().commit();
    }

    @After
    public void endEntityManager() {
        em.close();
    }

    @Test
    public void testRetrieveTupleEntryWithPrimitiveType() {
        final Tuple result = queryTuple();
        final int quantity = result.get(ThingWithQuantity_.quantity.getName(), int.class);
        Assert.assertEquals(QUANTITY_OF_THING, quantity);
    }

    @Test
    public void testRetrieveTupleEntryWithMetadata() {
        final Tuple result = queryTuple();
        final int quantity = result.get(ThingWithQuantity_.quantity.getName(), ThingWithQuantity_.quantity.getJavaType());
        Assert.assertEquals(QUANTITY_OF_THING, quantity);
    }

    @Test
    public void testRetrieveTupleEntryFromIndex() {
        final Tuple result = queryTuple();
        final int quantity = result.get(0, ThingWithQuantity_.quantity.getJavaType());
        Assert.assertEquals(QUANTITY_OF_THING, quantity);
    }

    @Test
    public void testRetrieveTupleEntryWithTupleElement() {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Tuple> query = cb.createTupleQuery();
        final Root<ThingWithQuantity> thingWithQuantity = query.from(ThingWithQuantity.class);
        final Path<Integer> tupleElement = thingWithQuantity.get(ThingWithQuantity_.quantity);
        query.multiselect(tupleElement.alias(ThingWithQuantity_.quantity.getName()));
        Tuple result = em.createQuery(query).setMaxResults(1).getSingleResult();
        final int quantity = result.get(tupleElement);
        Assert.assertEquals(QUANTITY_OF_THING, quantity);
    }

    private Tuple queryTuple() {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Tuple> query = cb.createTupleQuery();
        final Root<ThingWithQuantity> thingWithQuantity = query.from(ThingWithQuantity.class);
        query.multiselect(thingWithQuantity.get(ThingWithQuantity_.quantity).alias(ThingWithQuantity_.quantity.getName()));
        return em.createQuery(query).setMaxResults(1).getSingleResult();
    }
}
