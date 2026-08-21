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


@JiraKey(value = "HHH-19495")
@Jpa(
		xmlMappings = {"org/hibernate/orm/test/jpa/xml/NoDefaultOptimisticLockAnnotationTest.xml"},
		annotatedClasses = {Consumer.class, ConsumerItem.class},
		useCollectingStatementInspector = true
)
class NoDefaultOptimisticLockAnnotationTest {

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

		statementInspector.assertIsInsert( 1 );
		statementInspector.assertNoUpdate();
	}
}
