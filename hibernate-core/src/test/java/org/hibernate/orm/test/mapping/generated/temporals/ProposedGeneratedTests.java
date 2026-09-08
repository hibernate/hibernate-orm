/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.temporals;

import java.time.Instant;

import org.hibernate.generator.EventType;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.DialectContext.awaitServerTimestampTick;

/**
 * Tests for {@link ProposedGenerated}, a proposed update to {@link org.hibernate.annotations.Generated}
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = ProposedGeneratedTests.GeneratedInstantEntity.class )
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.CurrentTimestampHasMicrosecondPrecision.class, comment = "Without this, we might not see an update to the timestamp")
@RequiresDialectFeature( feature = DialectFeatureChecks.UsesStandardCurrentTimestampFunction.class )
public class ProposedGeneratedTests {
	@Test
	public void test(SessionFactoryScope scope) throws InterruptedException {
		final GeneratedInstantEntity created = scope.fromTransaction( (session) -> {
			final GeneratedInstantEntity entity = new GeneratedInstantEntity( 1, "tsifr" );
			session.persist( entity );
			return entity;
		} );

		assertThat( created.createdAt ).isNotNull();
		assertThat( created.updatedAt ).isNotNull();
		assertThat( created.createdAt ).isEqualTo( created.updatedAt );

		created.name = "first";

		//We need to wait a little to make sure the timestamps produced are different
		awaitServerTimestampTick( scope );

		// then changing
		final GeneratedInstantEntity merged = scope.fromTransaction( (session) -> session.merge( created ) );

		assertThat( merged ).isNotNull();
		assertThat( merged.createdAt ).isNotNull();
		assertThat( merged.updatedAt ).isNotNull();
		assertThat( merged.createdAt ).isEqualTo( created.createdAt );
		assertThat( merged.updatedAt ).isNotEqualTo( created.updatedAt );

		assertThat( merged ).isNotNull();

		//We need to wait a little to make sure the timestamps produced are different
		awaitServerTimestampTick( scope );

		// lastly, make sure we can load it..
		final GeneratedInstantEntity loaded = scope.fromTransaction( (session) -> session.get( GeneratedInstantEntity.class, 1 ) );

		assertThat( loaded ).isNotNull();

		assertThat( loaded.createdAt ).isEqualTo( merged.createdAt );
		assertThat( loaded.updatedAt ).isEqualTo( merged.updatedAt );
	}

	@Entity( name = "GeneratedInstantEntity" )
	@Table( name = "gen_ann_instant" )
	public static class GeneratedInstantEntity {
		@Id
		public Integer id;
		public String name;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Proposed update to the legacy `Generated` annotation

		@ProposedGenerated( timing = EventType.INSERT, sqlDefaultValue = "current_timestamp" )
		public Instant createdAt;
		@ProposedGenerated( timing = {EventType.INSERT, EventType.UPDATE}, sqlDefaultValue = "current_timestamp" )
		public Instant updatedAt;

		public GeneratedInstantEntity() {
		}

		public GeneratedInstantEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
