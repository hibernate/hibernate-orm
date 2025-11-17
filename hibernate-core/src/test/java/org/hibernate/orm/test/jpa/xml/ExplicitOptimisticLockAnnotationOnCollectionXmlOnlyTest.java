/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@JiraKey(value = "HHH-19528")
@Jpa(
		xmlMappings = {"org/hibernate/orm/test/jpa/xml/ExplicitOptimisticLockAnnotationOnCollectionXmlOnlyTest.xml"},
		useCollectingStatementInspector = true
)
class ExplicitOptimisticLockAnnotationOnCollectionXmlOnlyTest {

	private static SQLStatementInspector statementInspector;
	private static int consumerId;


	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		statementInspector = scope.getCollectingStatementInspector();

		scope.inTransaction( em -> {
			Consumer consumer = new Consumer();
			em.persist( consumer );
			consumerId = consumer.getId();

			ConsumerItem item1 = new ConsumerItem();
			item1.setConsumer( consumer );
			em.persist( item1 );

			ConsumerItem item2 = new ConsumerItem();
			item2.setConsumer( consumer );
			em.persist( item2 );
		} );
	}

	@Test
	void test(EntityManagerFactoryScope scope) {
		statementInspector.clear();

		scope.inTransaction( em -> {
			Consumer consumer = em.find( Consumer.class, consumerId );
			ConsumerItem inventory = new ConsumerItem();
			inventory.setConsumer( consumer );
			consumer.getConsumerItems().add( inventory );
		} );
		statementInspector.assertUpdate();
		statementInspector.assertInsert();
	}

	@Test
	void testVersionOnMappedSupertype(EntityManagerFactoryScope scope) {
		var shop = scope.fromTransaction( em -> {
			Supermarket supermarket = new Supermarket();
			supermarket.setName( "Tesco" );
			em.persist( supermarket );
			return supermarket;
		} );

		statementInspector.clear();
		scope.inTransaction( em -> {
			Supermarket supermarket = em.find( Supermarket.class, shop.getId() );
			supermarket.setName( "Leclerc" );
		} );
		scope.inTransaction( em -> {
			Supermarket supermarket = em.find( Supermarket.class, shop.getId() );
			assertThat( shop.getVersion() ).isNotEqualTo( supermarket.getVersion() );
		} );

		statementInspector.assertHasQueryMatching( "update.*version.*" );
	}
}
