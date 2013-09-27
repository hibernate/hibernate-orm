package org.hibernate.envers.internal.tools;

import org.hibernate.property.Getter;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MapProxyTest {

    @Test
    public void testGenerate() throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("age",14);
        Map<String, Class<?>> properties = new HashMap<String, Class<?>>();
        properties.put("age",Integer.class);
        Class testClass = MapProxyTool.generate("TestClass", properties);
        Object testClassInstance = testClass.getConstructor(Map.class).newInstance(map);
        Getter getter = ReflectionTools.getGetter(testClass, "age", "property");
        Object o = getter.get(testClassInstance);
        System.out.println(o);
    }
}
