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

import org.hibernate.envers.exception.VersionsException;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class YProperty {
    private final YMethodsAndClasses ymc;
    private final Object delegate;

    public YProperty(YMethodsAndClasses ymc, Object delegate) {
        this.ymc = ymc;
        this.delegate = delegate;
    }

    public String getName() {
        try {
            return (String) ymc.getXProperty_getNameMethod().invoke(delegate);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotation) {
        try {
            //noinspection unchecked
            return (T) ymc.getXProperty_getAnnotationMethod().invoke(delegate, annotation);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }

    public YClass getType() {
        try {
            return new YClass(ymc, ymc.getXProperty_getTypeMethod().invoke(delegate));
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }
}
