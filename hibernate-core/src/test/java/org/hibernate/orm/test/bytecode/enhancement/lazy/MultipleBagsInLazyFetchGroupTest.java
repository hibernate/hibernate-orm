/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
			MultipleBagsInLazyFetchGroupTest.StringsEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class MultipleBagsInLazyFetchGroupTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		assertTrue( scope.getSessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );

		scope.inTransaction( em -> {
			StringsEntity entity = new StringsEntity();
			entity.id = 1L;
			entity.text = "abc";
			entity.someStrings = new ArrayList<>( Arrays.asList( "a", "b", "c" ) );
			entity.someStrings2 = new ArrayList<>( Arrays.asList( "a", "b", "c" ) );
			em.persist( entity );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		Assertions.assertTrue( scope.getSessionFactory().getSessionFactoryOptions().isCollectionsInDefaultFetchGroupEnabled() );
		scope.inTransaction( entityManager -> {
			StringsEntity entity = entityManager.getReference( StringsEntity.class, 1L );
			assertEquals( 3, entity.someStrings.size() );
			assertEquals( 3, entity.someStrings2.size() );
		} );
	}

	// --- //

	@Entity
	@Table( name = "STRINGS_ENTITY" )
	static class StringsEntity {

		@Id
		Long id;

		String text;

		@CollectionTable(name = "SOME_STRINGS")
		@ElementCollection(fetch = FetchType.EAGER)
		List<String> someStrings;

		@CollectionTable(name = "SOME_STRINGS_TWO")
		@ElementCollection(fetch = FetchType.EAGER)
		List<String> someStrings2;
	}
}
