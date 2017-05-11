/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import java.util.Map;

import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.produce.result.spi.EntityIdentifierReference;
import org.hibernate.sql.ast.produce.result.spi.QueryResultEntity;
import org.hibernate.sql.ast.consume.results.internal.EntityReturnInitializerImpl;
import org.hibernate.sql.ast.consume.results.internal.QueryResultAssemblerEntity;
import org.hibernate.sql.ast.consume.results.spi.EntityReferenceInitializer;
import org.hibernate.sql.ast.consume.results.spi.InitializerCollector;
import org.hibernate.sql.ast.consume.results.spi.InitializerParent;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionGroup;

/**
 * Standard ReturnEntity impl
 *
 * @author Steve Ebersole
 */
public class QueryResultEntityImpl extends AbstractFetchParent implements QueryResultEntity {
	private final Expression expression;
	private final EntityPersister entityPersister;
	private final String resultVariable;

	private final QueryResultAssemblerEntity assembler;
	private final EntityReturnInitializerImpl initializer;

	public QueryResultEntityImpl(
			Expression expression,
			EntityPersister entityPersister,
			String resultVariable,
			Map<PersistentAttribute, SqlSelectionGroup> sqlSelectionGroupMap,
			NavigablePath navigablePath,
			String tableGroupUid) {
		super( navigablePath, tableGroupUid );
		this.expression = expression;
		this.entityPersister = entityPersister;
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
	public EntityPersister getEntityPersister() {
		return entityPersister;
	}

	@Override
	public EntityIdentifierReference getIdentifierReference() {
		throw new NotYetImplementedException();
	}

	@Override
	public Expression getSelectedExpression() {
		return expression;
	}

	@Override
	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public Class getReturnedJavaType() {
		return entityPersister.getMappedClass();
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
