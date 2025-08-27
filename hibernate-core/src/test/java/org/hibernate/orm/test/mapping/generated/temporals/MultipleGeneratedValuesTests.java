/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.temporals;

import java.time.Instant;

import org.hibernate.HibernateError;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.generator.EventType;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for defining multiple generated values per entity
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = MultipleGeneratedValuesTests.GeneratedInstantEntity.class )
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.CurrentTimestampHasMicrosecondPrecision.class, comment = "Without this, we might not see an update to the timestamp")
@RequiresDialectFeature( feature = DialectFeatureChecks.UsesStandardCurrentTimestampFunction.class )
@SkipForDialect( dialectClass = SQLServerDialect.class, reason = "CURRENT_TIMESTAMP has millisecond precision" )
public class MultipleGeneratedValuesTests {
	@Test
	public void test(SessionFactoryScope scope) {
		final GeneratedInstantEntity created = scope.fromTransaction( (session) -> {
			final GeneratedInstantEntity entity = new GeneratedInstantEntity( 1, "tsifr" );
			session.persist( entity );
			return entity;
		} );

		assertThat( created.createdAt ).isNotNull();
		assertThat( created.updatedAt ).isNotNull();
		assertThat( created.createdAt ).isEqualTo( created.updatedAt );

		assertThat( created.createdAt2 ).isNotNull();
		assertThat( created.updatedAt2 ).isNotNull();
		assertThat( created.createdAt2 ).isEqualTo( created.updatedAt2 );

		created.name = "first";

		//We need to wait a little to make sure the timestamps produced are different
		waitALittle();

		// then changing
		final GeneratedInstantEntity merged = scope.fromTransaction( (session) -> {
			return (GeneratedInstantEntity) session.merge( created );
		} );

		assertThat( merged ).isNotNull();
		assertThat( merged.createdAt ).isNotNull();
		assertThat( merged.updatedAt ).isNotNull();
		assertThat( merged.createdAt ).isEqualTo( created.createdAt );
		assertThat( merged.updatedAt ).isNotEqualTo( created.updatedAt );

		assertThat( merged ).isNotNull();
		assertThat( merged.createdAt2 ).isNotNull();
		assertThat( merged.updatedAt2 ).isNotNull();
		assertThat( merged.createdAt2 ).isEqualTo( created.createdAt2 );
		assertThat( merged.updatedAt2 ).isNotEqualTo( created.updatedAt2 );

		//We need to wait a little to make sure the timestamps produced are different
		waitALittle();

		// lastly, make sure we can load it..
		final GeneratedInstantEntity loaded = scope.fromTransaction( (session) -> {
			return session.get( GeneratedInstantEntity.class, 1 );
		} );

		assertThat( loaded ).isNotNull();

		assertThat( loaded.createdAt ).isEqualTo( merged.createdAt );
		assertThat( loaded.updatedAt ).isEqualTo( merged.updatedAt );

		assertThat( loaded.createdAt2 ).isEqualTo( merged.createdAt2 );
		assertThat( loaded.updatedAt2 ).isEqualTo( merged.updatedAt2 );
	}

	@Entity( name = "GeneratedInstantEntity" )
	@Table( name = "gen_ann_instant" )
	public static class GeneratedInstantEntity {
		@Id
		public Integer id;
		public String name;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Legacy `Generated`

		@CurrentTimestamp(event = EventType.INSERT)
		public Instant createdAt;

		@CurrentTimestamp
		public Instant updatedAt;

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// `GeneratedValue`

		@ProposedGenerated( timing = EventType.INSERT, sqlDefaultValue = "current_timestamp" )
		public Instant createdAt2;
		@ProposedGenerated( timing = {EventType.INSERT,EventType.UPDATE}, sqlDefaultValue = "current_timestamp" )
		public Instant updatedAt2;

		public GeneratedInstantEntity() {
		}

		public GeneratedInstantEntity(Integer id, String name) {
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
