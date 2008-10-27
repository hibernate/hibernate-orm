/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.jboss.envers.tools.reflection;

import org.jboss.envers.exception.VersionsException;

import java.lang.reflect.Method;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class YMethodsAndClasses {
    private final Method reflectionManager_classForNameMethod;
    private final Method reflectionManager_equalsMethod;

    private final Method xClass_getNameMethod;
    private final Method xClass_getSuperclassMethod;
    private final Method xClass_getDeclaredPropertiesMethod;
    private final Method xClass_getAnnotationMethod;

    private final Method xProperty_getNameMethod;
    private final Method xProperty_getAnnotationMethod;
    private final Method xProperty_getTypeMethod;

    public YMethodsAndClasses(Class<?> delegateClass) throws Exception {
        // Initializing classes
        ClassLoader cl = YMethodsAndClasses.class.getClassLoader();

        Class xClassClass;
        try {
            xClassClass = cl.loadClass("org.hibernate.annotations.common.reflection.XClass");
        } catch (ClassNotFoundException e) {
            try {
                xClassClass = cl.loadClass("org.hibernate.reflection.XClass");
            } catch (ClassNotFoundException e1) {
                throw new VersionsException("No XClass found.");
            }
        }

        Class xPropertyClass;
        try {
            xPropertyClass = cl.loadClass("org.hibernate.annotations.common.reflection.XProperty");
        } catch (ClassNotFoundException e) {
            try {
                xPropertyClass = cl.loadClass("org.hibernate.reflection.XProperty");
            } catch (ClassNotFoundException e1) {
                throw new VersionsException("No XProperty found.");
            }
        }

        // Initializing methods
        reflectionManager_classForNameMethod = delegateClass.getMethod("classForName", String.class, Class.class);
        reflectionManager_equalsMethod = delegateClass.getMethod("equals", xClassClass, Class.class);

        xClass_getNameMethod = xClassClass.getMethod("getName");
        xClass_getSuperclassMethod = xClassClass.getMethod("getSuperclass");
        xClass_getDeclaredPropertiesMethod = xClassClass.getMethod("getDeclaredProperties", String.class);
        xClass_getAnnotationMethod = xClassClass.getMethod("getAnnotation", Class.class);

        xProperty_getNameMethod = xPropertyClass.getMethod("getName");
        xProperty_getTypeMethod = xPropertyClass.getMethod("getType");
        xProperty_getAnnotationMethod = xPropertyClass.getMethod("getAnnotation", Class.class);
    }

    public Method getXClass_getNameMethod() {
        return xClass_getNameMethod;
    }

    public Method getXClass_getSuperclassMethod() {
        return xClass_getSuperclassMethod;
    }

    public Method getXClass_getDeclaredPropertiesMethod() {
        return xClass_getDeclaredPropertiesMethod;
    }

    public Method getXClass_getAnnotationMethod() {
        return xClass_getAnnotationMethod;
    }

    public Method getXProperty_getNameMethod() {
        return xProperty_getNameMethod;
    }

    public Method getXProperty_getAnnotationMethod() {
        return xProperty_getAnnotationMethod;
    }

    public Method getXProperty_getTypeMethod() {
        return xProperty_getTypeMethod;
    }

    public Method getReflectionManager_classForNameMethod() {
        return reflectionManager_classForNameMethod;
    }

    public Method getReflectionManager_equalsMethod() {
        return reflectionManager_equalsMethod;
    }
}
