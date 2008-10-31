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
package org.hibernate.envers.tools.log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hibernate.envers.exception.AuditException;

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
                throw new AuditException("No 'getLog' method in org.apache.commons.logging.LogFactory.");
            }
        } catch (ClassNotFoundException e) {
            try {
                Class<?> loggerFactoryClass = cl.loadClass("org.slf4j.LoggerFactory");
                try {
                    getLogMethod = loggerFactoryClass.getMethod("getLogger", Class.class);
                    argClass = String.class;
                } catch (NoSuchMethodException e1) {
                    throw new AuditException("No 'getLogger' method in org.slf4j.LoggerFactory.");
                }
            } catch (ClassNotFoundException e1) {
                throw new AuditException("No org.apache.commons.logging.LogFactory or org.slf4j.LoggerFactory found.");
            }
        }
    }

    public YLog getLog(Class<?> cls) {
        try {
            return new YLog(getLogMethod.invoke(null, cls), argClass);
        } catch (IllegalAccessException e) {
            throw new AuditException(e);
        } catch (InvocationTargetException e) {
            throw new AuditException(e);
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
