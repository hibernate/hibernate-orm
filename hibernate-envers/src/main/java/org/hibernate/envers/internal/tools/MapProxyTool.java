package org.hibernate.envers.internal.tools;

import javassist.*;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.envers.internal.entities.PropertyData;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.hibernate.envers.internal.tools.StringTools.capitalizeFirst;
import static org.hibernate.envers.internal.tools.StringTools.getLastComponent;

/**
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
public class MapProxyTool {

    /**
     * @author Lukasz Zuchowski (author at zuchos dot com)
     *  Creates instance of map proxy class. This proxy class will be a java bean with properties from <code>propertyDatas</code>.
     *  Instance will proxy calls to instance of the map passed as parameter.
     * @param name Name of the class to construct (should be unique within class loader)
     * @param map instance that will be proxied by java bean
     * @param propertyDatas properties that should java bean declare
     * @param classLoaderService
     * @return new instance of proxy
     */
    public static Object newInstanceOfBeanProxyForMap(String name, Map<String, Object> map, Set<PropertyData> propertyDatas, ClassLoaderService classLoaderService) {
        Class aClass = loadClass(name, classLoaderService);
        if (aClass == null) {
            Map<String, Class<?>> properties = prepareProperties(propertyDatas);
            aClass = generate(name, properties);
        }
        return createNewInstance(map, aClass);
    }

    private static Object createNewInstance(Map<String, Object> map, Class aClass) {
        try {
            return aClass.getConstructor(Map.class).newInstance(map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Class<?>> prepareProperties(Set<PropertyData> propertyDatas) {
        Map<String, Class<?>> properties = new HashMap<String, Class<?>>();
        for (PropertyData propertyData : propertyDatas) {
            properties.put(propertyData.getBeanName(), Object.class);
        }
        return properties;
    }

    private static Class loadClass(String className, ClassLoaderService classLoaderService) {
        try {
            return ReflectionTools.loadClass(className, classLoaderService);
        } catch (ClassLoadingException e) {
            return null;
        }

    }

    /**
     * Protected for test only
     */
    protected static Class generate(String className, Map<String, Class<?>> properties) {
        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.makeClass(className);

            cc.addInterface(resolveCtClass(Serializable.class));
            cc.addField(new CtField(resolveCtClass(Map.class), "theMap", cc));
            cc.addConstructor(generateConstructor(className, cc));

            for (Entry<String, Class<?>> entry : properties.entrySet()) {
                cc.addField(new CtField(resolveCtClass(entry.getValue()), entry.getKey(), cc));

                // add getter
                cc.addMethod(generateGetter(cc, entry.getKey(), entry.getValue()));

                // add setter
                cc.addMethod(generateSetter(cc, entry.getKey(), entry.getValue()));
            }
            return cc.toClass();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static CtConstructor generateConstructor(String className, CtClass cc) throws NotFoundException, CannotCompileException {
        StringBuffer sb = new StringBuffer();
        sb.append("public ").append(getLastComponent(className)).append("(").append(Map.class.getName()).append(" map)").append("{")
                .append("this.theMap = map;").append("}");
        System.out.println(sb);
        return CtNewConstructor.make(sb.toString(), cc);
    }

    private static CtMethod generateGetter(CtClass declaringClass, String fieldName, Class fieldClass)
            throws CannotCompileException {

        String getterName = "get" + capitalizeFirst(fieldName);

        StringBuffer sb = new StringBuffer();
        sb.append("public ").append(fieldClass.getName()).append(" ")
                .append(getterName).append("(){").append("return (" + fieldClass.getName() + ")this.theMap.get(\"")
                .append(fieldName).append("\")").append(";").append("}");
        return CtMethod.make(sb.toString(), declaringClass);
    }

    private static CtMethod generateSetter(CtClass declaringClass, String fieldName, Class fieldClass)
            throws CannotCompileException {

        String setterName = "set" + capitalizeFirst(fieldName);

        StringBuffer sb = new StringBuffer();
        sb.append("public void ").append(setterName).append("(")
                .append(fieldClass.getName()).append(" ").append(fieldName)
                .append(")").append("{").append("this.theMap.put(\"").append(fieldName)
                .append("\",").append(fieldName).append(")").append(";").append("}");
        return CtMethod.make(sb.toString(), declaringClass);
    }

    private static CtClass resolveCtClass(Class clazz) throws NotFoundException {
        return resolveCtClass(clazz.getName());
    }


    private static CtClass resolveCtClass(String clazz) throws NotFoundException {
        try {
            ClassPool pool = ClassPool.getDefault();
            return pool.get(clazz);
        } catch (NotFoundException e) {
            return null;
        }
    }

}
