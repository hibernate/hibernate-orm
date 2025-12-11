/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = ImmutableDirtinessCheckingTests.ReferenceData.class)
@SessionFactory
public class ImmutableDirtinessCheckingTests {
	@Test
	void testDirtyCheckingBehavior(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var data = session.find( ReferenceData.class, 1 );
			data.setName( "another value" );
			nameAccessCount = 0;
			session.flush();
			assertThat( nameAccessCount ).isEqualTo( 0 );
		} );

		factoryScope.inTransaction( (session) -> {
			var data = session.find( ReferenceData.class, 1 );
			assertThat( data.getName() ).isEqualTo( "initial value" );
		} );
	}

	@BeforeEach
	void prepareTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.persist( new ReferenceData( 1, "initial value" ) );
		} );
	}

	@Test
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	private static int nameAccessCount = 0;

	@Entity(name="ReferenceData")
	@Table(name="ReferenceData")
	@Immutable
	public static class ReferenceData {
		private Integer id;
		private String name;

		public ReferenceData() {
		}

		public ReferenceData(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			nameAccessCount++;
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
