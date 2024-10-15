/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.innerclass;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;

public class InnerClassTest extends CompilationTest {

	@WithClasses({Person.class, Dummy.class})
	@Test
	public void test() {
		assertMetamodelClassGeneratedFor( Person.class );
		assertMetamodelClassGeneratedFor( Person.PersonId.class );
		assertNoMetamodelClassGeneratedFor( Dummy.class );
		assertMetamodelClassGeneratedFor( Dummy.DummyEmbeddable.class );
		assertMetamodelClassGeneratedFor( Dummy.Inner.class );
		/*assertMetamodelClassGeneratedFor( Dummy.Persona.class );*/
//		assertMetamodelClassGeneratedFor( Persona.class );
	}

//	@MappedSuperclass
//	public abstract static class Persona {
//		private String name;
//
//		public String getName() {
//			return name;
//		}
//
//		public void setName(String name) {
//			this.name = name;
//		}
//
//		public abstract void setId(Long id);
//
//		public abstract String getCity();
//
//		public abstract void setCity(String city);
//	}
}
