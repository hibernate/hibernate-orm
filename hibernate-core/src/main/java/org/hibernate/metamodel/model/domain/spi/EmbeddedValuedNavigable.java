/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.Type;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.produce.metamodel.spi.EmbeddedValuedExpressableType;
import org.hibernate.sql.results.internal.domain.embedded.CompositeResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * Describes parts of the domain model that can be composite values.
 *
 * @author Steve Ebersole
 */
public interface EmbeddedValuedNavigable<J> extends EmbeddedValuedExpressableType<J>, NavigableContainer<J> {
	@Override
	EmbeddedContainer getContainer();

	EmbeddedTypeDescriptor<J> getEmbeddedDescriptor();

	@Override
	EmbeddableJavaDescriptor<J> getJavaTypeDescriptor();

	@Override
	default Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.EMBEDDABLE;
	}

	@Override
	default SqmNavigableReference createSqmExpression(SqmPath lhs, SqmCreationState creationState) {
		return new SqmEmbeddedValuedSimplePath(
				lhs.getNavigablePath().append( getNavigableName() ),
				this,
				lhs,
				creationState.getCreationContext().getNodeBuilder()
		);
	}

	@Override
	default DomainResult createDomainResult(
			NavigablePath navigablePath,
			String resultVariable,
			DomainResultCreationState creationState) {
		creationState.getFromClauseAccess().resolveTableGroup(
				navigablePath,
				np -> creationState.getFromClauseAccess().getTableGroup( navigablePath.getParent() )
		);

		return new CompositeResultImpl(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	default int getNumberOfJdbcParametersNeeded() {
		return getEmbeddedDescriptor().getNumberOfJdbcParametersNeeded();
	}

	@Override
	default <X> X as(Class<X> type) {
		return TreatAsHelper.handleEmbeddedTreat( this, type );
	}
}
