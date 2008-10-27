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
package org.jboss.envers.tools.log;

import org.jboss.envers.exception.VersionsException;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * A class for creating logging facades either to loggers obtained from
 * <code>org.apache.commons.logging.LogFactory</code> (in Hibernate 3.2.x) or from
 * <code>org.slf4j.LoggerFactory</code> (in Hibernate 3.3.x).
 * @author Adam Warski (adam at warski dot org)
 */
public class YLogManager {
    private Method getLogMethod;
    private Class argClass;

    private YLogManager() {
        ClassLoader cl = YLogManager.class.getClassLoader();

        try {
            Class<?> logFactoryClass = cl.loadClass("org.apache.commons.logging.LogFactory");
            try {
                getLogMethod = logFactoryClass.getMethod("getLog", Class.class);
                argClass = Object.class;
            } catch (NoSuchMethodException e) {
                throw new VersionsException("No 'getLog' method in org.apache.commons.logging.LogFactory.");
            }
        } catch (ClassNotFoundException e) {
            try {
                Class<?> loggerFactoryClass = cl.loadClass("org.slf4j.LoggerFactory");
                try {
                    getLogMethod = loggerFactoryClass.getMethod("getLogger", Class.class);
                    argClass = String.class;
                } catch (NoSuchMethodException e1) {
                    throw new VersionsException("No 'getLogger' method in org.slf4j.LoggerFactory.");
                }
            } catch (ClassNotFoundException e1) {
                throw new VersionsException("No org.apache.commons.logging.LogFactory or org.slf4j.LoggerFactory found.");
            }
        }
    }

    public YLog getLog(Class<?> cls) {
        try {
            return new YLog(getLogMethod.invoke(null, cls), argClass);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }

    //

    private static YLogManager instance;

    public static synchronized YLogManager getLogManager() {
        if (instance == null) {
            instance = new YLogManager();
        }

        return instance;
    }
}
