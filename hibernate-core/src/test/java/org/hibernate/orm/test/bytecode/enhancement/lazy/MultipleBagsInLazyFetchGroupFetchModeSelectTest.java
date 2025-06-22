/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				MultipleBagsInLazyFetchGroupFetchModeSelectTest.StringsEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class MultipleBagsInLazyFetchGroupFetchModeSelectTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		assertTrue( scope.getSessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );

		scope.inTransaction( em -> {
			StringsEntity entity = new StringsEntity();
			entity.id = 1L;
			entity.text = "abc";
			entity.someStrings = new ArrayList<>( Arrays.asList( "a", "b", "c" ) );
			entity.someStrings2 = new ArrayList<>( Arrays.asList( "a", "b", "c", "d" ) );
			em.persist( entity );
		} );
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		Assertions.assertTrue( scope.getSessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );
		scope.inTransaction( entityManager -> {
			StringsEntity entity = entityManager.getReference( StringsEntity.class, 1L );
			assertEquals( 3, entity.someStrings.size() );
			assertEquals( 4, entity.someStrings2.size() );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		Assertions.assertTrue( scope.getSessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );
		scope.inTransaction( entityManager -> {
			StringsEntity entity = entityManager.find( StringsEntity.class, 1L );
			assertEquals( 3, entity.someStrings.size() );
			assertEquals( 4, entity.someStrings2.size() );

		} );
	}

	// --- //

	@Entity(name = "StringsEntity")
	@Table(name = "STRINGS_ENTITY")
	static class StringsEntity {

		@Id
		Long id;

		String text;

		@ElementCollection(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		List<String> someStrings;

		@ElementCollection(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		List<String> someStrings2;
	}
}
