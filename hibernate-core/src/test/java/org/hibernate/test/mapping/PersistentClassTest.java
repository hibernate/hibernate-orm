/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.boot.MetadataBuildingContextTestingImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PersistentClassTest extends BaseUnitTestCase {

	private StandardServiceRegistry serviceRegistry;
	private MetadataBuildingContext metadataBuildingContext;

	@Before
	public void prepare() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
		metadataBuildingContext = new MetadataBuildingContextTestingImpl( serviceRegistry );
	}

	@After
	public void release() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	public void testGetMappedClass() {
		RootClass pc = new RootClass( metadataBuildingContext );
		pc.setClassName(String.class.getName());
		Assert.assertEquals(String.class.getName(), pc.getClassName());
		Assert.assertEquals(String.class, pc.getMappedClass());
		pc.setClassName(Integer.class.getName());
		Assert.assertEquals(Integer.class, pc.getMappedClass());
	}
	
	@Test
	public void testGetProxyInterface() {
		RootClass pc = new RootClass( metadataBuildingContext );
		pc.setProxyInterfaceName(String.class.getName());
		Assert.assertEquals(String.class.getName(), pc.getProxyInterfaceName());
		Assert.assertEquals(String.class, pc.getProxyInterface());
		pc.setProxyInterfaceName(Integer.class.getName());
		Assert.assertEquals(Integer.class, pc.getProxyInterface());
	}
	
	@Test
	public void testGetProperty() {
		RootClass pc = new RootClass( metadataBuildingContext );
		Property p = new Property();
		p.setName("name");
		pc.addProperty(p);
		Assert.assertEquals(p, pc.getProperty("name"));
		Assert.assertEquals(p, pc.getProperty("name.test"));
		try {
			Assert.assertNull(pc.getProperty("test"));
			Assert.fail("MappingException expected");
		} catch (MappingException e) {
			// expected
		}
	}

}
