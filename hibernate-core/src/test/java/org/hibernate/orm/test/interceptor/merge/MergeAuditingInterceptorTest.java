/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor.merge;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.cfg.SessionEventSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = MergeAuditingInterceptorTest.Thing.class,
		integrationSettings = @Setting(name = SessionEventSettings.INTERCEPTOR,
				value = "org.hibernate.orm.test.interceptor.merge.MergeAuditingInterceptor"))
class MergeAuditingInterceptorTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		Thing t = scope.fromTransaction( em -> {
			Thing thing = new Thing();
			thing.name = "Hibernate";
			em.persist( thing );
			return thing;
		} );
		scope.inTransaction( em -> {
			t.name = "Hibernate ORM";
			Thing thing = em.merge( t );
			assertEquals( 1, MergeAuditingInterceptor.auditTrail.size() );
			assertEquals( "name changed from Hibernate to Hibernate ORM for " + t.id,
					MergeAuditingInterceptor.auditTrail.get( 0 ) );
		} );
	}

	@Entity
	static class Thing {
		@Id
		@GeneratedValue
		private Long id;
		private String name;
	}
}
