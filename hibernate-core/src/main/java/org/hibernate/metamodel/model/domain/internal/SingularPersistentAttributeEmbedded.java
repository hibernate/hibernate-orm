/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEmbedded;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EmbeddableValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.domain.embedded.CompositeFetchImpl;
import org.hibernate.sql.results.internal.domain.embedded.CompositeResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.internal.EmbeddedMutabilityPlanImpl;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEmbedded<O,J>
		extends AbstractNonIdSingularPersistentAttribute<O,J>
		implements EmbeddedValuedNavigable<J>, Fetchable<J> {

	private final EmbeddedTypeDescriptor<J> embeddedDescriptor;

	public SingularPersistentAttributeEmbedded(
			ManagedTypeDescriptor<O> runtimeModelContainer,
			PersistentAttributeMapping bootModelAttribute,
			PropertyAccess propertyAccess,
			Disposition disposition,
			RuntimeModelCreationContext context) {
		super( runtimeModelContainer, bootModelAttribute, propertyAccess, disposition );

		final Component embeddedMapping = (Component) bootModelAttribute.getValueMapping();
		this.embeddedDescriptor = embeddedMapping.makeRuntimeDescriptor(
				runtimeModelContainer,
				bootModelAttribute.getName(),
				disposition,
				context
		);

		instantiationComplete( bootModelAttribute, context );
	}


	@Override
	public ManagedTypeDescriptor<O> getContainer() {
		return super.getContainer();
	}

	@Override
	public SimpleTypeDescriptor<?> getValueGraphType() {
		return getEmbeddedDescriptor();
	}

	@Override
	public SimpleTypeDescriptor<?> getKeyGraphType() {
		return null;
	}

	@Override
	public EmbeddedTypeDescriptor<J> getEmbeddedDescriptor() {
		return embeddedDescriptor;
	}

	@Override
	public EmbeddableJavaDescriptor<J> getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor<J>) super.getJavaTypeDescriptor();
	}

	@Override
	public SimpleTypeDescriptor<J> getType() {
		return getEmbeddedDescriptor();
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public String asLoggableText() {
		return toString();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N> Navigable<N> findNavigable(String navigableName) {
		return this.getEmbeddedDescriptor().findNavigable( navigableName );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return embeddedDescriptor.getNavigableRole();
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEmbedded( this );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		embeddedDescriptor.visitNavigables( visitor );
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmSingularAttributeReferenceEmbedded( containerReference, this, creationContext );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode, String resultVariable,
			DomainResultCreationState creationState, DomainResultCreationContext creationContext) {
		final Stack<NavigableReference> navigableReferenceStack = creationState.getNavigableReferenceStack();

		if ( navigableReferenceStack.depth() > creationContext.getSessionFactory().getSessionFactoryOptions().getMaximumFetchDepth() ) {
			selected = false;
		}

		final NavigableContainerReference parentReference = (NavigableContainerReference) navigableReferenceStack.getCurrent();

		final NavigablePath navigablePath = fetchParent.getNavigablePath().append( getNavigableName() );

		// if there is an existing NavigableReference this fetch can use, use it.  otherwise create one
		NavigableReference navigableReference = parentReference.findNavigableReference( getNavigableName() );
		if ( navigableReference == null ) {
			navigableReference = new EmbeddableValuedNavigableReference(
					parentReference,
					this,
					navigablePath,
					lockMode
			);
		}

		creationState.getNavigableReferenceStack().push( navigableReference );
		creationState.getColumnReferenceQualifierStack().push( navigableReference.getColumnReferenceQualifier() );

		try {
			return new CompositeFetchImpl(
					fetchParent,
					this,
					fetchTiming,
					creationState
			);
		}
		finally {
			creationState.getColumnReferenceQualifierStack().pop();
			creationState.getNavigableReferenceStack().pop();
		}
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		getEmbeddedDescriptor().visitFetchables( fetchableConsumer );
	}

	private final FetchStrategy mappedFetchStrategy = new FetchStrategy(
			FetchTiming.IMMEDIATE,
			FetchStyle.JOIN
	);

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return mappedFetchStrategy;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"%s(%s)@%s",
				getClass().getSimpleName(),
				getNavigableRole().getFullPath(),
				System.identityHashCode( this )
		);
	}

	@Override
	public List<Column> getColumns() {
		return getEmbeddedDescriptor().collectColumns();
	}

	@Override
	public Object resolveHydratedState(
			Object hydratedForm,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		final J instance = embeddedDescriptor.instantiate( session );
		final Object[] hydratedValues = (Object[]) hydratedForm;
		embeddedDescriptor.visitStateArrayContributors(
				contributor -> {
					final Object subHydratedForm = hydratedValues[ contributor.getStateArrayPosition() ];
					final Object subResolvedForm = contributor.resolveHydratedState(
							subHydratedForm,
							executionContext,
							session,
							containerInstance
					);
					hydratedValues[ contributor.getStateArrayPosition() ] = subResolvedForm;
				}
		);
		embeddedDescriptor.setPropertyValues( instance, (Object[]) hydratedForm );
		return instance;
	}

	@Override
	public int getNumberOfJdbcParametersForRestriction() {
		return getColumns().size();
	}

	@Override
	public AllowableParameterType resolveTemporalPrecision(TemporalType temporalType, TypeConfiguration typeConfiguration) {
		throw new ParameterMisuseException( "Cannot apply temporal precision to embeddable value" );
	}

	@Override
	public void collectNonNullableTransientEntities(
			Object value,
			ForeignKeys.Nullifier nullifier,
			NonNullableTransientDependencies nonNullableTransientEntities,
			SharedSessionContractImplementor session) {
		if ( value == null ) {
			return;
		}

		final Object[] subValues;
		if ( value instanceof Object[] ) {
			subValues = (Object[]) value;
		}
		else if ( getEmbeddedDescriptor().getJavaTypeDescriptor().isInstance( value ) ) {
			subValues = getEmbeddedDescriptor().getPropertyValues( value );
		}
		else {
			throw new HibernateException( "Unexpected value : " + value );
		}

		if ( subValues.length == 0 ) {
			return;
		}

		getEmbeddedDescriptor().visitStateArrayContributors(
				new Consumer<StateArrayContributor<?>>() {
					int i = 0;

					@Override
					public void accept(StateArrayContributor<?> stateArrayContributor) {
						final Object subValue = subValues[i++];

						if ( subValue == null ) {
							return;
						}

						stateArrayContributor.collectNonNullableTransientEntities(
								subValue,
								nullifier,
								nonNullableTransientEntities,
								session
						);
					}
				}
		);
	}

	@Override
	public void visitJdbcTypes(
			Consumer<SqlExpressableType> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		getEmbeddedDescriptor().visitJdbcTypes( action, clause, typeConfiguration );
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		final Object[] values = getEmbeddedDescriptor().getPropertyValues( value );
		getEmbeddedDescriptor().visitStateArrayContributors(
				contributor -> {
					final int index = contributor.getStateArrayPosition();
					values[index] = contributor.unresolve( values[index], session );
				}
		);

		return values;
	}

	@Override
	public boolean isInsertable() {
		for ( StateArrayContributor contributor : getEmbeddedDescriptor().getStateArrayContributors() ) {
			if ( contributor.isInsertable() ) {
				// it is illegal to mix insertable and non-insertable
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isUpdatable() {
		for ( StateArrayContributor contributor : getEmbeddedDescriptor().getStateArrayContributors() ) {
			if ( contributor.isUpdatable() ) {
				// it is illegal to mix updatable and non-updatable
				return true;
			}
		}
		return false;
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		final Object[] values = (Object[]) value;
		getEmbeddedDescriptor().visitStateArrayContributors(
				contributor -> {
					if ( clause.getInclusionChecker().test( contributor ) ) {
						contributor.dehydrate(
								values[contributor.getStateArrayPosition()],
								jdbcValueCollector,
								clause,
								session
						);
					}
				}
		);
	}

	@Override
	public boolean isDirty(Object originalValue, Object currentValue, SharedSessionContractImplementor session) {
		if ( originalValue == currentValue ) {
			return false;
		}

		final Object[] originalValues = getEmbeddedDescriptor().getPropertyValues( originalValue );
		final Object[] currentValues = getEmbeddedDescriptor().getPropertyValues( currentValue );
		for ( StateArrayContributor contributor : getEmbeddedDescriptor().getStateArrayContributors() ) {
			final int index = contributor.getStateArrayPosition();
			if ( contributor.isDirty( originalValues[index], currentValues[index], session ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void instantiationComplete(
			PersistentAttributeMapping bootModelAttribute,
			RuntimeModelCreationContext context) {
		super.instantiationComplete( bootModelAttribute, context );

		// todo (6.0) : determine mutability plan based on JTD & @Immutable
		//		for now just use the JTD MP

		this.mutabilityPlan = new EmbeddedMutabilityPlanImpl( embeddedDescriptor );
	}

	@Override
	public List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier, SqlAstCreationContext resolutionContext) {
		final List<ColumnReference> columnReferences = new ArrayList<>();
		getEmbeddedDescriptor().visitStateArrayContributors(
				contributor ->
						columnReferences.addAll( contributor.resolveColumnReferences(
								qualifier,
								resolutionContext
						) )
		);
		return columnReferences;
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action, Clause clause, TypeConfiguration typeConfiguration) {
		getEmbeddedDescriptor().visitStateArrayContributors(
				contributor -> contributor.visitColumns( action, clause, typeConfiguration )

		);
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new CompositeResultImpl( resultVariable, getEmbeddedDescriptor(), creationState );
	}
}
