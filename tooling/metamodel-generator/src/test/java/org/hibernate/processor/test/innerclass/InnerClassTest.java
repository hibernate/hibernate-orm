/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.innerclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

public class InnerClassTest extends CompilationTest {

	@WithClasses({Person.class, Dummy.class, Inner.class})
	@Test
	public void test() {
		assertMetamodelClassGeneratedFor( Inner.class );
		System.out.println( getMetaModelSourceAsString( Inner.class ) );
		assertMetamodelClassGeneratedFor( Dummy.Inner.class );
		System.out.println( getMetaModelSourceAsString( Dummy.Inner.class ) );
		assertMetamodelClassGeneratedFor( Person.class );
		System.out.println( getMetaModelSourceAsString( Person.class ) );
		assertMetamodelClassGeneratedFor( Person.PersonId.class );
		System.out.println( getMetaModelSourceAsString( Person.PersonId.class ) );
		assertNoMetamodelClassGeneratedFor( Dummy.class );
		assertMetamodelClassGeneratedFor( Dummy.DummyEmbeddable.class );
		System.out.println( getMetaModelSourceAsString( Dummy.DummyEmbeddable.class ) );
	}

	@Entity(name = "Inner")
	@NamedQuery(name = "allInner", query = "from Inner")
	public static class Inner {
		@Id
		Integer id;

		String address;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}
	}
}
