/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.engine.internal.FilteredAssociationState;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { RestrictedToOneRetainedKeysTest.CustomOwner.class,
		RestrictedToOneRetainedKeysTest.LockedOwner.class,
		RestrictedToOneTest.SqlTarget.class, RestrictedToOneTest.FilterTarget.class })
@SessionFactory
@ServiceRegistry(settings = @Setting(name = "hibernate.check_nullability", value = "true"))
class RestrictedToOneRetainedKeysTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void nestedHiddenKeysRemainAvailableForSqlAndLocking(boolean customSql, SessionFactoryScope scope) {
		final Class<? extends Owner> ownerType = customSql ? CustomOwner.class : LockedOwner.class;
		final String table = customSql ? "restricted_custom_embedded" : "restricted_locked_embedded";
		scope.inTransaction( session -> {
			final var sql = new RestrictedToOneTest.SqlTarget();
			sql.id = 2L;
			final var filter = new RestrictedToOneTest.FilterTarget();
			filter.id = 2L;
			session.persist( sql );
			session.persist( filter );
			final Owner owner = customSql ? new CustomOwner() : new LockedOwner();
			owner.id = 1L;
			owner.details = new RestrictedToOneBitmapTest.Details();
			owner.details.links = new RestrictedToOneBitmapTest.Links();
			owner.details.links.sql = sql;
			owner.details.links.filter = filter;
			session.persist( owner );
		} );
		scope.inTransaction( session -> {
			RestrictedToOneTest.enable( session );
			final var owner = session.find( ownerType, 1L );
			final var entry = session.getPersistenceContextInternal().getEntry( owner );
			final var state = entry.getExtraState( FilteredAssociationState.class );
			assertThat( state.retainsKeys() ).isTrue();
			assertThat( owner.details ).isNull();
			owner.details = new RestrictedToOneBitmapTest.Details();
			owner.details.note = "changed";
			owner.name = "changed";
			session.flush();
			assertThat( owner.details.links ).isNull();
			assertThat( state.isEmpty() ).isFalse();
		} );
		scope.inTransaction( session -> assertThat( session.createNativeQuery(
				"select sql_id, filter_id, note from " + table + " where id=1", Object[].class )
				.getSingleResult() ).containsExactly( 2L, 2L, "changed" ) );
	}

	@MappedSuperclass
	static class Owner {
		@Id Long id;
		String name;
		@Embedded RestrictedToOneBitmapTest.Details details;
	}

	@Entity(name = "CustomEmbeddedOwner")
	@Table(name = "restricted_custom_embedded")
	@SQLUpdate(sql = "update restricted_custom_embedded set filter_id=?,sql_id=?,note=?,name=? where id=?")
	static class CustomOwner extends Owner {
	}

	@Entity(name = "LockedEmbeddedOwner")
	@Table(name = "restricted_locked_embedded")
	@OptimisticLocking(type = OptimisticLockType.ALL)
	@DynamicUpdate
	static class LockedOwner extends Owner {
	}
}
