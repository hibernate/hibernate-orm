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
import java.util.Iterator;

import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.LockOptions;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;
import org.hibernate.type.AssociationType;
import org.hibernate.util.CollectionHelper;

/**
 * Abstract walker for walkers which begin at an entity (criteria
 * queries and entity loaders).
 *
 * @author Gavin King
 */
public abstract class AbstractEntityJoinWalker extends JoinWalker {

	private final OuterJoinLoadable persister;
	private final String alias;

	public AbstractEntityJoinWalker(
			OuterJoinLoadable persister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		this( persister, factory, loadQueryInfluencers, null );
	}

	public AbstractEntityJoinWalker(
			OuterJoinLoadable persister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers,
			String alias) {
		super( factory, loadQueryInfluencers );
		this.persister = persister;
		this.alias = ( alias == null ) ? generateRootAlias( persister.getEntityName() ) : alias;
	}

	protected final void initAll(
			final String whereString,
			final String orderByString,
			final LockOptions lockOptions) throws MappingException {
		walkEntityTree( persister, getAlias() );
		List allAssociations = new ArrayList();
		allAssociations.addAll(associations);
		allAssociations.add(
				new OuterJoinableAssociation(
						persister.getEntityType(),
						null,
						null,
						alias,
						JoinFragment.LEFT_OUTER_JOIN,
						null,
						getFactory(),
						CollectionHelper.EMPTY_MAP
				)
		);
		initPersisters(allAssociations, lockOptions);
		initStatementString( whereString, orderByString, lockOptions);
	}

	protected final void initProjection(
			final String projectionString,
			final String whereString,
			final String orderByString,
			final String groupByString,
			final LockOptions lockOptions) throws MappingException {
		walkEntityTree( persister, getAlias() );
		persisters = new Loadable[0];
		initStatementString(projectionString, whereString, orderByString, groupByString, lockOptions);
	}

	private void initStatementString(
			final String condition,
			final String orderBy,
			final LockOptions lockOptions) throws MappingException {
		initStatementString(null, condition, orderBy, "", lockOptions);
	}

	private void initStatementString(
			final String projection,
			final String condition,
			final String orderBy,
			final String groupBy,
			final LockOptions lockOptions) throws MappingException {

		final int joins = countEntityPersisters( associations );
		suffixes = BasicLoader.generateSuffixes( joins + 1 );

		JoinFragment ojf = mergeOuterJoins( associations );

		Select select = new Select( getDialect() )
				.setLockOptions( lockOptions )
				.setSelectClause(
						projection == null ?
								persister.selectFragment( alias, suffixes[joins] ) + selectString( associations ) :
								projection
				)
				.setFromClause(
						getDialect().appendLockHint( lockOptions.getLockMode(), persister.fromTableFragment( alias ) ) +
								persister.fromJoinFragment( alias, true, true )
				)
				.setWhereClause( condition )
				.setOuterJoins(
						ojf.toFromFragmentString(),
						ojf.toWhereFragmentString() + getWhereFragment()
				)
				.setOrderByClause( orderBy( associations, orderBy ) )
				.setGroupByClause( groupBy );

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( getComment() );
		}
		sql = select.toStatementString();
	}

	protected String getWhereFragment() throws MappingException {
		// here we do not bother with the discriminator.
		return persister.whereJoinFragment(alias, true, true);
	}

	/**
	 * The superclass deliberately excludes collections
	 */
	protected boolean isJoinedFetchEnabled(AssociationType type, FetchMode config, CascadeStyle cascadeStyle) {
		return isJoinedFetchEnabledInMapping( config, type );
	}

	protected final boolean isJoinFetchEnabledByProfile(OuterJoinLoadable persister, String path, int propertyNumber) {
		if ( !getLoadQueryInfluencers().hasEnabledFetchProfiles() ) {
			// perf optimization
			return false;
		}

		// ugh, this stuff has to be made easier...
		String rootPropertyName = persister.getSubclassPropertyName( propertyNumber );
		int pos = path.lastIndexOf( rootPropertyName );
		String relativePropertyPath = pos >= 0
				? path.substring( pos )
				: rootPropertyName;
		String fetchRole = persister.getEntityName() + "." + relativePropertyPath;

		Iterator profiles = getLoadQueryInfluencers().getEnabledFetchProfileNames().iterator();
		while ( profiles.hasNext() ) {
			final String profileName = ( String ) profiles.next();
			final FetchProfile profile = getFactory().getFetchProfile( profileName );
			final Fetch fetch = profile.getFetchByRole( fetchRole );
			if ( fetch != null && Fetch.Style.JOIN == fetch.getStyle() ) {
				return true;
			}
		}
		return false;
	}

	public abstract String getComment();

	protected final Loadable getPersister() {
		return persister;
	}

	protected final String getAlias() {
		return alias;
	}

	public String toString() {
		return getClass().getName() + '(' + getPersister().getEntityName() + ')';
	}
}
