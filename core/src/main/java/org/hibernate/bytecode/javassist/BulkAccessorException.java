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
 *
 */
package org.hibernate.bytecode.javassist;

/**
 * An exception thrown while generating a bulk accessor.
 * 
 * @author Muga Nishizawa
 * @author modified by Shigeru Chiba
 */
public class BulkAccessorException extends RuntimeException {
    private Throwable myCause;

    /**
     * Gets the cause of this throwable.
     * It is for JDK 1.3 compatibility.
     */
    public Throwable getCause() {
        return (myCause == this ? null : myCause);
    }

    /**
     * Initializes the cause of this throwable.
     * It is for JDK 1.3 compatibility.
     */
    public synchronized Throwable initCause(Throwable cause) {
        myCause = cause;
        return this;
    }

    private int index;

    /**
     * Constructs an exception.
     */
    public BulkAccessorException(String message) {
        super(message);
        index = -1;
        initCause(null);
    }

    /**
     * Constructs an exception.
     *
     * @param index     the index of the property that causes an exception.
     */
    public BulkAccessorException(String message, int index) {
        this(message + ": " + index);
        this.index = index;
    }

    /**
     * Constructs an exception.
     */
    public BulkAccessorException(String message, Throwable cause) {
        super(message);
        index = -1;
        initCause(cause);
    }

    /**
     * Constructs an exception.
     *
     * @param index     the index of the property that causes an exception.
     */
    public BulkAccessorException(Throwable cause, int index) {
        this("Property " + index);
        this.index = index;
        initCause(cause);
    }

    /**
     * Returns the index of the property that causes this exception.
     *
     * @return -1 if the index is not specified.
     */
    public int getIndex() {
        return this.index;
    }
}
