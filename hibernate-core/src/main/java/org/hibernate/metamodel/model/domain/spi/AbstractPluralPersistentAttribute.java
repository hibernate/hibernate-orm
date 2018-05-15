/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.LoadingCollectionEntry;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * todo (6.0) : potentially split this single impl into specific impls for each "collection nature":
 * 		`collection`:: {@link BagPersistentAttribute}
 * 		`list`:: {@link ListPersistentAttribute}
 * 		`set`:: {@link SetPersistentAttribute}
 * 		`map`:: {@link MapPersistentAttribute}
 * 		`bag`:: synonym for `collection`
 * 		`idbag`:: not sure yet
 *
 * @author Steve Ebersole
 */
public class AbstractPluralPersistentAttribute<O,C,E> extends AbstractPersistentAttribute<O,C> implements PluralPersistentAttribute<O,C,E> {
	private static final Logger log = Logger.getLogger( AbstractPluralPersistentAttribute.class );

	private static final Object NOT_NULL_COLLECTION = new MarkerObject( "NOT NULL COLLECTION" );

	private final PersistentCollectionDescriptor collectionDescriptor;
	private final FetchStrategy fetchStrategy;

	private int stateArrayPosition;

	@SuppressWarnings("unchecked")
	public AbstractPluralPersistentAttribute(
			PersistentCollectionDescriptor collectionDescriptor,
			Property bootProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext) {
		super( collectionDescriptor.getContainer(), bootProperty, propertyAccess );

		final Collection bootCollectionDescriptor = (Collection) bootProperty.getValue();

		this.collectionDescriptor = collectionDescriptor;

		creationContext.registerCollectionDescriptor( collectionDescriptor, bootCollectionDescriptor );

		this.fetchStrategy = DomainModelHelper.determineFetchStrategy( bootCollectionDescriptor );
	}

	@Override
	public PersistentCollectionDescriptor getPersistentCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public void collectNonNullableTransientEntities(
			Object value,
			ForeignKeys.Nullifier nullifier,
			NonNullableTransientDependencies nonNullableTransientEntities,
			SharedSessionContractImplementor session) {
		// todo (6.0) : prior versions essentially skipped collections when doing this process
		//		verify this is actually correct... the collection can hold non-null, transient entities as
		//		well and not cascade to them, seems like it should add them.  Did previous versions handle
		// 		the collection values differently?
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.PLURAL_ATTRIBUTE;
	}

	@Override
	public ManagedTypeDescriptor getContainer() {
		return getPersistentCollectionDescriptor().getContainer();
	}

	@Override
	public SimpleTypeDescriptor getElementType() {
		return collectionDescriptor.getElementDescriptor().getDomainTypeDescriptor();
	}

	@Override
	public SimpleTypeDescriptor<?> getValueGraphType() {
		return getElementType();
	}

	@Override
	public SimpleTypeDescriptor<?> getKeyGraphType() {
		return collectionDescriptor.getIndexDescriptor() == null
				? null
				: collectionDescriptor.getIndexDescriptor().getDomainTypeDescriptor();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public boolean isAssociation() {
		return getPersistentAttributeType() == PersistentAttributeType.ONE_TO_MANY
				|| getPersistentAttributeType() == PersistentAttributeType.MANY_TO_MANY;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Class getBindableJavaType() {
		return getPersistentCollectionDescriptor().getElementDescriptor().getJavaType();
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public void visitColumns(
			BiConsumer action, Clause clause, TypeConfiguration typeConfiguration) {

	}

	@Override
	public boolean isInsertable() {
		return false;
	}

	@Override
	public boolean isUpdatable() {
		return false;
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public void setStateArrayPosition(int position) {
		this.stateArrayPosition = position;
	}

	@Override
	public CollectionMutabilityPlan getMutabilityPlan() {
		return getCollectionDescriptor().getMutabilityPlan();
	}

	@Override
	public Navigable findNavigable(String navigableName) {
		return getPersistentCollectionDescriptor().findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getPersistentCollectionDescriptor().visitNavigables( visitor );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return collectionDescriptor.getNavigableRole();
	}

	@Override
	public String asLoggableText() {
		return toString();
	}

	@Override
	public CollectionJavaDescriptor getJavaTypeDescriptor() {
		return collectionDescriptor.getJavaTypeDescriptor();
	}

	@Override
	public SqmPluralAttributeReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmPluralAttributeReference( containerReference, this, creationContext );
	}

	@Override
	public boolean isIncludedInDirtyChecking() {
		// todo (6.0) : depends how we want to handle dirty collections marking container as dirty
		//		this is only important for versioned entities
		//
		// for now return false
		return false;
	}

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState, DomainResultCreationContext creationContext) {
		return getPersistentCollectionDescriptor().createDomainResult(
				navigableReference,
				resultVariable,
				creationState, creationContext
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return getPersistentCollectionDescriptor().generateFetch(
				fetchParent,
				fetchTiming,
				selected,
				lockMode,
				resultVariable,
				creationState,
				creationContext
		);
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.emptyList();
	}

	@Override
	public Object hydrate(Object jdbcValues, SharedSessionContractImplementor session) {
		if ( jdbcValues == null ) {
			return null;
		}

		return NOT_NULL_COLLECTION;
	}

	@Override
	public Object resolveHydratedState(
			Object hydratedForm,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		// todo (6.0) : use the collection-key descriptor to resolve the hydrated key value...
		//
		// for now we hack it in such a way that will work for all simple identifiers

		final IdentifiableTypeDescriptor ownerDescriptor = (IdentifiableTypeDescriptor) getContainer();
		final EntityIdentifierSimple identifierDescriptor = (EntityIdentifierSimple) ownerDescriptor.getHierarchy().getIdentifierDescriptor();

		final Object key = identifierDescriptor.asAttribute( identifierDescriptor.getJavaType() ).getPropertyAccess().getGetter().get( containerInstance );

		PersistentCollectionDescriptor collectionDescriptor = getPersistentCollectionDescriptor();

		final CollectionKey collectionKey = new CollectionKey( collectionDescriptor, key );

		final PersistenceContext persistenceContext = session.getPersistenceContext();

		// check if collection is currently being loaded
		final LoadingCollectionEntry loadingCollectionEntry = persistenceContext.getLoadContexts()
				.findLoadingCollectionEntry( collectionKey );

		PersistentCollection collection = loadingCollectionEntry == null ? null : loadingCollectionEntry.getCollectionInstance();

		if ( collection == null ) {
			// check if it is already completely loaded, but unowned
			collection = persistenceContext.useUnownedCollection( collectionKey );

			if ( collection == null ) {

				collection = persistenceContext.getCollection( collectionKey );

				if ( collection == null ) {
					// create a new collection wrapper, to be initialized later
					collection = collectionDescriptor.instantiateWrapper( session, key );
					collection.setOwner( containerInstance );

					persistenceContext.addUninitializedCollection( collectionDescriptor, collection, key );

					// todo (6.0) (fetching) : handle fetching or not
					//
					// for now. lazy

//					boolean eager = overridingEager != null ? overridingEager : ! isLazy();
//					if ( initializeImmediately() ) {
//						session.initializeCollection( collection, false );
//					}
//					else if ( eager ) {
//						persistenceContext.addNonLazyCollection( collection );
//					}
//
//					if ( hasHolder() ) {
//						session.getPersistenceContext().addCollectionHolder( collection );
//					}
				}

			}

			if ( log.isTraceEnabled() ) {
				log.tracef(
						"Created collection wrapper: %s",
						MessageHelper.collectionInfoString(
								collectionDescriptor,
								collection,
								key,
								session
						)
				);
			}

		}

		collection.setOwner( containerInstance );
		collection.setCurrentSession( session );

		return collection.getValue();
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
//		throw new NotYetImplementedFor6Exception();
		// can't just return null here, since that would
		// cause an owning component to become null
		return NOT_NULL_COLLECTION;
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isDirty(Object originalValue, Object currentValue, SharedSessionContractImplementor session) {
		return !getJavaTypeDescriptor().areEqual( originalValue, currentValue );
	}

	@Override
	public void visitFetchables(Consumer consumer) {
		getPersistentCollectionDescriptor().visitFetchables( consumer );
	}

	@Override
	public String toString() {
		return "PluralPersistentAttribute(" + getNavigableRole() + ")";
	}

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception();

		// todo (6.0) : implement - need to build CollectionTableGroup, etc

//		DomainResult collectionKeyResult = getCollectionDescriptor().getCollectionKeyDescriptor().createCollectionResult(
//				creationState.
//		);
//		return new CollectionResultImpl(
//				this,
//				new NavigablePath( getNavigableRole().getFullPath() ),
//				resultVariable,
//				creationState.determineLockMode( resultVariable ),
//				createDomainResult(  ),
//		);
	}

	@Override
	public DomainType getAttributeType() {
		return null;
	}
}
