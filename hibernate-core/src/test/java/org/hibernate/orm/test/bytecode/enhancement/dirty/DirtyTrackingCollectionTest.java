/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import jakarta.persistence.CollectionTable;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-11293" )
@DomainModel(
		annotatedClasses = {
				DirtyTrackingCollectionTest.StringsEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class DirtyTrackingCollectionTest {

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
			entity.someStrings.clear();
		} );

		scope.inTransaction( entityManager -> {
			StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
			assertEquals( 0, entity.someStrings.size() );
			entity.someStrings.add( "d" );
		} );

		scope.inTransaction( entityManager -> {
			StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
			assertEquals( 1, entity.someStrings.size() );
			entity.someStrings = new ArrayList<>();
		} );

		scope.inTransaction( entityManager -> {
			StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
			assertEquals( 0, entity.someStrings.size() );
		} );
	}

	// --- //

	@Entity
	@Table( name = "STRINGS_ENTITY" )
	static class StringsEntity {

		@Id
		Long id;

		@ElementCollection
		@CollectionTable(name = "SOME_STRINGS")
		List<String> someStrings;
	}
}
