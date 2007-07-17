//$Id: OuterJoinLoader.java 7124 2005-06-13 20:27:16Z oneovthafew $
package org.hibernate.loader;

import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.EntityType;

/**
 * Implements logic for walking a tree of associated classes.
 *
 * Generates an SQL select string containing all properties of those classes.
 * Tables are joined using an ANSI-style left outer join.
 *
 * @author Gavin King
 */
public abstract class OuterJoinLoader extends BasicLoader {

	protected Loadable[] persisters;
	protected CollectionPersister[] collectionPersisters;
	protected int[] collectionOwners;
	protected String[] aliases;
	protected LockMode[] lockModeArray;
	protected int[] owners;
	protected EntityType[] ownerAssociationTypes;
	protected String sql;
	protected String[] suffixes;
	protected String[] collectionSuffixes;

    private Map enabledFilters;
    
    protected final Dialect getDialect() {
    	return getFactory().getDialect();
    }

	public OuterJoinLoader(SessionFactoryImplementor factory, Map enabledFilters) {
		super(factory);
		this.enabledFilters = enabledFilters;
	}

	protected String[] getSuffixes() {
		return suffixes;
	}

	protected String[] getCollectionSuffixes() {
		return collectionSuffixes;
	}

	protected final String getSQLString() {
		return sql;
	}

	protected final Loadable[] getEntityPersisters() {
		return persisters;
	}

	protected int[] getOwners() {
		return owners;
	}

	protected EntityType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}

	protected LockMode[] getLockModes(Map lockModes) {
		return lockModeArray;
	}
	
	public Map getEnabledFilters() {
		return enabledFilters;
	}

	protected final String[] getAliases() {
		return aliases;
	}

	protected final CollectionPersister[] getCollectionPersisters() {
		return collectionPersisters;
	}

	protected final int[] getCollectionOwners() {
		return collectionOwners;
	}

	protected void initFromWalker(JoinWalker walker) {
		persisters = walker.getPersisters();
		collectionPersisters = walker.getCollectionPersisters();
		ownerAssociationTypes = walker.getOwnerAssociationTypes();
		lockModeArray = walker.getLockModeArray();
		suffixes = walker.getSuffixes();
		collectionSuffixes = walker.getCollectionSuffixes();
		owners = walker.getOwners();
		collectionOwners = walker.getCollectionOwners();
		sql = walker.getSQLString();
		aliases = walker.getAliases();
	}

}
