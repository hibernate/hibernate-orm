/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.innerclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import org.hibernate.processor.test.data.innerclass.InnerClassTest.One.Two;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

@CompilationTest
class InnerClassTest {

	@WithClasses({Person.class, Dummy.class, Inner.class, Two.class})
	@Test
	void test() {
		System.out.println( getMetaModelSourceAsString( InnerClassTest.class ) );
		System.out.println( getMetaModelSourceAsString( Dummy.class ) );
		System.out.println( getMetaModelSourceAsString( Person.class ) );
		System.out.println( getMetaModelSourceAsString( InnerClassTest.class, true ) );
		System.out.println( getMetaModelSourceAsString( Dummy.class, true ) );
		System.out.println( getMetaModelSourceAsString( Person.class, true ) );
		assertEquals(
				getMetaModelSourceAsString( Inner.class ),
				getMetaModelSourceAsString( Two.class )
		);
		assertEquals(
				getMetaModelSourceAsString( Inner.class, true ),
				getMetaModelSourceAsString( Two.class, true )
		);
		assertMetamodelClassGeneratedFor( Inner.class );
		assertMetamodelClassGeneratedFor( Inner.class, true );
		assertMetamodelClassGeneratedFor( Two.class );
		assertMetamodelClassGeneratedFor( Two.class, true );
		assertMetamodelClassGeneratedFor( Dummy.Inner.class );
		assertMetamodelClassGeneratedFor( Dummy.Inner.class, true );
		assertMetamodelClassGeneratedFor( Person.class );
		assertMetamodelClassGeneratedFor( Person.class, true );
		assertMetamodelClassGeneratedFor( Person.PersonId.class );
		assertNoMetamodelClassGeneratedFor( Dummy.class );
		assertMetamodelClassGeneratedFor( Dummy.DummyEmbeddable.class );
		/*assertNoMetamodelClassGeneratedFor( Dummy.class );*/
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

	static class One {
		@Entity
		static class Two {
			@Id
			Integer id;
			String value;
		}
	}
}
