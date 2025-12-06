/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.discriminator;

import org.hibernate.bugs.entity.MultitenantChildEntity;
import org.hibernate.bugs.entity.MultitenantParentEntity;
import org.hibernate.bugs.entity.MultitenantReferenceEntity;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;


@DomainModel(
		annotatedClasses = {
				MultitenantChildEntity.class,
				MultitenantParentEntity.class,
				MultitenantReferenceEntity.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
				@Setting(name = AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
						value = "org.hibernate.orm.test.multitenancy.discriminator.resolver.TenantResolver"),
		}
)
@SessionFactory
class MultitenancyJoinedStrategyQueryTest {

	@Test
	void hhh123Test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<MultitenantReferenceEntity> results = session.createSelectionQuery(
							"from MultitenantReferenceEntity r where r.child.number = :number",
							MultitenantReferenceEntity.class )
					.setParameter( "number", 7 )
					.getResultList();
			// no exception is thrown
		} );
	}
}
