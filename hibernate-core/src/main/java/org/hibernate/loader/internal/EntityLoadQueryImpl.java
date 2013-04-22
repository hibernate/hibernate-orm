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
import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.spi.JoinableAssociation;
import org.hibernate.loader.spi.LoadQueryAliasResolutionContext;

/**
 * Represents an load query for fetching an entity, used for generating SQL.
 *
 * This code is based on the SQL generation code originally in
 * org.hibernate.loader.EntityJoinWalker.
 *
 * @author Gavin King
 * @author Gail Badner
 */
public class EntityLoadQueryImpl extends AbstractEntityLoadQueryImpl {

	public EntityLoadQueryImpl(
			EntityReturn entityReturn,
			List<JoinableAssociation> associations) throws MappingException {
		super( entityReturn, associations );
	}

	public String generateSql(
			String[] uniqueKey,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryAliasResolutionContext aliasResolutionContext) {
		StringBuilder whereCondition = whereString( resolveEntityReturnAlias( aliasResolutionContext ), uniqueKey, batchSize )
				//include the discriminator and class-level where, but not filters
				.append( getPersister().filterFragment( resolveEntityReturnAlias( aliasResolutionContext ), Collections.EMPTY_MAP ) );
		return generateSql( whereCondition.toString(), "",  new LockOptions().setLockMode( lockMode ), factory, aliasResolutionContext );
	}

	protected String getComment() {
		return "load " + getPersister().getEntityName();
	}
}