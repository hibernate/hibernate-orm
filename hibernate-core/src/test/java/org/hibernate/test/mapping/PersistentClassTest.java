package org.hibernate.test.mapping;

import org.hibernate.MappingException;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Assert;
import org.junit.Test;

public class PersistentClassTest extends BaseUnitTestCase {

	@Test
	public void testGetMappedClass() {
		RootClass pc = new RootClass();
		pc.setClassName(String.class.getName());
		Assert.assertEquals(String.class.getName(), pc.getClassName());
		Assert.assertEquals(String.class, pc.getMappedClass());
		pc.setClassName(Integer.class.getName());
		Assert.assertEquals(Integer.class, pc.getMappedClass());
	}
	
	@Test
	public void testGetProxyInterface() {
		RootClass pc = new RootClass();
		pc.setProxyInterfaceName(String.class.getName());
		Assert.assertEquals(String.class.getName(), pc.getProxyInterfaceName());
		Assert.assertEquals(String.class, pc.getProxyInterface());
		pc.setProxyInterfaceName(Integer.class.getName());
		Assert.assertEquals(Integer.class, pc.getProxyInterface());
	}
	
	@Test
	public void testGetProperty() {
		RootClass pc = new RootClass();
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
