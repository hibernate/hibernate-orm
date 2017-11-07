/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.EntityQueryResult;
import org.hibernate.sql.results.spi.EntitySqlSelectionMappings;
import org.hibernate.sql.results.spi.InitializerCollector;
import org.hibernate.sql.results.spi.QueryResultAssembler;
import org.hibernate.sql.results.spi.QueryResultCreationContext;

/**
 * Standard ReturnEntity impl
 *
 * @author Steve Ebersole
 */
public class EntityQueryResultImpl extends AbstractFetchParent implements EntityQueryResult {
	private final String resultVariable;

	private final EntityQueryResultAssembler assembler;
	private final EntityRootInitializer initializer;

	public EntityQueryResultImpl(
			EntityValuedNavigable navigable,
			String resultVariable,
			EntitySqlSelectionMappings sqlSelectionMappings,
			LockMode lockMode,
			NavigablePath navigablePath,
			QueryResultCreationContext creationContext) {
		super( navigable, navigablePath );
		this.resultVariable = resultVariable;

		// todo (6.0) : the difficulty here is a chicken-egg problem...
		//
		this.initializer = new EntityRootInitializer(
				navigable.getEntityDescriptor(),
				sqlSelectionMappings,
				lockMode,
				creationContext.shouldCreateShallowEntityResult()
		);

		this.assembler = new EntityQueryResultAssembler(
				getNavigable().getJavaTypeDescriptor(),
				initializer
		);
	}

	@Override
	public EntityValuedNavigable getFetchContainer() {
		return (EntityValuedNavigable) super.getFetchContainer();
	}

	public EntityValuedNavigable getNavigable() {
		return getFetchContainer();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public EntityDescriptor getEntityDescriptor() {
		return getNavigable().getEntityDescriptor();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		collector.addInitializer( initializer );
		registerFetchInitializers( initializer, collector );
	}
}
