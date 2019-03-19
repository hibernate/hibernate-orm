/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import javax.persistence.metamodel.Type;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.spi.TableReferenceContributor;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.results.internal.domain.entity.EntityResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * Specialization of Navigable(Container) for any entity-valued Navigable
 *
 * @author Steve Ebersole
 */
public interface EntityValuedNavigable<J>
		extends EntityValuedExpressableType<J>, NavigableContainer<J>, TableReferenceContributor, AllowableParameterType<J> {
	@Override
	default Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.ENTITY;
	}

	EntityJavaDescriptor<J> getJavaTypeDescriptor();

	boolean isNullable();

	default String getMappedBy(){
		return null;
	}

	@Override
	default SqmNavigableReference createSqmExpression(SqmPath lhs, SqmCreationState creationState) {
		return new SqmEntityValuedSimplePath(
				lhs.getNavigablePath().append( getNavigableName() ),
				this,
				lhs
		);
	}

	@Override
	default DomainResult createDomainResult(
			NavigablePath navigablePath,
			String resultVariable,
			DomainResultCreationState creationState) {
		// its possible we have an implicit join and need to make sure the joined TableGroup gets created.
		// e.g., `select p.address from Person p`
		//
		// another option would be a specialized DotIdentifierConsumer for the select-clause

		// todo (6.0) : account for "shallow entity selection" (historical)

		if ( this instanceof SingularPersistentAttributeEntity ) {
			if ( navigablePath.getParent() != null ) {
				creationState.getFromClauseAccess().resolveTableGroup(
						navigablePath,
						np -> {
							final TableGroup lhsTableGroup = creationState.getFromClauseAccess().getTableGroup( navigablePath.getParent() );
							final TableGroupJoin tableGroupJoin = ( (SingularPersistentAttributeEntity) this ).createTableGroupJoin(
									navigablePath,
									lhsTableGroup,
									null,
									JoinType.INNER,
									creationState.determineLockMode( null ),
									creationState.getSqlAstCreationState()
							);
							lhsTableGroup.addTableGroupJoin( tableGroupJoin );
							return tableGroupJoin.getJoinedGroup();
						}
				);
			}
			else {
				// this means the TableGroup should already exist
				assert creationState.getFromClauseAccess().getTableGroup( navigablePath ) != null;
			}
		}

		return new EntityResultImpl(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	default <X> X as(Class<X> type) {
		return TreatAsHelper.handleEntityTreat( this, type );
	}

	@Override
	default int getNumberOfJdbcParametersNeeded() {
		return getEntityDescriptor().getIdentifierDescriptor().getNumberOfJdbcParametersNeeded();
	}
}
