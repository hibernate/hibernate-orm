/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import java.util.Arrays;

import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-20757")
@DomainModel(annotatedClasses = {
		VersionedDirtyAttributeIndexesTest.PlainEntity.class,
		VersionedDirtyAttributeIndexesTest.RootEntity.class,
		VersionedDirtyAttributeIndexesTest.ChildEntity.class
})
@SessionFactory
class VersionedDirtyAttributeIndexesTest {
	@Test
	void versionAttributeWithSnapshot(SessionFactoryScope scope) {
		verifyVersionAttribute( scope, PlainEntity.class );
	}

	@Test
	void inheritedVersionAttributeWithSnapshot(SessionFactoryScope scope) {
		verifyVersionAttribute( scope, ChildEntity.class );
	}

	private void verifyVersionAttribute(SessionFactoryScope scope, Class<?> entityClass) {
		scope.inSession( session -> {
			final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( entityClass );
			final int versionIndex = persister.getVersionPropertyIndex();
			final int nameIndex = Arrays.asList( persister.getPropertyNames() ).indexOf( "name" );
			assertThat( nameIndex ).isNotNegative();
			final Object[] current = new Object[persister.getPropertyNames().length];
			current[versionIndex] = 1L;
			current[nameIndex] = "changed";
			final Object[] previous = current.clone();

			assertThat( persister.resolveDirtyAttributeIndexes(
					current, previous, new String[] { "version" }, session
			) ).isEmpty();
			// Other reported dirty attributes must survive, even when their values compare equal.
			assertThat( persister.resolveDirtyAttributeIndexes(
					current, previous, new String[] { "version", "name" }, session
			) ).containsExactly( nameIndex );

			previous[versionIndex] = 0L;
			assertThat( persister.resolveDirtyAttributeIndexes(
					current, previous, new String[] { "version" }, session
			) ).containsExactly( versionIndex );
			assertThat( persister.resolveDirtyAttributeIndexes(
					current, null, new String[] { "version" }, session
			) ).containsExactly( versionIndex );

			previous[versionIndex] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			assertThat( persister.resolveDirtyAttributeIndexes(
					current, previous, new String[] { "version" }, session
			) ).containsExactly( versionIndex );
			previous[versionIndex] = 1L;
			current[versionIndex] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			assertThat( persister.resolveDirtyAttributeIndexes(
					current, previous, new String[] { "version" }, session
			) ).containsExactly( versionIndex );
			previous[versionIndex] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			assertThat( persister.resolveDirtyAttributeIndexes(
					current, previous, new String[] { "version" }, session
			) ).containsExactly( versionIndex );
		} );
	}

	@Entity(name = "PlainDirtyVersionEntity")
	public static class PlainEntity {
		@Id
		Long id;
		@Version
		Long version;
		String name;
	}

	@Entity(name = "RootDirtyVersionEntity")
	@Inheritance
	public static class RootEntity {
		@Id
		Long id;
		@Version
		Long version;
		String name;
	}

	@Entity(name = "ChildDirtyVersionEntity")
	public static class ChildEntity extends RootEntity {
		String code;
	}
}
