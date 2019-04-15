/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.property.access.internal.PropertyAccessMapImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyEmbeddedImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.SqmPathRegistry;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierCompositeNonAggregatedImpl<O,J>
		implements EntityIdentifierCompositeNonAggregated<O,J> {
	// todo : IdClass handling eventually

	private final EntityHierarchy runtimeModelHierarchy;
	private final EmbeddedTypeDescriptor<J> embeddedDescriptor;
	private final PropertyAccess propertyAccess;

	@SuppressWarnings("WeakerAccess")
	public EntityIdentifierCompositeNonAggregatedImpl(
			EntityHierarchy runtimeModelHierarchy,
			EmbeddedTypeDescriptor<J> embeddedDescriptor,
			@SuppressWarnings("unused") EmbeddedValueMapping bootMapping) {
		this.runtimeModelHierarchy = runtimeModelHierarchy;
		this.embeddedDescriptor = embeddedDescriptor;

		if ( embeddedDescriptor.getRepresentationStrategy() instanceof PropertyAccessStrategyMapImpl ) {
			assert embeddedDescriptor.getRepresentationStrategy().getMode() == RepresentationMode.MAP;
			this.propertyAccess = new PropertyAccessMapImpl(
					(PropertyAccessStrategyMapImpl) embeddedDescriptor.getRepresentationStrategy(),
					embeddedDescriptor.getNavigableName()
			);
		}
		else {
			this.propertyAccess = PropertyAccessStrategyEmbeddedImpl.INSTANCE.buildPropertyAccess(
					embeddedDescriptor.getJavaType(),
					embeddedDescriptor.getNavigableName()
			);
		}
	}

	@Override
	public EmbeddedTypeDescriptor<J> getEmbeddedDescriptor() {
		return embeddedDescriptor;
	}

	@Override
	public EmbeddedTypeDescriptor<O> getContainer() {
		//noinspection unchecked
		return (EmbeddedTypeDescriptor<O>) runtimeModelHierarchy.getRootEntityType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return getEmbeddedDescriptor().getNavigableRole();
	}

	@Override
	public EmbeddableJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEmbeddedDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public String asLoggableText() {
		return "IdentifierCompositeNonAggregated(" + getContainer().asLoggableText() + ")";
	}

	@Override
	public List<Column> getColumns() {
		return getEmbeddedDescriptor().collectColumns();
	}

	@Override
	public boolean canContainSubGraphs() {
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SingularPersistentAttribute asAttribute(Class javaType) {
		// todo (6.0) : see not on super.
		//		for now throw the exception as JPA defines
		throw new IllegalArgumentException(
				"Non-aggregated composite ID cannot be represented as a single attribute"
		);
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEmbeddedDescriptor().findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getEmbeddedDescriptor().visitNavigables( visitor );
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		getEmbeddedDescriptor().visitFetchables( fetchableConsumer );
	}

	@Override
	public EmbeddedTypeDescriptor<J> getNavigableType() {
		return getEmbeddedDescriptor();
	}

	@Override
	public IdentifierGenerator getIdentifierValueGenerator() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		final Object[] values = (Object[]) value;
		getEmbeddedDescriptor().visitStateArrayContributors(
				contributor -> contributor.dehydrate(
						values[contributor.getStateArrayPosition()],
						jdbcValueCollector,
						clause,
						session
				)
		);
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmPath lhs,
			SqmCreationState creationState) {
		final NavigablePath navigablePath = lhs.getNavigablePath().append( getNavigableName() );
		final SqmPathRegistry pathRegistry = creationState.getProcessingStateStack().getCurrent().getPathRegistry();
		//noinspection unchecked
		return (SqmNavigableReference) pathRegistry.resolvePath(
				navigablePath,
				np -> new SqmEmbeddedValuedSimplePath(
						navigablePath,
						this,
						lhs,
						creationState.getCreationContext().getNodeBuilder()
				)
		);
	}

	@Override
	public IdentifierValue getUnsavedValue() {
		return null;
	}

	@Override
	public SqmAttributeJoin createSqmJoin(
			SqmFrom lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		//noinspection unchecked
		return new SqmSingularJoin(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
		);
	}

	@Override
	public DomainType<J> getAttributeType() {
		return embeddedDescriptor;
	}

	@Override
	public SimpleTypeDescriptor<?> getKeyGraphType() {
		return null;
	}

	@Override
	public SimpleTypeDescriptor<?> getValueGraphType() {
		return embeddedDescriptor;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
