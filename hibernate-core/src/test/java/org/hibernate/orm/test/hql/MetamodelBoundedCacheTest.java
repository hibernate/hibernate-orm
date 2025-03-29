/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.lang.reflect.Field;
import java.util.Map;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.internal.JpaMetamodelImpl;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;

public class MetamodelBoundedCacheTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@JiraKey(value = "HHH-14948")
	public void testMemoryConsumptionOfFailedImportsCache() throws NoSuchFieldException, IllegalAccessException {
		MappingMetamodel mappingMetamodel = sessionFactory().getMappingMetamodel();

		MappingMetamodelImpl mImpl = (MappingMetamodelImpl) mappingMetamodel;
		final JpaMetamodel jpaMetamodel = mImpl.getJpaMetamodel();

		for ( int i = 0; i < 1001; i++ ) {
			jpaMetamodel.qualifyImportableName( "nonexistend" + i );
		}

		Map<?, ?> validImports = extractMapFromMetamodel( jpaMetamodel, "nameToImportMap" );
		Map<?, ?> invalidImports  = extractMapFromMetamodel( jpaMetamodel, "knownInvalidnameToImportMap" );

		assertEquals( 2, validImports.size() );

		// VERY hard-coded, but considering the possibility of a regression of a memory-related issue,
		// it should be worth it
		assertEquals( 1000, invalidImports.size() );
	}


	private Map<?, ?> extractMapFromMetamodel(final JpaMetamodel jpaMetamodel, final String fieldName) throws NoSuchFieldException, IllegalAccessException {
		Field field = JpaMetamodelImpl.class.getDeclaredField( fieldName );
		field.setAccessible( true );
		//noinspection unchecked
		return (Map<?,?>) field.get( jpaMetamodel );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Employee.class };
	}

	@Entity( name = "Employee" )
	@Table( name= "tabEmployees" )
	public class Employee {
		@Id
		private long id;
		private String name;

		public Employee() {

		}

		public Employee(long id, String strName) {
			this();
			this.name = strName;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String strName) {
			this.name = strName;
		}

	}
}
