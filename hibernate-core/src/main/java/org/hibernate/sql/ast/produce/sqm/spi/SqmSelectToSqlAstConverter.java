/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.Remove;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.domain.SqmDiscriminatorReference;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;
import org.hibernate.sql.ast.produce.spi.FromClauseAccess;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.domain.BasicValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.results.spi.CircularFetchDetector;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Interprets an SqmSelectStatement as a SQL-AST SelectQuery.
 *
 * @author Steve Ebersole
 * @author John O'Hara
 */
@SuppressWarnings("unchecked")
public class SqmSelectToSqlAstConverter
		extends BaseSqmToSqlAstConverter
		implements DomainResultCreationState {
	private static final Logger log = Logger.getLogger( SqmSelectToSqlAstConverter.class );

	private final List<DomainResult> domainResults = new ArrayList<>();
	private final CircularFetchDetector circularFetchDetector = new CircularFetchDetector();

	/**
	 * @deprecated See {@link FromClauseAccess}
	 */
	@Remove
	@Deprecated
	private Map<NavigablePath, Set<SqmNavigableJoin>> fetchJoinsByParentPath;

	private int counter;

	public SqmSelectToSqlAstConverter(
			QueryOptions queryOptions,
			LoadQueryInfluencers influencers,
			Callback callback,
			SqlAstCreationContext creationContext) {
		super( creationContext, queryOptions, influencers, callback );
	}

	public SqlAstSelectDescriptor interpret(SqmSelectStatement statement) {
		fetchJoinsByParentPath = statement.getFetchJoinsByParentPath();
		if ( fetchJoinsByParentPath == null ) {
			fetchJoinsByParentPath = Collections.emptyMap();
		}
		return new SqlAstSelectDescriptorImpl(
				visitSelectStatement( statement ),
				domainResults,
				getFromClauseIndex().getAffectedTableNames()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultCreationState

	@Override
	public boolean fetchAllAttributes() {
		// todo (6.0) : need to expose this from the SQM
		return false;
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// walker

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement statement) {
		final QuerySpec querySpec = visitQuerySpec( statement.getQuerySpec() );

		return new SelectStatement( querySpec );
	}


	@Override
	public Void visitSelection(SqmSelection sqmSelection) {
		// todo (6.0) : this should actually be able to generate multiple SqlSelections
		final DomainResultProducer resultProducer = (DomainResultProducer) sqmSelection.getSelectableNode().accept( this );

		if ( getProcessingStateStack().depth() > 1 && Expression.class.isInstance( resultProducer ) ) {
			// we only need the QueryResults if we are in the top-level select-clause.
			// but we do need to at least resolve the sql selections
			getSqlExpressionResolver().resolveSqlSelection(
					(Expression) resultProducer,
					(BasicJavaDescriptor) sqmSelection.getJavaTypeDescriptor(),
					getCreationContext().getDomainModel().getTypeConfiguration()
			);
			return null;
		}

		final DomainResult domainResult = resultProducer.createDomainResult(
				sqmSelection.getAlias(),
				this
		);

		domainResults.add( domainResult );

		return null;
	}

	private int fetchDepth = 0;

	private int currentFetchDepth() {
		return fetchDepth;
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		final List<Fetch> fetches = new ArrayList();

		final Consumer<Fetchable> fetchableConsumer = fetchable -> {
			final Fetch biDirectionalFetch = circularFetchDetector.findBiDirectionalFetch(
					fetchParent,
					fetchable
			);

			if ( biDirectionalFetch != null ) {
				fetches.add( biDirectionalFetch );
				return;
			}

			try {
				fetchDepth++;
				final Fetch fetch = buildFetch( fetchParent, fetchable );
				if ( fetch != null ) {
					fetches.add( fetch );
				}
			}
			finally {
				fetchDepth--;
			}
		};

		NavigableContainer<?> navigableContainer = fetchParent.getNavigableContainer();
		navigableContainer.visitKeyFetchables( fetchableConsumer );
		navigableContainer.visitFetchables( fetchableConsumer );

		return fetches;
	}

	private Set<Fetchable> processedFetchables;

	private Fetch buildFetch(FetchParent fetchParent, Fetchable fetchable) {
		// fetch has access to its parent in addition to the parent having its fetches.
		//
		// we could sever the parent -> fetch link ... it would not be "seen" while walking
		// but it would still have access to its parent info - and be able to access its
		// "initializing" state as part of AfterLoadAction

		final NavigablePath fetchablePath = fetchParent.getNavigablePath().append( fetchable.getNavigableName() );

		LockMode lockMode = LockMode.READ;
		FetchTiming fetchTiming = fetchable.getMappedFetchStrategy().getTiming();

		final SqmNavigableJoin fetchedJoin = getFromClauseIndex().findFetchedJoinByPath( fetchablePath );
		final String alias;
		boolean joined;

		if ( fetchedJoin != null ) {
			// there was an explicit fetch in the SQM
			//		there should be a TableGroupJoin registered n SqmJoin registered for this `fetchablePath` already
			//		because it
			assert getFromClauseIndex().getTableGroup( fetchablePath ) != null;

			fetchTiming = FetchTiming.IMMEDIATE;
			joined = true;
			alias = fetchedJoin.getIdentificationVariable();
			lockMode = determineLockMode( alias );
		}
		else {
			// there was not an explicit fetch in the SQM

			alias = null;

			if ( fetchable instanceof Joinable ) {
				if ( processedFetchables == null ) {
					processedFetchables = new HashSet<>();
				}

				final boolean added = processedFetchables.add( fetchable );

				if ( ! added ) {
					joined = false;
				}
				else {
					joined = fetchTiming == FetchTiming.IMMEDIATE && fetchable.getMappedFetchStrategy().getStyle() == FetchStyle.JOIN;
				}
			}
			else {
				joined = true;
			}

			// todo (6.0) : account for EntityGraph
			//		it would adjust:
			//			* fetchTiming - make it IMMEDIATE
			//
			// todo (6.0) : how to handle
			//			* joined ? - sh


			final TableGroup existingJoinedGroup = getFromClauseIndex().findTableGroup( fetchablePath );
			if ( existingJoinedGroup !=  null ) {
				// we can use this to trigger the fetch from the joined group.

				// todo (6.0) : do we want to do this though?
				//  	On the positive side it would allow EntityGraph to use the existing TableGroup.  But that ties in
				//  	to the discussion above regarding how to handle eager and EntityGraph (JOIN versus SELECT).
				//		Can be problematic if the existing one is restricted
				//fetchTiming = FetchTiming.IMMEDIATE;
			}

			// lastly, account for any app-defined max-fetch-depth
			final Integer maxDepth = getCreationContext().getMaximumFetchDepth();
			if ( maxDepth != null ) {
				if ( fetchDepth == maxDepth ) {
					joined = false;
				}
				else if ( fetchDepth > maxDepth ) {
					return null;
				}
			}

			if ( joined && fetchable instanceof TableGroupJoinProducer ) {
				getFromClauseIndex().resolveTableGroup(
						fetchablePath,
						np -> {
							// generate the join
							final TableGroup lhs = getFromClauseIndex().getTableGroup( fetchParent.getNavigablePath() );
							final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) fetchable ).createTableGroupJoin(
									null,
									fetchablePath,
									lhs,
									null,
									JoinType.LEFT,
									LockMode.NONE,
									this
							);
							lhs.addTableGroupJoin(  tableGroupJoin );
							return tableGroupJoin.getJoinedGroup();
						}
				);

			}
		}

		try {
			return fetchable.generateFetch(
					fetchParent,
					fetchTiming,
					joined,
					lockMode,
					alias,
					SqmSelectToSqlAstConverter.this
			);

		}
		catch (RuntimeException e) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not generate fetch : %s -> %s",
							fetchParent.getNavigablePath(),
							fetchable.getNavigableName()
					),
					e
			);
		}
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		return getFromClauseIndex();
	}

	@Override
	public LockMode determineLockMode(String identificationVariable) {
		final LockOptions lockOptions = getQueryOptions().getLockOptions();
		return lockOptions.getScope() || identificationVariable == null
				? lockOptions.getLockMode()
				: lockOptions.getEffectiveLockMode( identificationVariable );
	}

	private SqmNavigableJoin findFetchedJoin(FetchParent fetchParent, Fetchable fetchable) {
		final Set<SqmNavigableJoin> explicitFetchJoins = fetchJoinsByParentPath.get( fetchParent.getNavigablePath() );

		if ( explicitFetchJoins != null ) {
			for ( SqmNavigableJoin explicitFetchJoin : explicitFetchJoins ) {
				final String fetchedAttributeName = explicitFetchJoin.getReferencedNavigable().getNavigableName();
				if ( fetchable.getNavigableName().equals( fetchedAttributeName ) ) {
					if ( explicitFetchJoin.isFetched() ) {
						return explicitFetchJoin;
					}
					else {
						return null;
					}
				}
			}
		}

		return null;
	}

	@Override
	public DynamicInstantiation visitDynamicInstantiation(SqmDynamicInstantiation sqmDynamicInstantiation) {
		final SqmDynamicInstantiationTarget instantiationTarget = sqmDynamicInstantiation.getInstantiationTarget();
		final DynamicInstantiationNature instantiationNature = instantiationTarget.getNature();
		final JavaTypeDescriptor<Object> targetTypeDescriptor = interpretInstantiationTarget( instantiationTarget );

		final DynamicInstantiation dynamicInstantiation = new DynamicInstantiation(
				instantiationNature,
				targetTypeDescriptor
		);

		for ( SqmDynamicInstantiationArgument sqmArgument : sqmDynamicInstantiation.getArguments() ) {
			final DomainResultProducer argumentResultProducer = (DomainResultProducer) sqmArgument.getSelectableNode().accept( this );
			dynamicInstantiation.addArgument(
					sqmArgument.getAlias(),
					argumentResultProducer
			);
		}

		dynamicInstantiation.complete();

		return dynamicInstantiation;
	}

	@SuppressWarnings("unchecked")
	private <T> JavaTypeDescriptor<T> interpretInstantiationTarget(SqmDynamicInstantiationTarget instantiationTarget) {
		final Class<T> targetJavaType;

		if ( instantiationTarget.getNature() == DynamicInstantiationNature.LIST ) {
			targetJavaType = (Class<T>) List.class;
		}
		else if ( instantiationTarget.getNature() == DynamicInstantiationNature.MAP ) {
			targetJavaType = (Class<T>) Map.class;
		}
		else {
			targetJavaType = instantiationTarget.getJavaType();
		}

		return getCreationContext().getDomainModel()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( targetJavaType );
	}

	@Override
	public Expression visitEntityTypeLiteralExpression(SqmLiteralEntityType expression) {
		return new EntityTypeLiteral( expression.getExpressableType().getEntityDescriptor() );
	}

	@Override
	public NavigableReference visitDiscriminatorReference(SqmDiscriminatorReference expression) {
		final TableGroup entityTableGroup = getFromClauseIndex().getTableGroup( expression.getNavigablePath().getParent() );
		return new BasicValuedNavigableReference(
				expression.getNavigablePath(),
				( (EntityValuedNavigable) entityTableGroup.getNavigable() ).getEntityDescriptor().getHierarchy().getDiscriminatorDescriptor(),
				this
		);
	}

	@Override
	public Object visitFullyQualifiedClass(Class namedClass) {
		throw new NotYetImplementedFor6Exception();

		// what exactly is the expected end result here?

//		final MetamodelImplementor metamodel = getSessionFactory().getMetamodel();
//		final TypeConfiguration typeConfiguration = getSessionFactory().getTypeConfiguration();
//
//		// see if it is an entity-type
//		final EntityTypeDescriptor entityDescriptor = metamodel.findEntityDescriptor( namedClass );
//		if ( entityDescriptor != null ) {
//			throw new NotYetImplementedFor6Exception( "Add support for entity type literals as SqlExpression" );
//		}
//
//
//		final JavaTypeDescriptor jtd = typeConfiguration
//				.getJavaTypeDescriptorRegistry()
//				.getOrMakeJavaDescriptor( namedClass );
	}

	@Override
	public QueryLiteral visitFullyQualifiedEnum(Enum value) {
		return new QueryLiteral(
				value,
				getTypeConfiguration()
						.standardExpressableTypeForJavaType( value.getClass() )
						.getSqlExpressableType(),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public QueryLiteral visitFullyQualifiedField(Field field) {
		try {
			final Object value = field.get( null );
			return new QueryLiteral(
					value,
					getTypeConfiguration()
							.standardExpressableTypeForJavaType( value.getClass() )
							.getSqlExpressableType(),
					getCurrentClauseStack().getCurrent()
			);
		}
		catch (IllegalAccessException e) {
			throw new IllegalArgumentException(  );
		}
	}

	//	@Override
//	public SqlSelection resolveSqlSelection(Expression expression) {
//		return sqlSelectionByExpressionMap.get( expression );
//	}

	//	@Override
//	public DomainReferenceExpression visitAttributeReferenceExpression(AttributeBinding attributeBinding) {
//		if ( attributeBinding instanceof PluralAttributeBinding ) {
//			return getCurrentDomainReferenceExpressionBuilder().buildPluralAttributeExpression(
//					this,
//					(PluralAttributeBinding) attributeBinding
//			);
//		}
//		else {
//			return getCurrentDomainReferenceExpressionBuilder().buildSingularAttributeExpression(
//					this,
//					(SingularAttributeBinding) attributeBinding
//			);
//		}
//	}
}
