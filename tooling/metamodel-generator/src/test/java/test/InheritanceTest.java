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
package test;

import org.testng.annotations.Test;
import org.testng.Assert;
import model.Customer_;
import model.User_;
import model.House_;
import model.Building_;
import model.Area_;

/**
 * @author Emmanuel Bernard
 */
@Test
public class InheritanceTest {
	@Test
	public void testSuperEntity() throws Exception {
		Assert.assertEquals( Customer_.class.getSuperclass(), User_.class,
				"Entity with super entity should inherit at the metamodel level");
	}

	@Test
	public void testMappedSuperclass() throws Exception {
		Assert.assertEquals( House_.class.getSuperclass(), Building_.class,
				"Entity with mapped superclass should inherit at the metamodel level");
		Assert.assertEquals( Building_.class.getSuperclass(), Area_.class,
				"mapped superclass with mapped superclass should inherit at the metamodel level");
	}
}
