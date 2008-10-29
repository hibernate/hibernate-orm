/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.tools.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.exception.VersionsException;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class YClass {
    private final YMethodsAndClasses ymc;
    private final Object delegate;

    public YClass(YMethodsAndClasses ymc, Object delegate) {
        this.ymc = ymc;
        this.delegate = delegate;
    }

    Object getDelegate() {
        return delegate;
    }

    public String getName() {
        try {
            return (String) ymc.getXClass_getNameMethod().invoke(delegate);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }

    public YClass getSuperclass() {
        try {
            return new YClass(ymc, ymc.getXClass_getSuperclassMethod().invoke(delegate));
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }

    public List<YProperty> getDeclaredProperties(String accessMode) {
        List delegates;

        try {
            delegates = (List) ymc.getXClass_getDeclaredPropertiesMethod().invoke(delegate, accessMode);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }

        List<YProperty> ret = new ArrayList<YProperty>();
        for (Object delegate : delegates) {
            ret.add(new YProperty(ymc, delegate));
        }

        return ret;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        try {
            //noinspection unchecked
            return (T) ymc.getXClass_getAnnotationMethod().invoke(delegate, annotation);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }
}
