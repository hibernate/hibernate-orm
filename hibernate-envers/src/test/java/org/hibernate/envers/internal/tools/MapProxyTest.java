package org.hibernate.envers.internal.tools;

import junit.framework.Assert;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MapProxyTest {

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
        Getter getter = ReflectionTools.getGetter(testClass, "age", "property");
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
        Setter setter = ReflectionTools.getSetter(testClass, "age", "property");
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
        Getter getter = ReflectionTools.getGetter(testClass, "checkbox", "property");
        Assert.assertTrue((Boolean) getter.get(testClassInstance));
    }
}
