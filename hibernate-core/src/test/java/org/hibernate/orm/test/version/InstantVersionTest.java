/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-10026" )
@DomainModel(annotatedClasses = InstantVersionTest.TheEntity.class)
@SessionFactory
public class InstantVersionTest {

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testInstantUsageAsVersion(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var e = new TheEntity( 1 );
			session.persist( e );
		} );

		factoryScope.inTransaction( (session) -> {
			var e = session.find( TheEntity.class, 1 );
			assertThat( e.getTs() ).isNotNull();
		} );
	}


	@Entity(name = "TheEntity")
	@Table(name="the_entity")
	public static class TheEntity {
		private Integer id;
		private Instant ts;

		public TheEntity() {
		}

		public TheEntity(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Version
		public Instant getTs() {
			return ts;
		}

		public void setTs(Instant ts) {
			this.ts = ts;
		}
	}
}
