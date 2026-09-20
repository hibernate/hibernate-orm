/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.write;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = SecondaryTableDynamicUpdateVersionTest.Thing.class)
@SessionFactory
class SecondaryTableDynamicUpdateVersionTest {

	@BeforeEach
	void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var thing = new Thing();
			thing.id = 1L;
			thing.primaryText = "original";
			thing.secondaryText = "original";
			thing.unversionedText = "original";
			session.persist( thing );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void secondaryTableUpdatePersistsVersionIncrement(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var thing = session.find( Thing.class, 1L );
			thing.secondaryText = "changed";
			session.flush();
			assertThat( thing.version ).isEqualTo( 1 );
		} );

		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery(
					"select version from dynamic_version_main where id = 1", Integer.class )
					.getSingleResult() ).isEqualTo( 1 );
			assertThat( session.find( Thing.class, 1L ).secondaryText ).isEqualTo( "changed" );
		} );
	}

	@Test
	void excludedSecondaryTableUpdateDoesNotIncrementVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.find( Thing.class, 1L ).unversionedText = "changed" );

		scope.inTransaction( session -> {
			final var thing = session.find( Thing.class, 1L );
			assertThat( thing.version ).isZero();
			assertThat( thing.unversionedText ).isEqualTo( "changed" );
		} );
	}

	@ParameterizedTest(name = "winner on secondary table: {0}, stale writer on secondary table: {1}")
	@CsvSource({ "true, true", "true, false", "false, true", "false, false" })
	void staleUpdateIsRejected(boolean winnerOnSecondaryTable, boolean staleWriterOnSecondaryTable,
			SessionFactoryScope scope) {
		scope.inSession( staleSession -> {
			final var stale = scope.fromTransaction( staleSession, session -> session.find( Thing.class, 1L ) );
			assertThat( stale.version ).isZero();

			// Commit the competing change while keeping the first session's managed snapshot.
			scope.inTransaction( session -> change( session.find( Thing.class, 1L ), winnerOnSecondaryTable, "winner" ) );

			assertThatThrownBy( () -> scope.inTransaction( staleSession, session -> {
				change( stale, staleWriterOnSecondaryTable, "stale" );
				session.flush();
			} ) ).isInstanceOf( OptimisticLockException.class );
		} );

		scope.inTransaction( session -> {
			final var thing = session.find( Thing.class, 1L );
			assertThat( thing.version ).isEqualTo( 1 );
			assertThat( thing.primaryText ).isEqualTo( winnerOnSecondaryTable ? "original" : "winner" );
			assertThat( thing.secondaryText ).isEqualTo( winnerOnSecondaryTable ? "winner" : "original" );
		} );
	}

	private static void change(Thing thing, boolean secondaryTable, String value) {
		if ( secondaryTable ) {
			thing.secondaryText = value;
		}
		else {
			thing.primaryText = value;
		}
	}

	@Entity(name = "DynamicVersionThing")
	@Table(name = "dynamic_version_main")
	@SecondaryTable(name = "dynamic_version_extra")
	@DynamicUpdate
	static class Thing {
		@Id
		Long id;
		@Version
		int version;
		String primaryText;
		@Column(table = "dynamic_version_extra")
		String secondaryText;
		@Column(table = "dynamic_version_extra")
		@OptimisticLock(excluded = true)
		String unversionedText;
	}
}
