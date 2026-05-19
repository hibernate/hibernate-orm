/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.constraint;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class DataTest {
	@Test
	@WithClasses({MyEntity.class, MyConstrainedRepository.class})
	void test() {
		System.out.println( getMetaModelSourceAsString( MyEntity.class ) );
		final String repository = getMetaModelSourceAsString( MyConstrainedRepository.class );
		System.out.println( repository );
		assertMetamodelClassGeneratedFor( MyEntity.class );
		assertMetamodelClassGeneratedFor( MyConstrainedRepository.class );

		assertTrue( repository.contains( "_builder.like(_entity.get(MyEntity_.name), name)" ) );
		assertTrue( repository.contains( "_builder.notLike(_entity.get(MyEntity_.name), name)" ) );
		assertTrue( repository.contains( "_builder.notEqual(_entity.get(MyEntity_.name), name)" ) );
		assertTrue( repository.contains( "_entity.get(MyEntity_.name).in(name)" ) );
		assertTrue( repository.contains( "_builder.not(_entity.get(MyEntity_.name).in((Object[]) name))" ) );
		assertTrue( repository.contains(
				"JakartaDataRestriction.applyConstraint(_entity.get(MyEntity_.name), name, _entity, _builder)" ) );
		assertTrue( repository.contains( "Constraint<? super String> name" ) );
		assertTrue( repository.contains( "Like name" ) );
		assertTrue( repository.contains( "_builder.greaterThan(_entity.get(MyEntity_.age), age)" ) );
		assertTrue( repository.contains( "_builder.lessThan(_entity.get(MyEntity_.age), age)" ) );
		assertTrue( repository.contains( "_builder.greaterThanOrEqualTo(_entity.get(MyEntity_.age), age)" ) );
		assertTrue( repository.contains( "_builder.lessThanOrEqualTo(_entity.get(MyEntity_.age), age2)" ) );
	}
}
