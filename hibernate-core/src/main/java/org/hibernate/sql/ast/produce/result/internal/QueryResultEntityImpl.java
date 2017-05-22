/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.consume.results.internal.EntityReturnInitializerImpl;
import org.hibernate.sql.ast.consume.results.internal.QueryResultAssemblerEntity;
import org.hibernate.sql.ast.consume.results.spi.EntityReferenceInitializer;
import org.hibernate.sql.ast.consume.results.spi.InitializerCollector;
import org.hibernate.sql.ast.consume.results.spi.InitializerParent;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionGroup;
import org.hibernate.sql.ast.produce.result.spi.EntityIdentifierReference;
import org.hibernate.sql.ast.produce.result.spi.QueryResultEntity;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;

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
			NavigablePath navigablePath) {
		super( expression, navigablePath );
		this.resultVariable = resultVariable;

		this.initializer = new EntityReturnInitializerImpl(
				this,
				sqlSelectionGroupMap,
				// root entity result cannot be shallow
				false
		);
		assembler = new QueryResultAssemblerEntity( this );
	}

	@Override
	public EntityReference getNavigableContainerReference() {
		return (EntityReference) super.getNavigableContainerReference();
	}

	@Override
	public EntityTypeImplementor getEntityMetadata() {
		return getNavigableContainerReference().getNavigable();
	}

	@Override
	public EntityIdentifierReference getIdentifierReference() {
		throw new NotYetImplementedException();
	}

	@Override
	public Expression getSelectedExpression() {
		return getNavigableContainerReference();
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public Class getReturnedJavaType() {
		return getEntityMetadata().getMappedClass();
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
