/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.spi.LoadQueryAliasResolutionContext;

/**
 * A walker for loaders that fetch entities
 *
 * @see org.hibernate.loader.entity.EntityLoader
 * @author Gavin King
 */
public class EntityLoadQueryImpl extends AbstractEntityLoadQueryImpl {

	public EntityLoadQueryImpl(
			final SessionFactoryImplementor factory,
			EntityReturn entityReturn,
			List<JoinableAssociationImpl> associations) throws MappingException {
		super( factory, entityReturn, associations );
	}

	public String generateSql(
			String[] uniqueKey,
			int batchSize,
			LockMode lockMode,
			LoadQueryAliasResolutionContext aliasResolutionContext) {
		StringBuilder whereCondition = whereString( getAlias( aliasResolutionContext ), uniqueKey, batchSize )
				//include the discriminator and class-level where, but not filters
				.append( getPersister().filterFragment( getAlias( aliasResolutionContext ), Collections.EMPTY_MAP ) );
		return generateSql( whereCondition.toString(), "",  new LockOptions().setLockMode( lockMode ), aliasResolutionContext );
	}

	public String getComment() {
		return "load " + getPersister().getEntityName();
	}

}