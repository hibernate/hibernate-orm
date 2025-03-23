/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import org.hibernate.FlushMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@JiraKey(value = "HHH-12464")
@DomainModel(
		annotatedClasses = {
				RootEntity.class,
				RelatedEntity.class,
		}
)
@SessionFactory
public class CreateDeleteTest {

	@Test
	public void createAndDeleteAnEntityInTheSameTransactionTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.setHibernateFlushMode( FlushMode.COMMIT );
					RootEntity entity = new RootEntity();
					session.persist( entity );
					session.remove( entity );
				} );
	}

}
