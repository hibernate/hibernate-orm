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

import org.hibernate.cfg.Configuration;
import org.hibernate.MappingException;
import org.jboss.envers.exception.VersionsException;

import java.lang.reflect.InvocationTargetException;

/**
 * A reflection manager which proxies either to the old classes from package org.hibernate.reflection,
 * or to the new classed from package org.hibernate.annotations.common.reflection.
 * @author Adam Warski (adam at warski dot org)
 */
public class YReflectionManager {
    private final Object delegate;
    private final YMethodsAndClasses ymc;

    public YReflectionManager(Configuration cfg) throws Exception {
        Object delegateTemp;
        try {
            delegateTemp = cfg.getClass().getMethod("getReflectionManager").invoke(cfg);
        } catch (NoSuchMethodException e) {
            // TODO - what if there it's a pure Hibernate envirionment?
            delegateTemp = ReflectionTools
                    .loadClass("org.hibernate.cfg.annotations.reflection.EJB3ReflectionManager")
                    .newInstance();
        }
        delegate = delegateTemp;
        ymc = new YMethodsAndClasses(delegate.getClass());
    }

    public YClass classForName(String className, Class<?> caller) throws ClassNotFoundException {
        try {
            return new YClass(ymc, ymc.getReflectionManager_classForNameMethod().invoke(delegate, className, caller));
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }

    public <T> boolean equals(YClass class1, java.lang.Class<T> class2) {
        try {
            return (Boolean) ymc.getReflectionManager_equalsMethod().invoke(delegate, class1.getDelegate(), class2);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }

    public static YReflectionManager get(Configuration cfg) {
        try {
            return new YReflectionManager(cfg);
        } catch (Exception e) {
            throw new MappingException(e);
        }
    }
}
