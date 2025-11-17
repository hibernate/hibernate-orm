/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Same as {@link DirtyTrackingCollectionInDefaultFetchGroupTest},
 * but with {@code collectionInDefaultFetchGroup} set to {@code false} explicitly.
 * <p>
 * Kept here for <a href="https://github.com/hibernate/hibernate-orm/pull/5252#pullrequestreview-1095843220">historical reasons</a>.
 *
 * @author Christian Beikov
 */
@JiraKey( "HHH-14348" )
@DomainModel(
		annotatedClasses = {
				DirtyTrackingCollectionInDefaultFetchGroupFalseTest.StringsEntity.class
		}
)
@SessionFactory(
		// We want to test with this setting set to false explicitly,
		// because another test already takes care of the default.
		applyCollectionsInDefaultFetchGroup = false
)
@BytecodeEnhanced
public class DirtyTrackingCollectionInDefaultFetchGroupFalseTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			StringsEntity entity = new StringsEntity();
			entity.id = 1L;
			entity.someStrings = new ArrayList<>( Arrays.asList( "a", "b", "c" ) );
			em.persist( entity );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
			entityManager.flush();
			BytecodeLazyAttributeInterceptor interceptor = (BytecodeLazyAttributeInterceptor) ( (PersistentAttributeInterceptable) entity )
					.$$_hibernate_getInterceptor();
			assertTrue( interceptor.hasAnyUninitializedAttributes() );
			assertFalse( interceptor.isAttributeLoaded( "someStrings" ) );
			assertFalse( interceptor.isAttributeLoaded( "someStringEntities" ) );
		} );
	}

	// --- //

	@Entity
	@Table(name = "STRINGS_ENTITY")
	static class StringsEntity {

		@Id
		Long id;

		@ElementCollection
		@CollectionTable(name = "STRINGS_ENTITY_SOME", joinColumns = @JoinColumn(name = "SOME_ID"))
		List<String> someStrings;

		@ManyToOne(fetch = FetchType.LAZY)
		StringsEntity parent;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
		Set<StringsEntity> someStringEntities;
	}
}
