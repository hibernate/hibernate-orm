/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.orm.test.mapping.generated.temporals.CurrentTimestamp;
import org.hibernate.tuple.GenerationTiming;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = GeneratedValueAnnotationTests.AuditedEntity.class )
@SessionFactory
public class GeneratedValueAnnotationTests {
	@Test
	public void test(SessionFactoryScope scope) {
		final AuditedEntity created = scope.fromTransaction( (session) -> {
			final AuditedEntity entity = new AuditedEntity( 1, "tsifr" );
			session.persist( entity );
			return entity;
		} );

		assertThat( created.createdAt ).isNotNull();
		assertThat( created.lastUpdatedAt ).isNotNull();
		assertThat( created.lastUpdatedAt ).isEqualTo(created.createdAt );

		created.name = "first";

		// then changing
		final AuditedEntity merged = scope.fromTransaction( (session) -> {
			return (AuditedEntity) session.merge( created );
		} );

		assertThat( merged ).isNotNull();
		assertThat( merged.createdAt ).isNotNull();
		assertThat( merged.createdAt ).isEqualTo( created.createdAt );

		assertThat( merged.lastUpdatedAt ).isNotNull();
		assertThat( merged.lastUpdatedAt ).isNotEqualTo( merged.createdAt );
		assertThat( merged.lastUpdatedAt ).isNotEqualTo( created.createdAt );

		// lastly, make sure we can load it..
		final AuditedEntity loaded = scope.fromTransaction( (session) -> {
			return session.get( AuditedEntity.class, 1 );
		} );

		assertThat( loaded ).isNotNull();
		assertThat( loaded.createdAt ).isEqualTo( merged.createdAt );
		assertThat( loaded.lastUpdatedAt ).isEqualTo( merged.lastUpdatedAt );
	}

	@Entity( name = "gen_ann_baseline" )
	@Table( name = "" )
	public static class AuditedEntity {
		@Id
		public Integer id;
		public String name;

		@CurrentTimestamp( timing = GenerationTiming.INSERT )
		public Instant createdAt;

		@CurrentTimestamp( timing = GenerationTiming.ALWAYS )
		public Instant lastUpdatedAt;

		public AuditedEntity() {
		}

		public AuditedEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
