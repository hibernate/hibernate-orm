/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4.packagelevel;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				PackageLevelEntityListenerTests.PackageEntity.class,
				PackageLevelEntityListenerTests.ExcludingDefaultListenersEntity.class
		},
		annotatedPackageNames = "org.hibernate.orm.test.jpa.callbacks.jpa4.packagelevel"
)
@SessionFactory
public class PackageLevelEntityListenerTests {
	@BeforeEach
	void setUp() {
		Events.reset();
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void packageLevelEntityListenersAreAppliedBeforeEntityLevelListenersAndCallbacks(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new PackageEntity( 1 ) ) );

		assertThat( Events.names ).containsExactly(
				"package:PackageEntity",
				"entity-listener:PackageEntity",
				"entity-callback:PackageEntity"
		);
	}

	@Test
	void excludeDefaultListenersDoesNotExcludePackageLevelEntityListeners(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new ExcludingDefaultListenersEntity( 1 ) ) );

		assertThat( Events.names ).containsExactly(
				"package:ExcludingDefaultListenersEntity",
				"entity-callback:ExcludingDefaultListenersEntity"
		);
	}

	public static class Events {
		static final List<String> names = new ArrayList<>();

		static void reset() {
			names.clear();
		}
	}

	@Entity(name = "PackageLevelPackageEntity")
	@EntityListeners( EntityLevelListener.class )
	public static class PackageEntity {
		@Id
		private Integer id;

		public PackageEntity() {
		}

		public PackageEntity(Integer id) {
			this.id = id;
		}

		@PrePersist
		void prePersist() {
			Events.names.add( "entity-callback:PackageEntity" );
		}
	}

	@Entity(name = "PackageLevelExcludingDefaultListenersEntity")
	@ExcludeDefaultListeners
	public static class ExcludingDefaultListenersEntity {
		@Id
		private Integer id;

		public ExcludingDefaultListenersEntity() {
		}

		public ExcludingDefaultListenersEntity(Integer id) {
			this.id = id;
		}

		@PrePersist
		void prePersist() {
			Events.names.add( "entity-callback:ExcludingDefaultListenersEntity" );
		}
	}

	public static class EntityLevelListener {
		@PrePersist
		void prePersist(PackageEntity entity) {
			Events.names.add( "entity-listener:" + entity.getClass().getSimpleName() );
		}
	}
}
