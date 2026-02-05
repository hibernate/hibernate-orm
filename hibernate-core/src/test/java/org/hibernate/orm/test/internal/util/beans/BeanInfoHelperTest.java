/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.internal.util.beans;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.internal.util.beans.BeanInfo;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.internal.util.beans.PropertyDescriptor;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BeanInfoHelper} and related bean introspection classes.
 */
@BaseUnitTest
public class BeanInfoHelperTest {

	@Test
	public void testSimpleBean() {
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( SimpleBean.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		// Should find the "name" property
		PropertyDescriptor nameProp = byName.get( "name" );
		assertNotNull( nameProp, "Should find 'name' property" );
		assertNotNull( nameProp.getReadMethod(), "Should have getter for 'name'" );
		assertNotNull( nameProp.getWriteMethod(), "Should have setter for 'name'" );
		assertEquals( "getName", nameProp.getReadMethod().getName() );
		assertEquals( "setName", nameProp.getWriteMethod().getName() );

		// Should find the "age" property
		PropertyDescriptor ageProp = byName.get( "age" );
		assertNotNull( ageProp, "Should find 'age' property" );
		assertNotNull( ageProp.getReadMethod(), "Should have getter for 'age'" );
		assertNotNull( ageProp.getWriteMethod(), "Should have setter for 'age'" );
	}

	@Test
	public void testBooleanProperty() {
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( BooleanBean.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		// Should find "active" property with isActive() getter
		PropertyDescriptor activeProp = byName.get( "active" );
		assertNotNull( activeProp, "Should find 'active' property" );
		assertNotNull( activeProp.getReadMethod(), "Should have getter for 'active'" );
		assertEquals( "isActive", activeProp.getReadMethod().getName() );
		assertNotNull( activeProp.getWriteMethod(), "Should have setter for 'active'" );
	}

	@Test
	public void testInheritedProperties() {
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( ChildBean.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		// Should find inherited "name" property from SimpleBean
		PropertyDescriptor nameProp = byName.get( "name" );
		assertNotNull( nameProp, "Should find inherited 'name' property" );

		// Should find child-specific "description" property
		PropertyDescriptor descProp = byName.get( "description" );
		assertNotNull( descProp, "Should find 'description' property" );
	}

	@Test
	public void testStopClass() {
		// With stopClass = SimpleBean.class, we should only get ChildBean's own properties
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( ChildBean.class, SimpleBean.class );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		// Should NOT find inherited "name" property
		assertNull( byName.get( "name" ), "Should not find inherited 'name' property when using stopClass" );

		// Should find child-specific "description" property
		PropertyDescriptor descProp = byName.get( "description" );
		assertNotNull( descProp, "Should find 'description' property" );
	}

	@Test
	public void testPropertyWithUpperCaseAcronym() {
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( AcronymBean.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		// URL property should be named "URL" (both chars uppercase, not decapitalized)
		PropertyDescriptor urlProp = byName.get( "URL" );
		assertNotNull( urlProp, "Should find 'URL' property" );
		assertEquals( "getURL", urlProp.getReadMethod().getName() );
	}

	@Test
	public void testVisitBeanInfo() {
		// Test the delegate pattern
		boolean[] visited = { false };

		BeanInfoHelper.visitBeanInfo( SimpleBean.class, beanInfo -> {
			visited[0] = true;
			PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
			assertTrue( descriptors.length > 0, "Should have at least one property" );
		} );

		assertTrue( visited[0], "Delegate should have been called" );
	}

	@Test
	public void testVisitBeanInfoWithReturn() {
		// Test the returning delegate pattern
		int propertyCount = BeanInfoHelper.visitBeanInfo( SimpleBean.class,
				beanInfo -> beanInfo.getPropertyDescriptors().length );

		assertTrue( propertyCount >= 2, "Should have at least 2 properties (name and age)" );
	}

	@Test
	public void testReadOnlyProperty() {
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( ReadOnlyBean.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		PropertyDescriptor idProp = byName.get( "id" );
		assertNotNull( idProp, "Should find 'id' property" );
		assertNotNull( idProp.getReadMethod(), "Should have getter for 'id'" );
		assertNull( idProp.getWriteMethod(), "Should NOT have setter for 'id'" );
	}

	@Test
	public void testWriteOnlyProperty() {
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( WriteOnlyBean.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		PropertyDescriptor passwordProp = byName.get( "password" );
		assertNotNull( passwordProp, "Should find 'password' property" );
		assertNull( passwordProp.getReadMethod(), "Should NOT have getter for 'password'" );
		assertNotNull( passwordProp.getWriteMethod(), "Should have setter for 'password'" );
	}

	@Test
	public void testGetClassNotExposedAsProperty() {
		// Object.getClass() should NOT be exposed as a "class" property
		// because we stop introspection at Object.class
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( SimpleBean.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		assertNull( byName.get( "class" ), "Object.getClass() should NOT be exposed as a property" );
	}

	@Test
	public void testSingleCharacterPropertyName() {
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( SingleCharBean.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		// getX() should produce property "x"
		PropertyDescriptor xProp = byName.get( "x" );
		assertNotNull( xProp, "Should find 'x' property" );
		assertEquals( "getX", xProp.getReadMethod().getName() );
	}

	@Test
	public void testCovariantReturnType() {
		// Child overrides getter with more specific return type
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( CovariantChild.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		PropertyDescriptor valueProp = byName.get( "value" );
		assertNotNull( valueProp, "Should find 'value' property" );
		// Should use the child's getter (more specific type)
		assertEquals( Integer.class, valueProp.getReadMethod().getReturnType() );
	}

	@Test
	public void testOverloadedSetters() {
		// When multiple setters exist with same name but different param types,
		// we accept finding any one of them (implementation takes first found)
		BeanInfo beanInfo = BeanInfoHelper.getBeanInfo( OverloadedSetterBean.class, null );
		PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();

		Map<String, PropertyDescriptor> byName = toMap( descriptors );

		PropertyDescriptor valueProp = byName.get( "value" );
		assertNotNull( valueProp, "Should find 'value' property" );
		assertNotNull( valueProp.getWriteMethod(), "Should have a setter" );
		// We accept any setter - just verify one was found
		assertEquals( "setValue", valueProp.getWriteMethod().getName() );
	}

	@Test
	public void testCachingReturnsSameInstance() {
		// When stopClass is null, results should be cached
		BeanInfo first = BeanInfoHelper.getBeanInfo( SimpleBean.class, null );
		BeanInfo second = BeanInfoHelper.getBeanInfo( SimpleBean.class, null );

		// Should return the same cached instance
		assertSame( first, second, "BeanInfo should be cached and return same instance" );
	}

	@Test
	public void testNonNullStopClassNotCached() {
		// When stopClass is non-null, results are not cached (rare case)
		BeanInfo first = BeanInfoHelper.getBeanInfo( ChildBean.class, SimpleBean.class );
		BeanInfo second = BeanInfoHelper.getBeanInfo( ChildBean.class, SimpleBean.class );

		// These may or may not be the same instance - just verify they work correctly
		Map<String, PropertyDescriptor> props1 = toMap( first.getPropertyDescriptors() );
		Map<String, PropertyDescriptor> props2 = toMap( second.getPropertyDescriptors() );

		assertEquals( props1.keySet(), props2.keySet(), "Should have same properties" );
	}

	private static Map<String, PropertyDescriptor> toMap(final PropertyDescriptor[] descriptors) {
		Map<String, PropertyDescriptor> map = new HashMap<>();
		for ( PropertyDescriptor pd : descriptors ) {
			map.put( pd.getName(), pd );
		}
		return map;
	}

	// Test beans

	public static class SimpleBean {
		private String name;
		private int age;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	public static class BooleanBean {
		private boolean active;

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}
	}

	public static class ChildBean extends SimpleBean {
		private String description;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	public static class AcronymBean {
		private String url;

		public String getURL() {
			return url;
		}

		public void setURL(String url) {
			this.url = url;
		}
	}

	public static class ReadOnlyBean {
		private final Long id = 1L;

		public Long getId() {
			return id;
		}
	}

	public static class WriteOnlyBean {
		private String password;

		public void setPassword(String password) {
			this.password = password;
		}
	}

	public static class SingleCharBean {
		private int x;

		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}
	}

	public static class CovariantParent {
		public Number getValue() {
			return 0;
		}
	}

	public static class CovariantChild extends CovariantParent {
		@Override
		public Integer getValue() {
			return 42;
		}
	}

	public static class OverloadedSetterBean {
		private Object value;

		public Object getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}
	}
}
