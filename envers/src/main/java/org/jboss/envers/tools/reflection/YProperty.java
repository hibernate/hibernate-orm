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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

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
