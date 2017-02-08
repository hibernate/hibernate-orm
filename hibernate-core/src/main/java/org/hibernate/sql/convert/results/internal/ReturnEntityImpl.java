/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.results.internal;

import java.util.Map;

import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.expression.domain.NavigablePath;
import org.hibernate.sql.convert.results.spi.EntityIdentifierReference;
import org.hibernate.sql.convert.results.spi.ReturnEntity;
import org.hibernate.sql.exec.results.process.internal.EntityReturnInitializerImpl;
import org.hibernate.sql.exec.results.process.internal.ReturnAssemblerEntity;
import org.hibernate.sql.exec.results.process.spi.EntityReferenceInitializer;
import org.hibernate.sql.exec.results.process.spi.InitializerCollector;
import org.hibernate.sql.exec.results.process.spi.InitializerParent;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionGroup;

/**
 * Standard ReturnEntity impl
 *
 * @author Steve Ebersole
 */
public class ReturnEntityImpl extends AbstractFetchParent implements ReturnEntity {
	private final Expression expression;
	private final EntityPersister entityPersister;
	private final String resultVariable;

	private final ReturnAssemblerEntity assembler;
	private final EntityReturnInitializerImpl initializer;

	public ReturnEntityImpl(
			Expression expression,
			EntityPersister entityPersister,
			String resultVariable,
			boolean isShallow,
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
				isShallow
		);
		assembler = new ReturnAssemblerEntity( this );
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
	public ReturnAssembler getReturnAssembler() {
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
