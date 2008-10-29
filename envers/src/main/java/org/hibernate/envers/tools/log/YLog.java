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
package org.jboss.envers.tools.log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.envers.exception.VersionsException;

/**
 * A simple logger facade which delegates through reflection to a logging delegate.
 * @author Adam Warski (adam at warski dot org)
 */
public class YLog {
    private final Object delegate;
    private final Method errorMethod;
    private final Method warnMethod;
    private final Method infoMethod;

    public YLog(Object delegate, Class<?> argClass) {
        this.delegate = delegate;

        try {
            errorMethod = delegate.getClass().getMethod("error", argClass);
        } catch (NoSuchMethodException e) {
            throw new VersionsException(e);
        }

        try {
            warnMethod = delegate.getClass().getMethod("warn", argClass);
        } catch (NoSuchMethodException e) {
            throw new VersionsException(e);
        }

        try {
            infoMethod = delegate.getClass().getMethod("info", argClass);
        } catch (NoSuchMethodException e) {
            throw new VersionsException(e);
        }
    }

    public void error(String message) {
        try {
            errorMethod.invoke(delegate, message);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }

    public void warn(String message) {
        try {
            warnMethod.invoke(delegate, message);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }

    public void info(String message) {
        try {
            infoMethod.invoke(delegate, message);
        } catch (IllegalAccessException e) {
            throw new VersionsException(e);
        } catch (InvocationTargetException e) {
            throw new VersionsException(e);
        }
    }
}
