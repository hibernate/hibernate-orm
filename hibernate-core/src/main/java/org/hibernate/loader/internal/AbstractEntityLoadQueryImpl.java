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
package org.hibernate.loader.internal;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;

/**
 * Abstract walker for walkers which begin at an entity (criteria
 * queries and entity loaders).
 *
 * @author Gavin King
 */
public abstract class AbstractEntityLoadQueryImpl extends AbstractLoadQueryImpl {

	private final EntityReturn entityReturn;

	public AbstractEntityLoadQueryImpl(
			SessionFactoryImplementor factory,
			EntityReturn entityReturn,
			List<JoinableAssociationImpl> associations,
			List<String> suffixes) {
		super( factory, associations, suffixes );
		this.entityReturn = entityReturn;
	}

	protected final String generateSql(
			final String whereString,
			final String orderByString,
			final LockOptions lockOptions) throws MappingException {
		return generateSql( null, whereString, orderByString, "", lockOptions );
	}

	private String generateSql(
			final String projection,
			final String condition,
			final String orderBy,
			final String groupBy,
			final LockOptions lockOptions) throws MappingException {

		JoinFragment ojf = mergeOuterJoins();

		Select select = new Select( getDialect() )
				.setLockOptions( lockOptions )
				.setSelectClause(
						projection == null ?
								getPersister().selectFragment( getAlias(), entityReturn.getEntityAliases().getSuffix() ) + associationSelectString() :
								projection
				)
				.setFromClause(
						getDialect().appendLockHint( lockOptions, getPersister().fromTableFragment( getAlias() ) ) +
								getPersister().fromJoinFragment( getAlias(), true, true )
				)
				.setWhereClause( condition )
				.setOuterJoins(
						ojf.toFromFragmentString(),
						ojf.toWhereFragmentString() + getWhereFragment()
				)
				.setOrderByClause( orderBy( orderBy ) )
				.setGroupByClause( groupBy );

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( getComment() );
		}
		return select.toStatementString();
	}

	protected String getWhereFragment() throws MappingException {
		// here we do not bother with the discriminator.
		return getPersister().whereJoinFragment( getAlias(), true, true );
	}

	public abstract String getComment();

	public final OuterJoinLoadable getPersister() {
		return (OuterJoinLoadable) entityReturn.getEntityPersister();
	}

	public final String getAlias() {
		return entityReturn.getSqlTableAlias();
	}

	public String toString() {
		return getClass().getName() + '(' + getPersister().getEntityName() + ')';
	}
}
