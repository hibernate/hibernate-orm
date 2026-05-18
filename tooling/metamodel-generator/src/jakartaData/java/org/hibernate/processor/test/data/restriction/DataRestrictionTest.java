/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.restriction;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class DataRestrictionTest {
	@Test
	@WithClasses({
			DataRestrictionBook.class,
			DataRestrictionPublisher.class,
			DataRestrictionRepository.class
	})
	void generatedRepositoryAcceptsJakartaDataRestrictions() {
		final String repository = getMetaModelSourceAsString( DataRestrictionRepository.class );
		final String metamodel = getMetaModelSourceAsString( DataRestrictionBook.class, true );
		System.out.println( repository );
		System.out.println( metamodel );

		assertTrue( repository.contains( "Restriction<? super DataRestrictionBook> restriction" ) );
		assertTrue( repository.contains( "List<Restriction<? super DataRestrictionBook>> restrictions" ) );
		assertTrue( repository.contains( "Restriction<? super DataRestrictionBook>[] restrictions" ) );
		assertTrue( repository.contains( "_spec.restrict(JakartaDataRestriction.from(restriction));" ) );
		assertTrue( repository.contains( "_spec.restrict(JakartaDataRestriction.all(restrictions));" ) );
		assertTrue( repository.contains( "_spec.restrict(JakartaDataRestriction.from(queryRestriction));" ) );
		assertTrue( repository.contains( "_spec.restrict(JakartaDataRestriction.from(deleteRestriction));" ) );
		assertTrue( repository.contains( "for (var _sort : order.sorts())" ) );
		assertFalse( repository.contains( "DataRestrictionBook_.restriction" ) );
		assertFalse( repository.contains( "DataRestrictionBook_.queryRestriction" ) );
		assertFalse( repository.contains( "DataRestrictionBook_.deleteRestriction" ) );

		assertTrue( metamodel.contains( "TextAttribute.of(DataRestrictionBook.class, TITLE)" ) );
		assertTrue( metamodel.contains( "NumericAttribute.of(DataRestrictionBook.class, PAGES, int.class)" ) );
		assertTrue( metamodel.contains(
				"NavigableAttribute.of(DataRestrictionBook.class, PUBLISHER, DataRestrictionPublisher.class)" ) );
	}
}
