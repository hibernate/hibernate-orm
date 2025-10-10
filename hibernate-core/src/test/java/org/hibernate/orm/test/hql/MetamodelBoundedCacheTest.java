/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.internal.JpaMetamodelImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = MetamodelBoundedCacheTest.Employee.class)
@SessionFactory
public class MetamodelBoundedCacheTest {

	@Test
	@JiraKey(value = "HHH-14948")
	public void testMemoryConsumptionOfFailedImportsCache(SessionFactoryScope factoryScope) throws NoSuchFieldException, IllegalAccessException {
		var jpaModel = factoryScope.getSessionFactory().getJpaMetamodel();

		for ( int i = 0; i < 1001; i++ ) {
			jpaModel.qualifyImportableName( "nonexistend" + i );
		}

		Map<?, ?> validImports = extractMapFromMetamodel( jpaModel, "nameToImportMap" );
		Map<?, ?> invalidImports  = extractMapFromMetamodel( jpaModel, "knownInvalidnameToImportMap" );

		Assertions.assertEquals( 2, validImports.size() );

		// VERY hard-coded, but considering the possibility of a regression of a memory-related issue,
		// it should be worth it
		Assertions.assertEquals( 1000, invalidImports.size() );
	}


	private Map<?, ?> extractMapFromMetamodel(final JpaMetamodel jpaMetamodel, final String fieldName) throws NoSuchFieldException, IllegalAccessException {
		Field field = JpaMetamodelImpl.class.getDeclaredField( fieldName );
		field.setAccessible( true );
		//noinspection unchecked
		return (Map<?,?>) field.get( jpaMetamodel );
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
