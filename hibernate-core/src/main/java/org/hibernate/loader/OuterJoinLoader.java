/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
	private LockOptions lockOptions;
	protected LockMode[] lockModeArray;
	protected int[] owners;
	protected EntityType[] ownerAssociationTypes;
	protected String sql;
	protected String[] suffixes;
	protected String[] collectionSuffixes;

	private LoadQueryInfluencers loadQueryInfluencers;

	protected final Dialect getDialect() {
		return getFactory().getDialect();
	}

	public OuterJoinLoader(
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( factory );
		this.loadQueryInfluencers = loadQueryInfluencers;
	}

	protected String[] getSuffixes() {
		return suffixes;
	}

	protected String[] getCollectionSuffixes() {
		return collectionSuffixes;
	}

	@Override
	public final String getSQLString() {
		return sql;
	}

	public final Loadable[] getEntityPersisters() {
		return persisters;
	}

	protected int[] getOwners() {
		return owners;
	}

	protected EntityType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}

	protected LockMode[] getLockModes(LockOptions lockOptions) {
		return lockModeArray;
	}

	protected LockOptions getLockOptions() {
		return lockOptions;
	}

	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	protected final String[] getAliases() {
		return aliases;
	}

	public final CollectionPersister[] getCollectionPersisters() {
		return collectionPersisters;
	}

	protected final int[] getCollectionOwners() {
		return collectionOwners;
	}

	protected void initFromWalker(JoinWalker walker) {
		persisters = walker.getPersisters();
		collectionPersisters = walker.getCollectionPersisters();
		ownerAssociationTypes = walker.getOwnerAssociationTypes();
		lockOptions = walker.getLockModeOptions();
		lockModeArray = walker.getLockModeArray();
		suffixes = walker.getSuffixes();
		collectionSuffixes = walker.getCollectionSuffixes();
		owners = walker.getOwners();
		collectionOwners = walker.getCollectionOwners();
		sql = walker.getSQLString();
		aliases = walker.getAliases();
	}

}
