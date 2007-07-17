//$Id: BasicLoader.java 9870 2006-05-04 11:57:58Z steve.ebersole@jboss.com $
package org.hibernate.loader;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.BagType;
import org.hibernate.HibernateException;

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
		int bagCount = 0;
		if ( collectionPersisters != null ) {
			String[] collectionSuffixes = getCollectionSuffixes();
			collectionDescriptors = new CollectionAliases[collectionPersisters.length];
			for ( int i = 0; i < collectionPersisters.length; i++ ) {
				if ( isBag( collectionPersisters[i] ) ) {
					bagCount++;
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
		if ( bagCount > 1 ) {
			throw new HibernateException( "cannot simultaneously fetch multiple bags" );
		}
	}

	private boolean isBag(CollectionPersister collectionPersister) {
		return collectionPersister.getCollectionType().getClass().isAssignableFrom( BagType.class );
	}

	/**
	 * Utility method that generates 0_, 1_ suffixes. Subclasses don't
	 * necessarily need to use this algorithm, but it is intended that
	 * they will in most cases.
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
