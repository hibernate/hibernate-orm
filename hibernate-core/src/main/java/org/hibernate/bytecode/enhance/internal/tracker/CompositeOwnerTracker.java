/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.internal.tracker;

import java.util.Arrays;

import org.hibernate.engine.spi.CompositeOwner;

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
