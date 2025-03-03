/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.time.Instant;

import org.hibernate.HibernateError;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.generator.EventType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = GeneratedAnnotationTests.AuditedEntity.class )
@SessionFactory
@RequiresDialect(value = PostgreSQLDialect.class, comment = "To write a trigger only once")
public class GeneratedAnnotationTests {

	private static final String TRIGGER = "begin NEW.lastUpdatedAt = current_timestamp; return NEW; end;";

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.createNativeMutationQuery( "create function update_ts_func() returns trigger language plpgsql as $$ " + TRIGGER + " $$" )
							.executeUpdate();
					s.createNativeMutationQuery( "create trigger update_ts before update on gen_ann_baseline for each row execute procedure update_ts_func()" )
							.executeUpdate();
				}
		);
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					s.createNativeMutationQuery( "drop trigger if exists update_ts on gen_ann_baseline" )
							.executeUpdate();
					s.createNativeMutationQuery( "drop function if exists update_ts_func()" )
							.executeUpdate();
				}
		);
	}

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

		created.name = "changed";

		//We need to wait a little to make sure the timestamps produced are different
		waitALittle();

		// then changing
		final AuditedEntity merged = scope.fromTransaction( (session) -> {
			return (AuditedEntity) session.merge( created );
		} );

		assertThat( merged ).isNotNull();
		assertThat( merged.createdAt ).isNotNull();
		assertThat( merged.lastUpdatedAt ).isNotNull();
		assertThat( merged.lastUpdatedAt ).isNotEqualTo( merged.createdAt );

		//We need to wait a little to make sure the timestamps produced are different
		waitALittle();

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
		@Generated
		@ColumnDefault( "current_timestamp" )
		public Instant createdAt;
		@Generated( event = { EventType.INSERT, EventType.UPDATE } )
		@ColumnDefault( "current_timestamp" )
		public Instant lastUpdatedAt;

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
