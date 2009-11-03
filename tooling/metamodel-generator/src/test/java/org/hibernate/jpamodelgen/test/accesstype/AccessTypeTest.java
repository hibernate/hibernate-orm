// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jpamodelgen.test.accesstype;

import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAbsenceOfField;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfField;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class AccessTypeTest extends CompilationTest {

	@Test
	public void testExcludeTransientFieldAndStatic() throws Exception {
		assertAbsenceOfField( Product.class.getName() + "_", "nonPersistent" );
		assertAbsenceOfField( Product.class.getName() + "_", "nonPersistent2" );
	}

	@Test
	public void testDefaultAccessTypeOnEntity() throws Exception {
		assertAbsenceOfField( User.class.getName() + "_", "nonPersistent" );
	}

	@Test
	public void testDefaultAccessTypeForSubclassOfEntity() throws Exception {
		assertAbsenceOfField( Customer.class.getName() + "_", "nonPersistent" );
	}

	@Test
	public void testDefaultAccessTypeForEmbeddable() throws Exception {
		assertAbsenceOfField( Detail.class.getName() + "_", "nonPersistent" );
	}

	@Test
	public void testInheritedAccessTypeForEmbeddable() throws Exception {
		assertAbsenceOfField( Country.class.getName() + "_", "nonPersistent" );
		assertAbsenceOfField(
				Pet.class.getName() + "_", "nonPersistent", "Collection of embeddable not taken care of"
		);
	}

	@Test
	public void testDefaultAccessTypeForMappedSuperclass() throws Exception {
		assertAbsenceOfField( Detail.class.getName() + "_", "volume" );
	}

	@Test
	public void testExplicitAccessTypeAndDefaultFromRootEntity() throws Exception {
		assertAbsenceOfField(
				LivingBeing.class.getName() + "_",
				"nonPersistent",
				"explicit access type on mapped superclass"
		);
		assertAbsenceOfField( Hominidae.class.getName() + "_", "nonPersistent", "explicit access type on entity" );
		assertAbsenceOfField(
				Human.class.getName() + "_",
				"nonPersistent",
				"proper inheritance from root entity access type"
		);
	}

	@Test
	public void testMemberAccessType() throws Exception {
		assertPresenceOfField( Customer.class.getName() + "_", "goodPayer", "access type overriding" );
	}

	@Override
	protected String getTestPackage() {
		return Product.class.getPackage().getName();
	}
}
