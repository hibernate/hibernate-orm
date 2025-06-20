/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.innerclass;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfMethodInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

@CompilationTest
@WithClasses({InnerInterfaceTest.MyEntity.class, InnerInterfaceTest.Queries.class})
class InnerInterfaceTest {

	@Entity
	public static class MyEntity {
		@Id
		@GeneratedValue
		public Long id;
		public String foo;
	}

	public interface Queries {
		@HQL("where foo = :foo")
		MyEntity findEntities(String foo);

		@Find
		MyEntity findEntitiesHql(String foo);
	}


	@Test
	void test() {
		System.out.println( getMetaModelSourceAsString( InnerInterfaceTest.MyEntity.class ) );
		System.out.println( getMetaModelSourceAsString( InnerInterfaceTest.Queries.class ) );
		assertMetamodelClassGeneratedFor( InnerInterfaceTest.MyEntity.class );
		assertMetamodelClassGeneratedFor( InnerInterfaceTest.Queries.class );

		assertPresenceOfMethodInMetamodelFor( InnerInterfaceTest.Queries.class, "findEntities",
				EntityManager.class, String.class );
		assertPresenceOfMethodInMetamodelFor( InnerInterfaceTest.Queries.class, "findEntitiesHql",
				EntityManager.class, String.class );
	}
}
