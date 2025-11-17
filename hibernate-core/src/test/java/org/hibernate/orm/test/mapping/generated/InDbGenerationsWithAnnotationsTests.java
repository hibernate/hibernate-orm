/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.sql.Timestamp;

import org.hibernate.HibernateError;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for using {@link CreationTimestamp} and {@link UpdateTimestamp}
 * annotations with Timestamp-valued attributes
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = InDbGenerationsWithAnnotationsTests.AuditedEntity.class )
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.CurrentTimestampHasMicrosecondPrecision.class,
		comment = "Without this, we might not see an update to the timestamp")
public class InDbGenerationsWithAnnotationsTests {
	@Test
	public void testGenerations(SessionFactoryScope scope) {
		scope.inSession( (session) -> {
			// first creation
			final AuditedEntity saved = scope.fromTransaction( session, (s) -> {
				final AuditedEntity entity = new AuditedEntity( 1, "it" );
				session.persist( entity );
				return entity;
			} );

			assertThat( saved ).isNotNull();
			assertThat( saved.createdOn ).isNotNull();
			assertThat( saved.lastUpdatedOn ).isNotNull();

			saved.name = "changed";

			//We need to wait a little to make sure the timestamps produced are different
			waitALittle();

			// then changing
			final AuditedEntity merged = scope.fromTransaction( session, s -> s.merge( saved ) );

			assertThat( merged ).isNotNull();
			assertThat( merged.createdOn ).isNotNull();
			assertThat( merged.lastUpdatedOn ).isNotNull();
			assertThat( merged.lastUpdatedOn ).isNotEqualTo( merged.createdOn );

			//We need to wait a little to make sure the timestamps produced are different
			waitALittle();

			// lastly, make sure we can load it
			final AuditedEntity loaded = scope.fromTransaction( session, s -> s.get( AuditedEntity.class, 1 ) );

			assertThat( loaded ).isNotNull();
			assertThat( loaded.createdOn ).isEqualTo( merged.createdOn );
			assertThat( loaded.lastUpdatedOn ).isEqualTo( merged.lastUpdatedOn );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity( name = "AuditedEntity" )
	@Table( name = "ann_generated_simple" )
	public static class AuditedEntity {
		@Id
		public Integer id;
		@Basic
		public String name;

		@CurrentTimestamp(event = EventType.INSERT)
		public Timestamp createdOn;
		@CurrentTimestamp
		public Timestamp lastUpdatedOn;

		public AuditedEntity() {
		}

		public AuditedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	private static void waitALittle() {
		try {
			Thread.sleep( 10 );
		}
		catch (InterruptedException e) {
			throw new HibernateError( "Unexpected wakeup from test sleep" );
		}
	}
}
