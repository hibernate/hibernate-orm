/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal.select;

import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;
import org.hibernate.sql.ast.tree.spi.select.EntityIdentifierReference;
import org.hibernate.sql.ast.tree.spi.select.QueryResultCreationContext;
import org.hibernate.sql.ast.tree.spi.select.QueryResultEntity;
import org.hibernate.sql.exec.results.internal.EntityReturnInitializerImpl;
import org.hibernate.sql.exec.results.internal.QueryResultAssemblerEntity;
import org.hibernate.sql.exec.results.spi.EntityReferenceInitializer;
import org.hibernate.sql.exec.results.spi.InitializerCollector;
import org.hibernate.sql.exec.results.spi.InitializerParent;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.SqlSelectionGroup;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Standard ReturnEntity impl
 *
 * @author Steve Ebersole
 */
public class QueryResultEntityImpl extends AbstractFetchParent implements QueryResultEntity {
	private final String resultVariable;

	private final QueryResultAssemblerEntity assembler;
	private final EntityReturnInitializerImpl initializer;

	public QueryResultEntityImpl(
			EntityReference expression,
			String resultVariable,
			Map<PersistentAttribute, SqlSelectionGroup> sqlSelectionGroupMap,
			NavigablePath navigablePath,
			QueryResultCreationContext creationContext) {
		super( expression, navigablePath );
		this.resultVariable = resultVariable;

		this.initializer = new EntityReturnInitializerImpl(
				this,
				sqlSelectionGroupMap,
				creationContext.shouldCreateShallowEntityResult()
		);
		assembler = new QueryResultAssemblerEntity(
				getJavaTypeDescriptor(),
				getInitializer()
		);
	}

	public EntityReference getSelectedExpression() {
		return getNavigableContainerReference();
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
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getSelectedExpression().getType().getJavaTypeDescriptor();
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
	public EntityReferenceInitializer getInitializer() {
		return initializer;
	}

	@Override
	public InitializerParent getInitializerParentForFetchInitializers() {
		return initializer;
	}
}
