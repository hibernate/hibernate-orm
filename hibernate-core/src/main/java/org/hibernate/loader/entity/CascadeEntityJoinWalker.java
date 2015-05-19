/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;

import java.util.Collections;

import org.hibernate.FetchMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.AbstractEntityJoinWalker;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.AssociationType;

public class CascadeEntityJoinWalker extends AbstractEntityJoinWalker {

	private final CascadingAction cascadeAction;

	public CascadeEntityJoinWalker(
			OuterJoinLoadable persister,
			CascadingAction action,
			SessionFactoryImplementor factory)
			throws MappingException {
		super( persister, factory, LoadQueryInfluencers.NONE );
		this.cascadeAction = action;
		StringBuilder whereCondition = whereString( getAlias(), persister.getIdentifierColumnNames(), 1 )
				//include the discriminator and class-level where, but not filters
				.append( persister.filterFragment( getAlias(), Collections.EMPTY_MAP ) );

		initAll( whereCondition.toString(), "", LockOptions.READ );
	}

	@Override
	protected boolean isJoinedFetchEnabled(AssociationType type, FetchMode config, CascadeStyle cascadeStyle) {
		return ( type.isEntityType() || type.isCollectionType() ) &&
				( cascadeStyle == null || cascadeStyle.doCascade( cascadeAction ) );
	}

	@Override
	protected boolean isTooManyCollections() {
		return countCollectionPersisters( associations ) > 0;
	}

	@Override
	public String getComment() {
		return "load " + getPersister().getEntityName();
	}

}
