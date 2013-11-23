/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.bytecode.enhance.spi;

import org.hibernate.engine.spi.CompositeOwner;

/**
 * small low memory class to keep references to composite owners
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CompositeOwnerTracker {
	private String[] names;
	private CompositeOwner[] owners;
	private int size;

	public CompositeOwnerTracker() {
		names = new String[1];
		owners = new CompositeOwner[1];
	}

	public void add(String name, CompositeOwner owner) {
		for ( int i = 0; i < size; i++ ) {
			if ( names[i].equals( name ) ) {
				owners[i] = owner;
				return;
			}
		}
		if ( size >= names.length ) {
			String[] tmpNames = new String[size + 1];
			System.arraycopy( names, 0, tmpNames, 0, size );
			names = tmpNames;
			CompositeOwner[] tmpOwners = new CompositeOwner[size + 1];
			System.arraycopy( owners, 0, tmpOwners, 0, size );
			owners = tmpOwners;
		}
		names[size] = name;
		owners[size] = owner;
		size++;
	}

	public void callOwner(String fieldName) {
		for ( int i = 0; i < size; i++ ) {
			owners[i].$$_hibernate_trackChange( names[i] + fieldName );
		}
	}

	public void removeOwner(String name) {
		for ( int i = 0; i < size; i++ ) {
			if ( names[i].equals( name ) ) {
				if ( i < size ) {
					for ( int j = i; j < size - 1; j++ ) {
						names[j] = names[j + 1];
						owners[j] = owners[j + 1];
					}
					names[size - 1] = null;
					owners[size - 1] = null;
					size--;
				}
			}
		}
	}
}
