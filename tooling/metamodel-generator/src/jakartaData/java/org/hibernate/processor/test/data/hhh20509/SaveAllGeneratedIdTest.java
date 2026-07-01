/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.hhh20509;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class SaveAllGeneratedIdTest {
	@Test
	@WithClasses({ Item.class, ItemRepository.class })
	void testGeneratedCodeForSaveAllWithGeneratedId() {
		assertMetamodelClassGeneratedFor( Item.class, true );
		assertMetamodelClassGeneratedFor( ItemRepository.class, true );
		final String repository = getMetaModelSourceAsString( ItemRepository.class, true );
		System.out.println( repository );

		// Single save should check the identifier and route to insert for null ids
		assertTrue( repository.contains( "getIdentifier(item)" ),
				"Single save should check the entity identifier" );
		assertTrue( repository.contains( ".insert(item)" ),
				"Single save should route to insert() when id is null" );

		// saveAll must NOT call getIdentifier on the list itself
		assertFalse( repository.contains( "getIdentifier(items)" ),
				"saveAll must not call getIdentifier on the list parameter" );

		// saveAll should not call upsertMultiple without a null-id guard
		// It should either iterate and check each entity, or use an appropriate bulk operation
		assertFalse( repository.contains( "upsertMultiple(items)" ),
				"saveAll must not blindly call upsertMultiple without handling null ids" );
	}

	@Test
	@WithClasses({ Item.class, ReactiveItemRepository.class })
	void testGeneratedCodeForReactiveSaveAllWithGeneratedId() {
		assertMetamodelClassGeneratedFor( ReactiveItemRepository.class, true );
		final String repository = getMetaModelSourceAsString( ReactiveItemRepository.class, true );
		System.out.println( repository );

		// Single save should check the identifier and route to insert for null ids
		assertTrue( repository.contains( "getIdentifier(item)" ),
				"Reactive single save should check the entity identifier" );
		assertTrue( repository.contains( ".insert(item)" ),
				"Reactive single save should route to insert() when id is null" );

		// saveAll must NOT call getIdentifier on the list itself
		assertFalse( repository.contains( "getIdentifier(items)" ),
				"Reactive saveAll must not call getIdentifier on the list parameter" );

		// saveAll should check each entity individually
		assertTrue( repository.contains( "getIdentifier(_entity)" ),
				"Reactive saveAll should check each entity's identifier individually" );
		assertTrue( repository.contains( ".insert(_entity)" ),
				"Reactive saveAll should route to insert() for null ids" );
		assertTrue( repository.contains( ".upsert(_entity)" ),
				"Reactive saveAll should route to upsert() for non-null ids" );
	}
}
