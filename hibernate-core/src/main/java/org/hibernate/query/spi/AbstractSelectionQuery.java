/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.sql.Types;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ScrollMode;
import org.hibernate.UnknownProfileException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.query.BindableType;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.internal.ScrollableResultsIterator;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.criteria.CompoundSelection;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Spliterators.spliteratorUnknownSize;
import static org.hibernate.CacheMode.fromJpaModes;
import static org.hibernate.FlushMode.fromJpaFlushMode;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_RETRIEVE_MODE;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_STORE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_MODE;
import static org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION;
import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;
import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_LOCKING;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;
import static org.hibernate.query.sqm.internal.SqmUtil.isHqlTuple;
import static org.hibernate.query.sqm.internal.SqmUtil.isSelectionAssignableToResultType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSelectionQuery<R>
		extends AbstractCommonQueryContract
		implements SelectionQuery<R>, DomainQueryExecutionContext {
	/**
	 * The value used for {@link #getQueryString} for Criteria-based queries
	 */
	public static final String CRITERIA_HQL_STRING = "<criteria>";

	private Callback callback;

	public AbstractSelectionQuery(SharedSessionContractImplementor session) {
		super( session );
	}

	protected TupleMetadata buildTupleMetadata(SqmStatement<?> statement, Class<R> resultType) {
		if ( statement instanceof SqmSelectStatement<?> ) {
			final SqmSelectStatement<?> select = (SqmSelectStatement<?>) statement;
			final List<SqmSelection<?>> selections =
					select.getQueryPart().getFirstQuerySpec().getSelectClause()
							.getSelections();
			return isTupleMetadataRequired( resultType, selections.get(0) )
					? getTupleMetadata( selections )
					: null;
		}
		else {
			return null;
		}
	}

	private static <R> boolean isTupleMetadataRequired(Class<R> resultType, SqmSelection<?> selection) {
		return isHqlTuple( selection )
			|| !isInstantiableWithoutMetadata( resultType )
				&& !isSelectionAssignableToResultType( selection, resultType );
	}

	private TupleMetadata getTupleMetadata(List<SqmSelection<?>> selections) {
		if ( getQueryOptions().getTupleTransformer() == null ) {
			return new TupleMetadata( buildTupleElementArray( selections ), buildTupleAliasArray( selections ) );
		}
		else {
			throw new IllegalArgumentException(
					"Illegal combination of Tuple resultType and (non-JpaTupleBuilder) TupleTransformer: "
							+ getQueryOptions().getTupleTransformer()
			);
		}
	}

	private static TupleElement<?>[] buildTupleElementArray(List<SqmSelection<?>> selections) {
		if ( selections.size() == 1 ) {
			final SqmSelectableNode<?> selectableNode = selections.get(0).getSelectableNode();
			if ( selectableNode instanceof CompoundSelection<?> ) {
				final List<? extends JpaSelection<?>> selectionItems = selectableNode.getSelectionItems();
				final TupleElement<?>[] elements = new TupleElement<?>[ selectionItems.size() ];
				for ( int i = 0; i < selectionItems.size(); i++ ) {
					elements[i] = selectionItems.get( i );
				}
				return elements;
			}
			else {
				return new TupleElement<?>[] { selectableNode };
			}
		}
		else {
			final TupleElement<?>[] elements = new TupleElement<?>[ selections.size() ];
			for ( int i = 0; i < selections.size(); i++ ) {
				elements[i] = selections.get( i ).getSelectableNode();
			}
			return elements;
		}
	}

	private static String[] buildTupleAliasArray(List<SqmSelection<?>> selections) {
		if ( selections.size() == 1 ) {
			final SqmSelectableNode<?> selectableNode = selections.get(0).getSelectableNode();
			if ( selectableNode instanceof CompoundSelection<?> ) {
				final List<? extends JpaSelection<?>> selectionItems = selectableNode.getSelectionItems();
				final String[] elements  = new String[ selectionItems.size() ];
				for ( int i = 0; i < selectionItems.size(); i++ ) {
					elements[i] = selectionItems.get( i ).getAlias();
				}
				return elements;
			}
			else {
				return new String[] { selectableNode.getAlias() };
			}
		}
		else {
			final String[] elements = new String[ selections.size() ];
			for ( int i = 0; i < selections.size(); i++ ) {
				elements[i] = selections.get( i ).getAlias();
			}
			return elements;
		}
	}

	protected void applyOptions(NamedSqmQueryMemento memento) {
		applyOptions( (NamedQueryMemento) memento );

		if ( memento.getFirstResult() != null ) {
			setFirstResult( memento.getFirstResult() );
		}

		if ( memento.getMaxResults() != null ) {
			setMaxResults( memento.getMaxResults() );
		}

		if ( memento.getParameterTypes() != null ) {
			final BasicTypeRegistry basicTypeRegistry =
					getSessionFactory().getTypeConfiguration().getBasicTypeRegistry();
			for ( Map.Entry<String, String> entry : memento.getParameterTypes().entrySet() ) {
				final BasicType<?> type =
						basicTypeRegistry.getRegisteredType( entry.getValue() );
				getParameterMetadata()
						.getQueryParameter( entry.getKey() ).applyAnticipatedType( type );
			}
		}
	}

	protected void applyOptions(NamedQueryMemento memento) {
		if ( memento.getHints() != null ) {
			memento.getHints().forEach( this::applyHint );
		}

		if ( memento.getCacheable() != null ) {
			setCacheable( memento.getCacheable() );
		}

		if ( memento.getCacheRegion() != null ) {
			setCacheRegion( memento.getCacheRegion() );
		}

		if ( memento.getCacheMode() != null ) {
			setCacheMode( memento.getCacheMode() );
		}

		if ( memento.getFlushMode() != null ) {
			setHibernateFlushMode( memento.getFlushMode() );
		}

		if ( memento.getReadOnly() != null ) {
			setReadOnly( memento.getReadOnly() );
		}

		if ( memento.getTimeout() != null ) {
			setTimeout( memento.getTimeout() );
		}

		if ( memento.getFetchSize() != null ) {
			setFetchSize( memento.getFetchSize() );
		}

		if ( memento.getComment() != null ) {
			setComment( memento.getComment() );
		}
	}

	protected abstract String getQueryString();

	/**
	 * Used to validate that the specified query return type is valid (i.e. the user
	 * did not pass {@code Integer.class} when the selection is an entity)
	 */
	protected void visitQueryReturnType(
			SqmQueryPart<R> queryPart,
			Class<R> expectedResultType,
			SessionFactoryImplementor factory) {
		if ( queryPart instanceof SqmQuerySpec<?> ) {
			final SqmQuerySpec<R> sqmQuerySpec = (SqmQuerySpec<R>) queryPart;
			final List<SqmSelection<?>> sqmSelections = sqmQuerySpec.getSelectClause().getSelections();

			if ( getQueryString() == CRITERIA_HQL_STRING ) {
				if ( sqmSelections == null || sqmSelections.isEmpty() ) {
					// make sure there is at least one root
					final List<SqmRoot<?>> sqmRoots = sqmQuerySpec.getFromClause().getRoots();
					if ( sqmRoots == null || sqmRoots.isEmpty() ) {
						throw new IllegalArgumentException( "Criteria did not define any query roots" );
					}
					// if there is a single root, use that as the selection
					if ( sqmRoots.size() == 1 ) {
						sqmQuerySpec.getSelectClause().add( sqmRoots.get( 0 ), null );
					}
					else {
						throw new IllegalArgumentException( "Criteria has multiple query roots" );
					}
				}
			}

			if ( expectedResultType != null ) {
				checkQueryReturnType( sqmQuerySpec, expectedResultType, factory );
			}
		}
		else {
			final SqmQueryGroup<R> queryGroup = (SqmQueryGroup<R>) queryPart;
			for ( SqmQueryPart<R> sqmQueryPart : queryGroup.getQueryParts() ) {
				visitQueryReturnType( sqmQueryPart, expectedResultType, factory );
			}
		}
	}

	protected static <T> void checkQueryReturnType(
			SqmQuerySpec<T> querySpec,
			Class<T> expectedResultClass,
			SessionFactoryImplementor sessionFactory) {
		if ( isResultTypeAlwaysAllowed( expectedResultClass ) ) {
			// the result-class is always safe to use (Object, ...)
			return;
		}

		final List<SqmSelection<?>> selections = querySpec.getSelectClause().getSelections();
		if ( selections.size() == 1 ) {
			final SqmSelection<?> sqmSelection = selections.get( 0 );
			final SqmSelectableNode<?> selectableNode = sqmSelection.getSelectableNode();
			if ( selectableNode.isCompoundSelection() ) {
				final Class<?> expectedSelectItemType = expectedResultClass.isArray()
						? expectedResultClass.getComponentType()
						: expectedResultClass;
				for ( JpaSelection<?> selection : selectableNode.getSelectionItems() ) {
					verifySelectionType( expectedSelectItemType, sessionFactory, (SqmSelectableNode<?>) selection );
				}
			}
			else {
				verifySingularSelectionType( expectedResultClass, sessionFactory, sqmSelection );
			}
		}
		else if ( expectedResultClass.isArray() ) {
			final Class<?> componentType = expectedResultClass.getComponentType();
			for ( SqmSelection<?> selection : selections ) {
				verifySelectionType( componentType, sessionFactory, selection.getSelectableNode() );
			}
		}
	}

	/**
	 * Special case for a single, non-compound selection-item.  It is essentially
	 * a special case of {@linkplain #verifySelectionType} which additionally
	 * handles the case where the type of the selection-item can be used to
	 * instantiate the result-class (result-class has a matching constructor).
	 *
	 * @apiNote We don't want to hoist this into {@linkplain #verifySelectionType}
	 * itself because this can only happen for the root non-compound case, and we
	 * want to avoid the try/catch otherwise
	 */
	private static <T> void verifySingularSelectionType(
			Class<T> expectedResultClass,
			SessionFactoryImplementor sessionFactory,
			SqmSelection<?> sqmSelection) {
		final SqmSelectableNode<?> selectableNode = sqmSelection.getSelectableNode();
		try {
			verifySelectionType( expectedResultClass, sessionFactory, selectableNode );
		}
		catch (QueryTypeMismatchException mismatchException) {
			// Check for special case of a single selection item and implicit instantiation.
			// See if the selected type can be used to instantiate the expected-type
			final JavaType<?> javaTypeDescriptor = selectableNode.getJavaTypeDescriptor();
			if ( javaTypeDescriptor != null ) {
				final Class<?> selectedJavaType = javaTypeDescriptor.getJavaTypeClass();
				// ignore the exception if the expected type has a constructor accepting the selected item type
				if ( hasMatchingConstructor( expectedResultClass, selectedJavaType ) ) {
					// ignore it
				}
				else {
					throw mismatchException;
				}
			}
		}
	}

	private static <T> boolean hasMatchingConstructor(Class<T> expectedResultClass, Class<?> selectedJavaType) {
		try {
			expectedResultClass.getDeclaredConstructor( selectedJavaType );
			return true;
		}
		catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static <T> void verifySelectionType(
			Class<T> expectedResultClass,
			SessionFactoryImplementor sessionFactory,
			SqmSelectableNode<?> selection) {
		// special case for parameters in the select list
		if ( selection instanceof SqmParameter ) {
			final SqmParameter<?> sqmParameter = (SqmParameter<?>) selection;
			final SqmExpressible<?> nodeType = sqmParameter.getExpressible();
			// we may not yet know a selection type
			if ( nodeType == null || nodeType.getExpressibleJavaType() == null ) {
				// we can't verify the result type up front
				return;
			}
		}

		if ( !sessionFactory.getSessionFactoryOptions().getJpaCompliance().isJpaQueryComplianceEnabled() ) {
			verifyResultType( expectedResultClass, selection.getExpressible() );
		}
	}

	private static boolean isInstantiableWithoutMetadata(Class<?> resultType) {
		return resultType == null
			|| resultType.isArray()
			|| Object.class == resultType
			|| List.class == resultType;
	}

	private static <T> boolean isResultTypeAlwaysAllowed(Class<T> expectedResultClass) {
		return expectedResultClass == null
			|| expectedResultClass == Object.class
			|| expectedResultClass == Object[].class
			|| expectedResultClass == List.class
			|| expectedResultClass == Map.class
			|| expectedResultClass == Tuple.class;
	}

	protected static <T> void verifyResultType(Class<T> resultClass, @Nullable SqmExpressible<?> selectionExpressible) {
		if ( selectionExpressible == null ) {
			// nothing we can validate
			return;
		}

		final JavaType<?> selectionExpressibleJavaType = selectionExpressible.getExpressibleJavaType();
		assert selectionExpressibleJavaType != null;

		final Class<?> selectionExpressibleJavaTypeClass = selectionExpressibleJavaType.getJavaTypeClass();
		if ( selectionExpressibleJavaTypeClass == Object.class ) {

		}
		if ( selectionExpressibleJavaTypeClass != Object.class ) {
			// performs a series of opt-out checks for validity... each if branch and return indicates a valid case
			if ( resultClass.isAssignableFrom( selectionExpressibleJavaTypeClass ) ) {
				return;
			}

			if ( selectionExpressibleJavaType instanceof PrimitiveJavaType ) {
				final PrimitiveJavaType<?> primitiveJavaType = (PrimitiveJavaType<?>) selectionExpressibleJavaType;
				if ( primitiveJavaType.getPrimitiveClass() == resultClass ) {
					return;
				}
			}

			if ( isMatchingDateType( selectionExpressibleJavaTypeClass, resultClass, selectionExpressible ) ) {
				return;
			}

			if ( isEntityIdType( selectionExpressible, resultClass ) ) {
				return;
			}

			throwQueryTypeMismatchException( resultClass, selectionExpressible );
		}
	}

	private static <T> boolean isEntityIdType(SqmExpressible<?> selectionExpressible, Class<T> resultClass) {
		if ( selectionExpressible instanceof IdentifiableDomainType ) {
			final IdentifiableDomainType<?> identifiableDomainType = (IdentifiableDomainType<?>) selectionExpressible;
			final SimpleDomainType<?> idType = identifiableDomainType.getIdType();
			return resultClass.isAssignableFrom( idType.getBindableJavaType() );
		}
		else if ( selectionExpressible instanceof EntitySqmPathSource ) {
			final EntitySqmPathSource<?> entityPath = (EntitySqmPathSource<?>) selectionExpressible;
			final EntityDomainType<?> entityType = entityPath.getSqmPathType();
			final SimpleDomainType<?> idType = entityType.getIdType();
			return resultClass.isAssignableFrom( idType.getBindableJavaType() );
		}

		return false;
	}

	// Special case for date because we always report java.util.Date as expression type
	// But the expected resultClass could be a subtype of that, so we need to check the JdbcType
	private static <T> boolean isMatchingDateType(
			Class<?> javaTypeClass,
			Class<T> resultClass,
			SqmExpressible<?> sqmExpressible) {
		return javaTypeClass == Date.class
			&& isMatchingDateJdbcType( resultClass, getJdbcType( sqmExpressible ) );
	}

	private static JdbcType getJdbcType(SqmExpressible<?> sqmExpressible) {
		if ( sqmExpressible instanceof BasicDomainType<?> ) {
			return ( (BasicDomainType<?>) sqmExpressible).getJdbcType();
		}
		else if ( sqmExpressible instanceof SqmPathSource<?> ) {
			final SqmPathSource<?> pathSource = (SqmPathSource<?>) sqmExpressible;
			final DomainType<?> domainType = pathSource.getSqmPathType();
			if ( domainType instanceof BasicDomainType<?> ) {
				return ( (BasicDomainType<?>) domainType ).getJdbcType();
			}
		}
		return null;
	}

	private static <T> boolean isMatchingDateJdbcType(Class<T> resultClass, JdbcType jdbcType) {
		if ( jdbcType != null ) {
			switch ( jdbcType.getDefaultSqlTypeCode() ) {
				case Types.DATE:
					return resultClass.isAssignableFrom( java.sql.Date.class );
				case Types.TIME:
					return resultClass.isAssignableFrom( java.sql.Time.class );
				case Types.TIMESTAMP:
					return resultClass.isAssignableFrom( java.sql.Timestamp.class );
				default:
					return false;
			}
		}
		else {
			return false;
		}
	}

	private static <T> void throwQueryTypeMismatchException(Class<T> resultClass, SqmExpressible<?> sqmExpressible) {
		throw new QueryTypeMismatchException( String.format(
				"Specified result type [%s] did not match Query selection type [%s] - multiple selections: use Tuple or array",
				resultClass.getName(),
				sqmExpressible.getTypeName()
		) );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// execution

	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;

	@Override
	public List<R> list() {
		final HashSet<String> fetchProfiles = beforeQueryHandlingFetchProfiles();
		boolean success = false;
		try {
			final List<R> result = doList();
			success = true;
			return result;
		}
		catch (IllegalQueryOperationException e) {
			throw new IllegalStateException( e );
		}
		catch (HibernateException he) {
			throw getSession().getExceptionConverter().convert( he, getQueryOptions().getLockOptions() );
		}
		finally {
			afterQueryHandlingFetchProfiles( success, fetchProfiles );
		}
	}

	protected HashSet<String> beforeQueryHandlingFetchProfiles() {
		beforeQuery();
		final MutableQueryOptions options = getQueryOptions();
		return getSession().getLoadQueryInfluencers()
				.adjustFetchProfiles( options.getDisabledFetchProfiles(), options.getEnabledFetchProfiles() );
	}

	protected void beforeQuery() {
		getQueryParameterBindings().validate();

		final SharedSessionContractImplementor session = getSession();
		final MutableQueryOptions options = getQueryOptions();

		session.prepareForQueryExecution( requiresTxn( options.getLockOptions().findGreatestLockMode() ) );
		prepareForExecution();

		assert sessionFlushMode == null;
		assert sessionCacheMode == null;

		final FlushMode effectiveFlushMode = getHibernateFlushMode();
		if ( effectiveFlushMode != null ) {
			sessionFlushMode = session.getHibernateFlushMode();
			session.setHibernateFlushMode( effectiveFlushMode );
		}

		final CacheMode effectiveCacheMode = getCacheMode();
		if ( effectiveCacheMode != null ) {
			sessionCacheMode = session.getCacheMode();
			session.setCacheMode( effectiveCacheMode );
		}
	}

	protected abstract void prepareForExecution();

	protected void afterQueryHandlingFetchProfiles(boolean success, HashSet<String> fetchProfiles) {
		resetFetchProfiles( fetchProfiles );
		afterQuery( success );
	}

	private void afterQueryHandlingFetchProfiles(HashSet<String> fetchProfiles) {
		resetFetchProfiles( fetchProfiles );
		afterQuery();
	}

	private void resetFetchProfiles(HashSet<String> fetchProfiles) {
		getSession().getLoadQueryInfluencers().setEnabledFetchProfileNames( fetchProfiles );
	}

	protected void afterQuery(boolean success) {
		afterQuery();

		final SharedSessionContractImplementor session = getSession();
		if ( !session.isTransactionInProgress() ) {
			session.getJdbcCoordinator().getLogicalConnection().afterTransaction();
		}
		session.afterOperation( success );
	}

	protected void afterQuery() {
		if ( sessionFlushMode != null ) {
			getSession().setHibernateFlushMode( sessionFlushMode );
			sessionFlushMode = null;
		}
		if ( sessionCacheMode != null ) {
			getSession().setCacheMode( sessionCacheMode );
			sessionCacheMode = null;
		}
	}

	protected boolean requiresTxn(LockMode lockMode) {
		return lockMode != null && lockMode.greaterThan( LockMode.READ );
	}

	protected abstract List<R> doList();

	@Override
	public ScrollableResultsImplementor<R> scroll() {
		return scroll( getSessionFactory().getJdbcServices().getDialect().defaultScrollMode() );
	}

	@Override
	public ScrollableResultsImplementor<R> scroll(ScrollMode scrollMode) {
		final HashSet<String> fetchProfiles = beforeQueryHandlingFetchProfiles();
		try {
			return doScroll( scrollMode );
		}
		finally {
			afterQueryHandlingFetchProfiles( fetchProfiles );
		}
	}

	protected abstract ScrollableResultsImplementor<R> doScroll(ScrollMode scrollMode);

	@Override
	public Stream<R> getResultStream() {
		return stream();
	}

	@SuppressWarnings( {"unchecked", "rawtypes"} )
	@Override
	public Stream stream() {
		final ScrollableResultsImplementor scrollableResults = scroll( ScrollMode.FORWARD_ONLY );
		final ScrollableResultsIterator iterator = new ScrollableResultsIterator<>( scrollableResults );
		final Spliterator spliterator = spliteratorUnknownSize( iterator, Spliterator.NONNULL );

		final Stream stream = StreamSupport.stream( spliterator, false );
		return (Stream) stream.onClose( scrollableResults::close );
	}

	@Override
	public R uniqueResult() {
		return uniqueElement( list() );
	}

	@Override
	public R getSingleResult() {
		try {
			final List<R> list = list();
			if ( list.isEmpty() ) {
				throw new NoResultException(
						String.format( "No result found for query [%s]", getQueryString() )
				);
			}
			return uniqueElement( list );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e, getQueryOptions().getLockOptions() );
		}
	}

	protected static <T> T uniqueElement(List<T> list) throws NonUniqueResultException {
		int size = list.size();
		if ( size == 0 ) {
			return null;
		}
		else {
			final T first = list.get( 0 );
			// todo (6.0) : add a setting here to control whether to perform this validation or not
			for ( int i = 1; i < size; i++ ) {
				if ( list.get( i ) != first ) {
					throw new NonUniqueResultException( list.size() );
				}
			}
			return first;
		}
	}

	@Override
	public Optional<R> uniqueResultOptional() {
		return Optional.ofNullable( uniqueResult() );
	}

	@Override
	public R getSingleResultOrNull() {
		try {
			return uniqueElement( list() );
		}
		catch ( HibernateException e ) {
			throw getSession().getExceptionConverter().convert( e, getLockOptions() );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainQueryExecutionContext

	@Override
	public Callback getCallback() {
		if ( callback == null ) {
			callback = new CallbackImpl();
		}
		return callback;
	}

	@Override
	public boolean hasCallbackActions() {
		return callback != null && callback.hasAfterLoadActions();
	}

	protected void resetCallback() {
		callback = null;
	}


	@Override
	public FlushModeType getFlushMode() {
		return getQueryOptions().getFlushMode().toJpaFlushMode();
	}

	@Override
	public SelectionQuery<R> setFlushMode(FlushModeType flushMode) {
		getQueryOptions().setFlushMode( fromJpaFlushMode( flushMode ) );
		return this;
	}

	@Override
	public SelectionQuery<R> setMaxResults(int maxResult) {
		super.applyMaxResults( maxResult );
		return this;
	}

	@Override
	public SelectionQuery<R> setFirstResult(int startPosition) {
		getSession().checkOpen();
		if ( startPosition < 0 ) {
			throw new IllegalArgumentException( "first-result value cannot be negative : " + startPosition );
		}
		getQueryOptions().getLimit().setFirstRow( startPosition );
		return this;
	}

	@Override
	public SelectionQuery<R> setHint(String hintName, Object value) {
		super.setHint( hintName, value );
		return this;
	}

	@Override
	public SelectionQuery<R> setEntityGraph(EntityGraph<R> graph, GraphSemantic semantic) {
		applyGraph( (RootGraphImplementor<R>) graph, semantic );
		return this;
	}

	@Override
	public SelectionQuery<R> enableFetchProfile(String profileName) {
		if ( !getSession().getFactory().containsFetchProfileDefinition( profileName ) ) {
			throw new UnknownProfileException( profileName );
		}
		getQueryOptions().enableFetchProfile( profileName );
		return this;
	}

	@Override
	public SelectionQuery<R> disableFetchProfile(String profileName) {
		getQueryOptions().disableFetchProfile( profileName );
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}

	@Override
	public LockModeType getLockMode() {
		return LockModeTypeHelper.getLockModeType( getHibernateLockMode() );
	}

	/**
	 * Specify the root LockModeType for the query
	 *
	 * @see #setHibernateLockMode
	 */
	@Override
	public SelectionQuery<R> setLockMode(LockModeType lockMode) {
		setHibernateLockMode( LockModeTypeHelper.getLockMode( lockMode ) );
		return this;
	}

	@Override
	public SelectionQuery<R> setLockMode(String alias, LockMode lockMode) {
		getQueryOptions().getLockOptions().setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	/**
	 * Get the root LockMode for the query
	 */
	@Override
	public LockMode getHibernateLockMode() {
		return getLockOptions().getLockMode();
	}

	/**
	 * Specify the root LockMode for the query
	 */
	@Override
	public SelectionQuery<R> setHibernateLockMode(LockMode lockMode) {
		getLockOptions().setLockMode( lockMode );
		return this;
	}

	/**
	 * Specify a LockMode to apply to a specific alias defined in the query
	 *
	 * @deprecated use {{@link #setLockMode(String, LockMode)}}
	 */
	@Override @Deprecated
	public SelectionQuery<R> setAliasSpecificLockMode(String alias, LockMode lockMode) {
		getLockOptions().setAliasSpecificLockMode( alias, lockMode );
		return this;
	}

	/**
	 * Specifies whether follow-on locking should be applied?
	 */
	public SelectionQuery<R> setFollowOnLocking(boolean enable) {
		getLockOptions().setFollowOnLocking( enable );
		return this;
	}

	protected void collectHints(Map<String, Object> hints) {
		super.collectHints( hints );

		if ( isReadOnly() ) {
			hints.put( HINT_READ_ONLY, true );
		}

		putIfNotNull( hints, HINT_FETCH_SIZE, getFetchSize() );

		if ( isCacheable() ) {
			hints.put( HINT_CACHEABLE, true );
			putIfNotNull( hints, HINT_CACHE_REGION, getCacheRegion() );

			putIfNotNull( hints, HINT_CACHE_MODE, getCacheMode() );
			putIfNotNull( hints, JAKARTA_SHARED_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			putIfNotNull( hints, JAKARTA_SHARED_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
			//noinspection deprecation
			putIfNotNull( hints, JPA_SHARED_CACHE_RETRIEVE_MODE, getQueryOptions().getCacheRetrieveMode() );
			//noinspection deprecation
			putIfNotNull( hints, JPA_SHARED_CACHE_STORE_MODE, getQueryOptions().getCacheStoreMode() );
		}

		final AppliedGraph appliedGraph = getQueryOptions().getAppliedGraph();
		if ( appliedGraph != null && appliedGraph.getSemantic() != null ) {
			hints.put( appliedGraph.getSemantic().getJakartaHintName(), appliedGraph );
			hints.put( appliedGraph.getSemantic().getJpaHintName(), appliedGraph );
		}

		putIfNotNull( hints, HINT_FOLLOW_ON_LOCKING, getQueryOptions().getLockOptions().getFollowOnLocking() );
	}

	@Override
	public Integer getFetchSize() {
		return getQueryOptions().getFetchSize();
	}

	@Override
	public SelectionQuery<R> setFetchSize(int fetchSize) {
		getQueryOptions().setFetchSize( fetchSize );
		return this;
	}

	@Override
	public boolean isReadOnly() {
		return getQueryOptions().isReadOnly() == null
				? getSession().isDefaultReadOnly()
				: getQueryOptions().isReadOnly();
	}

	@Override
	public SelectionQuery<R> setReadOnly(boolean readOnly) {
		getQueryOptions().setReadOnly( readOnly );
		return this;
	}
	@Override
	public CacheMode getCacheMode() {
		return getQueryOptions().getCacheMode();
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return getCacheMode().getJpaStoreMode();
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return getCacheMode().getJpaRetrieveMode();
	}

	@Override
	public SelectionQuery<R> setCacheMode(CacheMode cacheMode) {
		getQueryOptions().setCacheMode( cacheMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		return setCacheMode( fromJpaModes( cacheRetrieveMode, getQueryOptions().getCacheMode().getJpaStoreMode() ) );
	}

	@Override
	public SelectionQuery<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		return setCacheMode( fromJpaModes( getQueryOptions().getCacheMode().getJpaRetrieveMode(), cacheStoreMode ) );
	}

	@Override
	public boolean isCacheable() {
		return getQueryOptions().isResultCachingEnabled() == Boolean.TRUE;
	}

	@Override
	public SelectionQuery<R> setCacheable(boolean cacheable) {
		getQueryOptions().setResultCachingEnabled( cacheable );
		return this;
	}

	@Override
	public boolean isQueryPlanCacheable() {
		// By default, we assume query plan caching is enabled unless explicitly disabled
		return getQueryOptions().getQueryPlanCachingEnabled() != Boolean.FALSE;
	}

	@Override
	public SelectionQuery<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		getQueryOptions().setQueryPlanCachingEnabled( queryPlanCacheable );
		return this;
	}

	@Override
	public String getCacheRegion() {
		return getQueryOptions().getResultCacheRegionName();
	}

	@Override
	public SelectionQuery<R> setCacheRegion(String regionName) {
		getQueryOptions().setResultCacheRegionName( regionName );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// covariance

	@Override
	public SelectionQuery<R> setHibernateFlushMode(FlushMode flushMode) {
		super.setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setTimeout(int timeout) {
		super.setTimeout( timeout );
		return this;
	}

	@Override
	public SelectionQuery<R> setComment(String comment) {
		super.setComment( comment );
		return this;
	}


	@Override
	public SelectionQuery<R> setParameter(String name, Object value) {
		super.setParameter( name, value );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(String name, P value, Class<P> javaType) {
		super.setParameter( name, value, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(String name, P value, BindableType<P> type) {
		super.setParameter( name, value, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Object value) {
		super.setParameter( position, value );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(int position, P value, Class<P> javaType) {
		super.setParameter( position, value, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(int position, P value, BindableType<P> type) {
		super.setParameter( position, value, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> javaType) {
		super.setParameter( parameter, value, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, BindableType<P> type) {
		super.setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(Parameter<P> parameter, P value) {
		super.setParameter( parameter, value );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		super.setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType) {
		super.setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType) {
		super.setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(String name, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(String name, Object[] values) {
		super.setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType) {
		super.setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, P[] values, BindableType<P> type) {
		super.setParameterList( name, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(int position, @SuppressWarnings("rawtypes") Collection values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(int position, Object[] values) {
		super.setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType) {
		super.setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, P[] values, BindableType<P> type) {
		super.setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		super.setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		super.setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		super.setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setProperties(@SuppressWarnings("rawtypes") Map map) {
		super.setProperties( map );
		return this;
	}

	@Override
	public SelectionQuery<R> setProperties(Object bean) {
		super.setProperties( bean );
		return this;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return getSession().getFactory();
	}
}
