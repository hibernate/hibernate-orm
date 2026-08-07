/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.innerclass;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.query.JakartaQuery;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When a nested interface triggers both {@code createRepository} (via {@code @HQL})
 * and {@code createQueryMetamodel} (via {@code @JakartaQuery}), both inner classes
 * must be present in the generated metamodel.
 */
@CompilationTest
@WithClasses(InnerQueryInterfaceTest.MyEntity.class)
class InnerQueryInterfaceTest {

	@Entity
	public static class MyEntity {
		@Id
		@GeneratedValue
		public Long id;
		public String name;

		public interface Queries {
			EntityManager entityManager();

			@HQL("where name = :name")
			List<MyEntity> findByHql(String name);

			@JakartaQuery("from MyEntity where name = :name")
			List<MyEntity> findByJakarta(String name);
		}
	}

	@Test
	void testBothInnerClassesGenerated() {
		assertMetamodelClassGeneratedFor( MyEntity.class );
		final String source = getMetaModelSourceAsString( MyEntity.class );
		assertTrue( source.contains( "class _Queries implements Queries" ),
				"Implementation inner class '_Queries' was not generated" );
		assertTrue( source.contains( "class Queries_" ),
				"Query metamodel inner class 'Queries_' was not generated" );
	}
}
