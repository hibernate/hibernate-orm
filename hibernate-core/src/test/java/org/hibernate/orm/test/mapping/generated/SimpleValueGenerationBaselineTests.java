/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.sql.Time;
import java.sql.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SimpleValueGenerationBaselineTests.NonAuditedEntity.class )
@SessionFactory
public class SimpleValueGenerationBaselineTests {
	@Test
	public void testLoading(SessionFactoryScope scope) {
		// some of the generated-value tests show problems loading entities with attributes of
		// java.sql.Date type.  Make sure we can load such an entity without generation involved
		final NonAuditedEntity saved = scope.fromTransaction( (session) -> {
			final NonAuditedEntity entity = new NonAuditedEntity( 1 );
			session.persist( entity );
			return entity;
		} );

		// lastly, make sure we can load it..
		scope.inTransaction( (session) -> {
			assertThat( session.get( NonAuditedEntity.class, 1 ) ).isNotNull();
		} );
	}


	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "NonAuditedEntity" )
	@Table( name = "ann_generated_complex_base" )
	public static class NonAuditedEntity {
		@Id
		private Integer id;
		private String name;
		private String lastName;

		private java.sql.Date vmCreatedSqlDate;
		private Time vmCreatedSqlTime;
		private Timestamp vmCreatedSqlTimestamp;

		private NonAuditedEntity() {
		}

		private NonAuditedEntity(Integer id) {
			this.id = id;

			name = "it";

			vmCreatedSqlDate = new java.sql.Date( System.currentTimeMillis() );
			vmCreatedSqlTime = new Time( System.currentTimeMillis() );
			vmCreatedSqlTimestamp = new Timestamp( System.currentTimeMillis() );
		}
	}
}
