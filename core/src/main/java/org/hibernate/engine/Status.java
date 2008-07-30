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
package org.hibernate.engine;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;

/**
 * Represents the status of an entity with respect to
 * this session. These statuses are for internal
 * book-keeping only and are not intended to represent
 * any notion that is visible to the _application_.
 */
public final class Status implements Serializable {

	public static final Status MANAGED = new Status( "MANAGED" );
	public static final Status READ_ONLY = new Status( "READ_ONLY" );
	public static final Status DELETED = new Status( "DELETED" );
	public static final Status GONE = new Status( "GONE" );
	public static final Status LOADING = new Status( "LOADING" );
	public static final Status SAVING = new Status( "SAVING" );

	private String name;

	private Status(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}

	private Object readResolve() throws ObjectStreamException {
		return parse( name );
	}

	public static Status parse(String name) throws InvalidObjectException {
		if ( name.equals(MANAGED.name) ) return MANAGED;
		if ( name.equals(READ_ONLY.name) ) return READ_ONLY;
		if ( name.equals(DELETED.name) ) return DELETED;
		if ( name.equals(GONE.name) ) return GONE;
		if ( name.equals(LOADING.name) ) return LOADING;
		if ( name.equals(SAVING.name) ) return SAVING;
		throw new InvalidObjectException( "invalid Status" );
	}
}
