/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.internal;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.spi.JoinableAssociation;
import org.hibernate.loader.spi.LoadQueryAliasResolutionContext;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;

/**
 * Represents an entity load query for criteria
 * queries and entity loaders, used for generating SQL.
 *
 * This code is based on the SQL generation code originally in
 * org.hibernate.loader.AbstractEntityJoinWalker.
 *
 * @author Gavin King
 * @author Gail Badner
 */
public abstract class AbstractEntityLoadQueryImpl extends AbstractLoadQueryImpl {

	private final EntityReturn entityReturn;

	public AbstractEntityLoadQueryImpl(EntityReturn entityReturn, List<JoinableAssociation> associations) {
		super( associations );
		this.entityReturn = entityReturn;
	}

	protected final String generateSql(
			final String whereString,
			final String orderByString,
			final LockOptions lockOptions,
			final SessionFactoryImplementor factory,
			final LoadQueryAliasResolutionContext aliasResolutionContext) throws MappingException {
		return generateSql( null, whereString, orderByString, "", lockOptions, factory, aliasResolutionContext );
	}

	private String generateSql(
			final String projection,
			final String condition,
			final String orderBy,
			final String groupBy,
			final LockOptions lockOptions,
			final SessionFactoryImplementor factory,
			final LoadQueryAliasResolutionContext aliasResolutionContext) throws MappingException {

		JoinFragment ojf = mergeOuterJoins( factory, aliasResolutionContext );

		// If no projection, then the last suffix should be for the entity return.
		// TODO: simplify how suffixes are generated/processed.

		final String entityReturnAlias = resolveEntityReturnAlias( aliasResolutionContext );
		Select select = new Select( factory.getDialect() )
				.setLockOptions( lockOptions )
				.setSelectClause(
						projection == null ?
								getPersister().selectFragment(
										entityReturnAlias,
										aliasResolutionContext.resolveEntityColumnAliases( entityReturn ).getSuffix()
								) + associationSelectString( aliasResolutionContext ) :
								projection
				)
				.setFromClause(
						factory.getDialect().appendLockHint(
								lockOptions,
								getPersister().fromTableFragment( entityReturnAlias )
						) + getPersister().fromJoinFragment( entityReturnAlias, true, true )
				)
				.setWhereClause( condition )
				.setOuterJoins(
						ojf.toFromFragmentString(),
						ojf.toWhereFragmentString() + getWhereFragment( aliasResolutionContext )
				)
				.setOrderByClause( orderBy( orderBy, aliasResolutionContext ) )
				.setGroupByClause( groupBy );

		if ( factory.getSettings().isCommentsEnabled() ) {
			select.setComment( getComment() );
		}
		return select.toStatementString();
	}

	protected String getWhereFragment(LoadQueryAliasResolutionContext aliasResolutionContext) throws MappingException {
		// here we do not bother with the discriminator.
		return getPersister().whereJoinFragment( resolveEntityReturnAlias( aliasResolutionContext ), true, true );
	}

	protected abstract String getComment();

	protected final OuterJoinLoadable getPersister() {
		return (OuterJoinLoadable) entityReturn.getEntityPersister();
	}

	protected final String resolveEntityReturnAlias(LoadQueryAliasResolutionContext aliasResolutionContext) {
		return aliasResolutionContext.resolveEntityTableAlias( entityReturn );
	}

	public String toString() {
		return getClass().getName() + '(' + getPersister().getEntityName() + ')';
	}
}
