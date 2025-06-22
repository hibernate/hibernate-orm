/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@BootstrapServiceRegistry(
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(annotatedClasses = OracleArrayTest.ArrayHolder.class)
@SessionFactory
@RequiresDialect(OracleDialect.class)
@JiraKey("HHH-10999")
public class OracleArrayTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ArrayHolder expected = new ArrayHolder( 1, new Integer[] { 1, 2, 3 }, new String[] { "abc", "def" } );
			session.persist( expected );
			session.flush();
			session.clear();

			ArrayHolder arrayHolder = session.find( ArrayHolder.class, 1 );
			Assert.assertEquals( expected.getIntArray(), arrayHolder.getIntArray() );
			Assert.assertEquals( expected.getTextArray(), arrayHolder.getTextArray() );
		} );
	}

	@Entity(name = "ArrayHolder")
	public static class ArrayHolder {
		@Id
		Integer id;

		String[] textArray;

		String[] textArray2;

		Integer[] intArray;

		public ArrayHolder() {
		}

		public ArrayHolder(Integer id, Integer[] intArray, String[] textArray) {
			this.id = id;
			this.intArray = intArray;
			this.textArray = textArray;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer[] getIntArray() {
			return intArray;
		}

		public void setIntArray(Integer[] intArray) {
			this.intArray = intArray;
		}

		public String[] getTextArray() {
			return textArray;
		}

		public void setTextArray(String[] textArray) {
			this.textArray = textArray;
		}

		public String[] getTextArray2() {
			return textArray2;
		}

		public void setTextArray2(String[] textArray2) {
			this.textArray2 = textArray2;
		}
	}

}
