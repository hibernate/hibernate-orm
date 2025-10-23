/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.tuple;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity_;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey( value = "HHH-15454" )
@Jpa(annotatedClasses = {ThingWithQuantity.class})
public class TupleQueryRetrievePrimitiveTest {

	public static final int QUANTITY_OF_THING = 3;
	public static final String THING_WITH_QUANTITY_3_ID = "thingWithQuantity3";

	@BeforeEach
	public void createThingWithQuantity(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			ThingWithQuantity thing = new ThingWithQuantity();
			thing.setId( THING_WITH_QUANTITY_3_ID );
			thing.setName( "3 Things" );
			thing.setQuantity( QUANTITY_OF_THING );
			entityManager.persist( thing );
		} );
	}

	@AfterEach
	public void endEntityManager(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testRetrieveTupleEntryWithPrimitiveType(EntityManagerFactoryScope scope) {
		final Tuple result = queryTuple(scope);
		final int quantity = result.get(ThingWithQuantity_.quantity.getName(), int.class);
		assertEquals(QUANTITY_OF_THING, quantity);
	}

	@Test
	public void testRetrieveTupleEntryWithMetadata(EntityManagerFactoryScope scope) {
		final Tuple result = queryTuple(scope);
		final int quantity = result.get( ThingWithQuantity_.quantity.getName(), ThingWithQuantity_.quantity.getJavaType());
		assertEquals(QUANTITY_OF_THING, quantity);
	}

	@Test
	public void testRetrieveTupleEntryFromIndex(EntityManagerFactoryScope scope) {
		final Tuple result = queryTuple(scope);
		final int quantity = result.get(0, ThingWithQuantity_.quantity.getJavaType());
		assertEquals(QUANTITY_OF_THING, quantity);
	}

	@Test
	public void testRetrieveTupleEntryWithTupleElement(EntityManagerFactoryScope scope) {
		final CriteriaBuilder cb = scope.getEntityManagerFactory().getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = cb.createTupleQuery();
		final Root<ThingWithQuantity> thingWithQuantity = query.from( ThingWithQuantity.class);
		final Path<Integer> tupleElement = thingWithQuantity.get( ThingWithQuantity_.quantity);
		query.multiselect(tupleElement.alias(ThingWithQuantity_.quantity.getName()));
		Tuple result = scope.fromEntityManager( entityManager -> entityManager.createQuery(query).setMaxResults(1).getSingleResult() );
		final int quantity = result.get(tupleElement);
		assertEquals(QUANTITY_OF_THING, quantity);
	}

	private Tuple queryTuple(EntityManagerFactoryScope scope) {
		final CriteriaBuilder cb = scope.getEntityManagerFactory().getCriteriaBuilder();
		final CriteriaQuery<Tuple> query = cb.createTupleQuery();
		final Root<ThingWithQuantity> thingWithQuantity = query.from(ThingWithQuantity.class);
		query.multiselect(thingWithQuantity.get(ThingWithQuantity_.quantity).alias(ThingWithQuantity_.quantity.getName()));
		return scope.fromEntityManager( entityManager -> entityManager.createQuery(query).setMaxResults(1).getSingleResult() );
	}
}
