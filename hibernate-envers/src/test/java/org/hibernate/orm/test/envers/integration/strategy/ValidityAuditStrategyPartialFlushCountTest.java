/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.strategy;

import jakarta.persistence.EntityManager;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.engine.internal.StatisticalLoggingSessionEventListener;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.entities.collection.EmbeddableListEntity1;
import org.hibernate.orm.test.envers.entities.components.Component3;
import org.hibernate.orm.test.envers.entities.components.Component4;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the ValidityAuditStrategy does not trigger partial flushes when inserting an entity owning
 * an ElementCollection and when FlushMode is AUTO.
 *
 * @author Vincent Stradiot
 */
@JiraKey("HHH-17442")
public class ValidityAuditStrategyPartialFlushCountTest extends BaseEnversJPAFunctionalTestCase {

	private final AtomicInteger partialFlushEntityCount = new AtomicInteger();
	private final AtomicInteger partialFlushCollectionCount = new AtomicInteger();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {EmbeddableListEntity1.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( EnversSettings.AUDIT_STRATEGY, "org.hibernate.envers.strategy.ValidityAuditStrategy" );
	}

	@Test
	public void testPartialFlushCount() {
		TransactionUtil.doInJPA(this::entityManagerFactory, entityManager -> {

			givenHibernateFlushModeAuto(entityManager);
			recordPartialFlushCount(entityManager);

			final Component3 c3_1 = new Component3(
							"str1_1",
							new Component4("key_1", "value_1", "descr_1"),
							new Component4("key_2", "value_2", "descr_2"));
			final Component3 c3_2 = new Component3(
							"str1_2",
							new Component4("key_3", "value_3", "descr_3"),
							new Component4("key_4", "value_4", "descr_4"));

			final EmbeddableListEntity1 el = new EmbeddableListEntity1();
			el.setOtherData("other_data");
			el.setComponentList(List.of(c3_1, c3_2));

			entityManager.persist(el);
		});

		assertThat(partialFlushEntityCount.get()).isZero();
		assertThat(partialFlushCollectionCount.get()).isZero();
	}

	private void givenHibernateFlushModeAuto(final EntityManager entityManager) {
		entityManager.unwrap(Session.class).setHibernateFlushMode(FlushMode.AUTO);
	}

	private void recordPartialFlushCount(final EntityManager entityManager) {
		entityManager.unwrap(SessionImplementor.class).getEventListenerManager().addListener(new StatisticalLoggingSessionEventListener() {

			@Override
			public void partialFlushEnd(final int numberOfEntities, final int numberOfCollections) {
				super.partialFlushEnd(numberOfEntities, numberOfCollections);
				partialFlushEntityCount.getAndAdd(numberOfEntities);
				partialFlushCollectionCount.getAndAdd(numberOfCollections);
			}

		} );
	}
}
