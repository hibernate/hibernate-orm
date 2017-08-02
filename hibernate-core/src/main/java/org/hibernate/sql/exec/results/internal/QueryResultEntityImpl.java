/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal;

import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;
import org.hibernate.sql.exec.results.spi.InitializerEntity;
import org.hibernate.sql.exec.results.spi.InitializerCollector;
import org.hibernate.sql.exec.results.spi.InitializerParent;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.QueryResultCreationContext;
import org.hibernate.sql.exec.results.spi.QueryResultEntity;
import org.hibernate.sql.exec.results.spi.SqlSelectionGroup;

/**
 * Standard ReturnEntity impl
 *
 * @author Steve Ebersole
 */
public class QueryResultEntityImpl extends AbstractFetchParent implements QueryResultEntity {
	private final EntityValuedNavigable navigable;
	private final String resultVariable;

	private final QueryResultAssemblerEntity assembler;
	private final EntityReturnInitializerImpl initializer;

	public QueryResultEntityImpl(
			EntityValuedNavigable navigable,
			String resultVariable,
			Map<PersistentAttribute, SqlSelectionGroup> sqlSelectionGroupMap,
			NavigablePath navigablePath,
			QueryResultCreationContext creationContext) {
		super( null, navigablePath );
		this.navigable = navigable;
		this.resultVariable = resultVariable;

		this.initializer = new EntityReturnInitializerImpl(
				this,
				sqlSelectionGroupMap,
				creationContext.shouldCreateShallowEntityResult()
		);

		this.assembler = new QueryResultAssemblerEntity(
				getType().getJavaTypeDescriptor(),
				initializer
		);
	}

	public EntityValuedNavigable getNavigable() {
		return navigable;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public EntityReference getNavigableContainerReference() {
		return (EntityReference) super.getNavigableContainerReference();
	}

	@Override
	public EntityDescriptor getEntityDescriptor() {
		return ( (EntityValuedNavigable) getNavigableContainerReference().getNavigable() ).getEntityDescriptor();
	}

	@Override
	public QueryResultAssembler getResultAssembler() {
		return assembler;
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		collector.addInitializer( initializer );
		addFetchInitializers( collector );
	}

	@Override
	public InitializerEntity getInitializer() {
		return initializer;
	}

	@Override
	public InitializerParent getInitializerParentForFetchInitializers() {
		return initializer;
	}

	@Override
	public EntityValuedExpressableType getType() {
		return getNavigable();
	}
}
