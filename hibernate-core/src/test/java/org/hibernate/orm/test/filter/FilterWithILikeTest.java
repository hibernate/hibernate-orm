/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@DomainModel(
		annotatedClasses = {
				FilterWithILikeTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16464")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCaseInsensitiveLike.class)
public class FilterWithILikeTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity filtered = new TestEntity( 1, "filtered" );
					session.persist( filtered );

					TestEntity notFiltered = new TestEntity( 2, "not_filtered" );
					session.persist( notFiltered );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					session.enableFilter( "nameFilter" ).setParameter( "name", "not_filtered" );
					session.createQuery( "from TestEntity " ).list();
				}
		);
	}

	@FilterDef(name = "nameFilter", parameters = {
			@ParamDef(name = "name", type = String.class)
	})
	@Filter(name = "nameFilter", condition = "NAME ilike :name")
	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		@Column(name = "NAME")
		private String name;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
