/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.ForeignKey.ColumnMappings;
import org.hibernate.metamodel.model.domain.spi.JoinablePersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.NavigableEntityValued;
import org.hibernate.metamodel.queryable.spi.EntityValuedExpressableType;
import org.hibernate.metamodel.queryable.spi.JoinedTableGroupContext;
import org.hibernate.metamodel.queryable.spi.NavigableReferenceInfo;
import org.hibernate.metamodel.queryable.spi.SqlAliasBaseResolver;
import org.hibernate.metamodel.queryable.spi.TableGroupResolver;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.internal.NavigableSelection;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;


/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEntity<O,J>
		extends AbstractSingularPersistentAttribute<O,J>
		implements JoinablePersistentAttribute<O,J>, NavigableEntityValued<J> {

	private final SingularAttributeClassification classification;
	private final EntityTypeImplementor<J> entityPersister;
	private final ColumnMappings joinColumnMappings;

	private final NavigableRole navigableRole;


	public SingularPersistentAttributeEntity(
			ManagedTypeImplementor<O> declaringType,
			String name,
			PropertyAccess propertyAccess,
			SingularAttributeClassification classification,
			EntityValuedExpressableType<J> ormType,
			Disposition disposition,
			EntityTypeImplementor<J> entityPersister,
			ColumnMappings joinColumnMappings) {
		super( declaringType, name, propertyAccess, ormType, disposition, true );
		this.classification = classification;
		this.entityPersister = entityPersister;
		this.joinColumnMappings = joinColumnMappings;

		this.navigableRole = declaringType.getNavigableRole().append( name );
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		// assume ManyToOne for now
		return PersistentAttributeType.MANY_TO_ONE;
	}

	@Override
	public EntityTypeImplementor<J> getEntityPersister() {
		return entityPersister;
	}

	@Override
	public EntityValuedExpressableType<J> getType() {
		return (EntityValuedExpressableType<J>) super.getType();
	}

	@Override
	public String getJpaEntityName() {
		return entityPersister.getJpaEntityName();
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return entityPersister.getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return entityPersister.findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return entityPersister.findDeclaredNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		entityPersister.visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		entityPersister.visitNavigables( visitor );
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	public EntityTypeImplementor getAssociatedEntityPersister() {
		return entityPersister;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return classification;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeEntity([" + getAttributeTypeClassification().name() + "] " +
				getContainer().asLoggableText() + '.' + getAttributeName() +
				")";
	}

	@Override
	public String toString() {
		return asLoggableText();
	}

	public String getEntityName() {
		return entityPersister.getEntityName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEntity( this );
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression instanceof NavigableReference;
		return new NavigableSelection( (NavigableReference) selectedExpression, resultVariable );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return entityPersister.generateQueryResult(
				selectedExpression,
				resultVariable,
				columnReferenceSource,
				sqlSelectionResolver,
				creationContext
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceResolver,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public TableGroupJoin applyTableGroupJoin(
			NavigableReferenceInfo navigableReferenceInfo,
			SqmJoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext,
			TableGroupResolver tableGroupResolutionContext,
			SqlAliasBaseResolver sqlAliasBaseResolver) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public ColumnMappings getJoinColumnMappings() {
		return joinColumnMappings;
	}

	@Override
	public List<Navigable> getNavigables() {
		return entityPersister.getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return entityPersister.getDeclaredNavigables();
	}
}
