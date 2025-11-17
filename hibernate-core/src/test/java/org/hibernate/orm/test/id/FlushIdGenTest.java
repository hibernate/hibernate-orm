/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportsIdentityColumns;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@JiraKey(value = "HHH-8611")
@RequiresDialectFeature(feature = SupportsIdentityColumns.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@DomainModel(
		annotatedClasses = {
				RootEntity.class,
				RelatedEntity.class,
		}
)
@SessionFactory
public class FlushIdGenTest {

	@Test
	public void testPersistBeforeTransaction(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					RootEntity ent1_0 = new RootEntity();
					RootEntity ent1_1 = new RootEntity();

					session.persist( ent1_0 );
					session.persist( ent1_1 );

					Transaction tx = session.beginTransaction();
					try {
						tx.commit(); // flush
					}
					catch (Exception e) {
						tx.rollback();
					}
				}
		);

	}
}
