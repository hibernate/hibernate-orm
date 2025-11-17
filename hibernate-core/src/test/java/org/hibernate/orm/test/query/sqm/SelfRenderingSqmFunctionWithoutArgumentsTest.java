/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportPartitionBy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = SelfRenderingSqmFunctionWithoutArgumentsTest.Dummy.class
)
@SessionFactory
@JiraKey("HHH-19719")
@RequiresDialectFeature(feature = SupportPartitionBy.class)
public class SelfRenderingSqmFunctionWithoutArgumentsTest {

	@BeforeAll
	static void init(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Dummy(1, "John Doe") );
			session.persist( new Dummy(2, "Dave Default") );
		} );
	}

	@Test
	void test(SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.createQuery( """
					with tmp as (
						select id id, name name, row_number() over (order by name) pos
						from Dummy
					)
					select id, name, pos from tmp
					""" ).getResultList();
		} );

	}

	@Entity(name = "Dummy")
	static class Dummy {
		@Id
		private Integer id;

		private String name;

		private Dummy() {
			// for use by Hibernate
		}

		public Dummy(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
