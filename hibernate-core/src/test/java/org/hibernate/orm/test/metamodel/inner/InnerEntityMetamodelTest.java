/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.inner;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = InnerEntityMetamodelTest.Inner.class)
class InnerEntityMetamodelTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory();
		var innerName = InnerEntityMetamodelTest_.Inner_.name;
		assertNotNull(innerName);
		assertEquals("name", innerName.getName());
		assertEquals(String.class, innerName.getType().getJavaType());
		assertTrue(innerName.isOptional());
		assertFalse(innerName.isId());
		assertFalse(innerName.isVersion());
		assertFalse(innerName.isAssociation());
		assertFalse(innerName.isCollection());
		var innerId = InnerEntityMetamodelTest_.Inner_.id;
		assertNotNull(innerId);
		assertEquals("id", innerId.getName());
		assertEquals(long.class, innerId.getType().getJavaType());
		assertTrue(innerId.isId());
		assertFalse(innerId.isOptional());
		var metatype = InnerEntityMetamodelTest_.Inner_.class_;
		assertNotNull(metatype);
		assertEquals("InnerEntity", metatype.getName());
		assertEquals( 2, metatype.getAttributes().size() );
		assertEquals( Inner.class, metatype.getJavaType() );
	}
	@Entity(name="InnerEntity")
	static class Inner {
		@Id long id;
		String name;
	}
}
