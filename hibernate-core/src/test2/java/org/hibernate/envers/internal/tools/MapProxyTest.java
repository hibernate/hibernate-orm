/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import junit.framework.Assert;

public class MapProxyTest {

    private StandardServiceRegistry serviceRegistry;

	@Before
	public void prepare() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void release() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

    @Test
    public void shouldGenerateClassWithAppropriateGetter() throws Exception {
        //given
        Map<String, Object> map = new HashMap<String, Object>();
        int ageExpected = 14;
        map.put("age", ageExpected);
        Map<String, Class<?>> properties = new HashMap<String, Class<?>>();
        properties.put("age", Integer.class);
        //when
        Class testClass = MapProxyTool.classForName("TestClass1", properties, new ClassLoaderServiceImpl());
        Object testClassInstance = testClass.getConstructor(Map.class).newInstance(map);

        //then
        Getter getter = ReflectionTools.getGetter( testClass, "age", "property", serviceRegistry );
        int age = (Integer) getter.get(testClassInstance);
        Assert.assertEquals(ageExpected, age);
    }

    @Test
    public void shouldGenerateClassWithAppropriateSetter() throws Exception {
        //given
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, Class<?>> properties = new HashMap<String, Class<?>>();
        properties.put("age", Integer.class);

        //when
        Class testClass = MapProxyTool.classForName("TestClass2", properties, new ClassLoaderServiceImpl());
        Object testClassInstance = testClass.getConstructor(Map.class).newInstance(map);

        //then
        Setter setter = ReflectionTools.getSetter(testClass, "age", "property", serviceRegistry);
        int ageExpected = 14;
        setter.set(testClassInstance, ageExpected, null);
        Object age = map.get("age");
        Assert.assertEquals(ageExpected, age);
    }

    @Test
    public void shouldGenerateClassWithAppropriateAccessorsForBoolean() throws Exception {
        //given
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("checkbox",true);
        Map<String, Class<?>> properties = new HashMap<String, Class<?>>();
        properties.put("checkbox", Boolean.class);

        //when
        Class testClass = MapProxyTool.classForName("TestClass3", properties, new ClassLoaderServiceImpl());
        Object testClassInstance = testClass.getConstructor(Map.class).newInstance(map);

        //then
        Getter getter = ReflectionTools.getGetter(testClass, "checkbox", "property", serviceRegistry);
        Assert.assertTrue((Boolean) getter.get(testClassInstance));
    }
}
