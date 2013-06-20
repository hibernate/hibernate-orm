/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.spi;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.walking.spi.AttributeDefinition;

/**
 * Represents an entity return value in the query results.  Not the same
 * as a result (column) in the JDBC ResultSet!
 *
 * @see Return
 *
 * @author Steve Ebersole
 */
public class EntityReturn extends AbstractFetchOwner implements Return, EntityReference, CopyableReturn {
	private final EntityPersister persister;
	private final EntityPersisterBasedSqlSelectFragmentResolver sqlSelectFragmentResolver;
	private IdentifierDescription identifierDescription;

	private final PropertyPath propertyPath;

	private final LockMode lockMode;

	/**
	 * Construct an {@link EntityReturn}.
	 *
	 * @param sessionFactory - the session factory.
	 * @param lockMode - the lock mode.
	 * @param entityName - the entity name.
	 */
	public EntityReturn(
			SessionFactoryImplementor sessionFactory,
			LockMode lockMode,
			String entityName) {
		super( sessionFactory );
		this.persister = sessionFactory.getEntityPersister( entityName );
		this.propertyPath = new PropertyPath( entityName );
		this.sqlSelectFragmentResolver = new EntityPersisterBasedSqlSelectFragmentResolver( (Queryable) persister );

		this.lockMode = lockMode;
	}

	protected EntityReturn(EntityReturn original, CopyContext copyContext) {
		super( original, copyContext );
		this.persister = original.persister;
		this.propertyPath = original.propertyPath;
		this.sqlSelectFragmentResolver = original.sqlSelectFragmentResolver;

		this.lockMode = original.lockMode;
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public EntityReference getEntityReference() {
		return this;
	}

	@Override
	public EntityPersister getEntityPersister() {
		return persister;
	}

	@Override
	public IdentifierDescription getIdentifierDescription() {
		return identifierDescription;
	}

	@Override
	public void validateFetchPlan(FetchStrategy fetchStrategy, AttributeDefinition attributeDefinition) {
	}

	@Override
	public EntityPersister retrieveFetchSourcePersister() {
		return getEntityPersister();
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public void injectIdentifierDescription(IdentifierDescription identifierDescription) {
		this.identifierDescription = identifierDescription;
	}

	@Override
	public String toString() {
		return "EntityReturn(" + persister.getEntityName() + ")";
	}

	@Override
	public EntityReturn makeCopy(CopyContext copyContext) {
		return new EntityReturn( this, copyContext );
	}

	@Override
	public SqlSelectFragmentResolver toSqlSelectFragmentResolver() {
		return sqlSelectFragmentResolver;
	}
}
