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
package org.hibernate.jpamodelgen.test.inheritance;

import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSuperClass;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class InheritanceTest extends CompilationTest {
	@Test
	public void testSuperEntity() throws Exception {
		assertSuperClass(
				Customer.class.getName() + "_", User.class.getName() + "_"
		);
	}

	@Test
	public void testMappedSuperclass() throws Exception {
		assertSuperClass( House.class.getName() + "_", Building.class.getName() + "_" );
		assertSuperClass( Building.class.getName() + "_", Area.class.getName() + "_" );
	}

	@Override
	protected String getTestPackage() {
		return Customer.class.getPackage().getName();
	}
}
