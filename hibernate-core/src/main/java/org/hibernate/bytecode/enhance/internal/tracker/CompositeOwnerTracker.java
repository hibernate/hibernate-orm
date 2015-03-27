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
package org.hibernate.bytecode.enhance.internal.tracker;

import org.hibernate.engine.spi.CompositeOwner;

import java.util.Arrays;

/**
 * small low memory class to keep references to composite owners
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public final class CompositeOwnerTracker {

	private String[] names;
	private CompositeOwner[] owners;

	public CompositeOwnerTracker() {
		names = new String[0];
		owners = new CompositeOwner[0];
	}

	public void add(String name, CompositeOwner owner) {
		for ( int i = 0; i < names.length; i++ ) {
			if ( names[i].equals( name ) ) {
				owners[i] = owner;
				return;
			}
		}
		names = Arrays.copyOf( names, names.length + 1 );
		names[names.length - 1] = name;
		owners = Arrays.copyOf( owners, owners.length + 1 );
		owners[owners.length - 1] = owner;
	}

	public void callOwner(String fieldName) {
		for ( int i = 0; i < owners.length ; i++ ) {
			if ( owners[i] != null ) {
				owners[i].$$_hibernate_trackChange( names[i] + fieldName );
			}
		}
	}

	public void removeOwner(String name) {
		for ( int i = 0; i < names.length; i++ ) {
			if ( name.equals( names[i] ) ) {

				final String[] newNames = Arrays.copyOf( names, names.length - 1 );
				System.arraycopy( names, i + 1, newNames, i, newNames.length - i);
				names = newNames;

				final CompositeOwner[] newOwners = Arrays.copyOf( owners, owners.length - 1 );
				System.arraycopy( owners, i + 1, newOwners, i, newOwners.length - i);
				owners = newOwners;

				return;
			}
		}
	}

}
