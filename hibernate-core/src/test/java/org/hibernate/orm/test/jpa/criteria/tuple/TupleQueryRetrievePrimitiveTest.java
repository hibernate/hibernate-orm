/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.tuple;

import org.hibernate.orm.test.jpa.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity_;

import org.hibernate.testing.orm.junit.JiraKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

@JiraKey( value = "HHH-15454" )
public class TupleQueryRetrievePrimitiveTest extends AbstractMetamodelSpecificTest {

	public static final int QUANTITY_OF_THING = 3;
	public static final String THING_WITH_QUANTITY_3_ID = "thingWithQuantity3";
	private EntityManager em;


	@BeforeEach
	public void createThingWithQuantity() {
		em = getOrCreateEntityManager();
		em.getTransaction().begin();

		ThingWithQuantity thing = new ThingWithQuantity();
		thing.setId( THING_WITH_QUANTITY_3_ID );
		thing.setName( "3 Things" );
		thing.setQuantity(QUANTITY_OF_THING);
		em.persist( thing );

		em.getTransaction().commit();
	}

	@AfterEach
	public void endEntityManager() {
		em.getTransaction().begin();
		em.remove( em.find( ThingWithQuantity.class, THING_WITH_QUANTITY_3_ID ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testRetrieveTupleEntryWithPrimitiveType() {
		final Tuple result = queryTuple();
		final int quantity = result.get(ThingWithQuantity_.quantity.getName(), int.class);
		assertEquals(QUANTITY_OF_THING, quantity);
	}

	@Test
	public void testRetrieveTupleEntryWithMetadata() {
		final Tuple result = queryTuple();
		final int quantity = result.get( ThingWithQuantity_.quantity.getName(), ThingWithQuantity_.quantity.getJavaType());
		assertEquals(QUANTITY_OF_THING, quantity);
	}

	@Test
	public void testRetrieveTupleEntryFromIndex() {
		final Tuple result = queryTuple();
		final int quantity = result.get(0, ThingWithQuantity_.quantity.getJavaType());
		assertEquals(QUANTITY_OF_THING, quantity);
	}

	@Test
	public void testRetrieveTupleEntryWithTupleElement() {
		final CriteriaBuilder cb = em.getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = cb.createTupleQuery();
		final Root<ThingWithQuantity> thingWithQuantity = query.from( ThingWithQuantity.class);
		final Path<Integer> tupleElement = thingWithQuantity.get( ThingWithQuantity_.quantity);
		query.multiselect(tupleElement.alias(ThingWithQuantity_.quantity.getName()));
		Tuple result = em.createQuery(query).setMaxResults(1).getSingleResult();
		final int quantity = result.get(tupleElement);
		assertEquals(QUANTITY_OF_THING, quantity);
	}

	private Tuple queryTuple() {
		final CriteriaBuilder cb = em.getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = cb.createTupleQuery();
		final Root<ThingWithQuantity> thingWithQuantity = query.from(ThingWithQuantity.class);
		query.multiselect(thingWithQuantity.get(ThingWithQuantity_.quantity).alias(ThingWithQuantity_.quantity.getName()));
		return em.createQuery(query).setMaxResults(1).getSingleResult();
	}
}
