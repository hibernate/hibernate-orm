/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
				FilterWithILikeTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16464")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCaseInsensitiveLike.class)
public class FilterWithILikeTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
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
					//noinspection deprecation
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
