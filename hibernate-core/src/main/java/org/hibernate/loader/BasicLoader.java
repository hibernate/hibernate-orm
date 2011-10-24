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
package org.hibernate.loader;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.BagType;

/**
 * Uses the default mapping from property to result set column 
 * alias defined by the entities' persisters. Used when Hibernate
 * is generating result set column aliases.
 * 
 * @author Gavin King
 */
public abstract class BasicLoader extends Loader {

	protected static final String[] NO_SUFFIX = {""};

	private EntityAliases[] descriptors;
	private CollectionAliases[] collectionDescriptors;

	public BasicLoader(SessionFactoryImplementor factory) {
		super(factory);
	}

	protected final EntityAliases[] getEntityAliases() {
		return descriptors;
	}

	protected final CollectionAliases[] getCollectionAliases() {
		return collectionDescriptors;
	}

	protected abstract String[] getSuffixes();
	protected abstract String[] getCollectionSuffixes();

	protected void postInstantiate() {
		Loadable[] persisters = getEntityPersisters();
		String[] suffixes = getSuffixes();
		descriptors = new EntityAliases[persisters.length];
		for ( int i=0; i<descriptors.length; i++ ) {
			descriptors[i] = new DefaultEntityAliases( persisters[i], suffixes[i] );
		}

		CollectionPersister[] collectionPersisters = getCollectionPersisters();
		List bagRoles = null;
		if ( collectionPersisters != null ) {
			String[] collectionSuffixes = getCollectionSuffixes();
			collectionDescriptors = new CollectionAliases[collectionPersisters.length];
			for ( int i = 0; i < collectionPersisters.length; i++ ) {
				if ( isBag( collectionPersisters[i] ) ) {
					if ( bagRoles == null ) {
						bagRoles = new ArrayList();
					}
					bagRoles.add( collectionPersisters[i].getRole() );
				}
				collectionDescriptors[i] = new GeneratedCollectionAliases(
						collectionPersisters[i],
						collectionSuffixes[i]
					);
			}
		}
		else {
			collectionDescriptors = null;
		}
		if ( bagRoles != null && bagRoles.size() > 1 ) {
			throw new MultipleBagFetchException( bagRoles );
		}
	}

	private boolean isBag(CollectionPersister collectionPersister) {
		return collectionPersister.getCollectionType().getClass().isAssignableFrom( BagType.class );
	}

	/**
	 * Utility method that generates 0_, 1_ suffixes. Subclasses don't
	 * necessarily need to use this algorithm, but it is intended that
	 * they will in most cases.
	 *
	 * @param length The number of suffixes to generate
	 *
	 * @return The array of generated suffixes (with length=length).
	 */
	public static String[] generateSuffixes(int length) {
		return generateSuffixes( 0, length );
	}

	public static String[] generateSuffixes(int seed, int length) {
		if ( length == 0 ) return NO_SUFFIX;

		String[] suffixes = new String[length];
		for ( int i = 0; i < length; i++ ) {
			suffixes[i] = Integer.toString( i + seed ) + "_";
		}
		return suffixes;
	}

}
