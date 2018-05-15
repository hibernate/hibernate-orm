/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.produce.internal.PerQuerySpecSqlExpressionResolver;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstProducerContext;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.results.spi.CircularFetchDetector;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
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
		implements DomainResultCreationContext, DomainResultCreationState {
	private static final Logger log = Logger.getLogger( SqmSelectToSqlAstConverter.class );

	private final PerQuerySpecSqlExpressionResolver expressionResolver;

	private final LoadQueryInfluencers loadQueryInfluencers;

	private final Stack<Shallowness> shallownessStack = new StandardStack<>( Shallowness.NONE );

	private final List<DomainResult> domainResults = new ArrayList<>();

	private Map<NavigablePath, Set<SqmNavigableJoin>> fetchJoinsByParentPath;

	private int counter;

	public String generateSqlAstNodeUid() {
		return "<uid(fetchgraph):" + counter++ + ">";
	}

	public SqmSelectToSqlAstConverter(
			QueryOptions queryOptions,
			SqlAstProducerContext producerContext) {
		super( producerContext, queryOptions );

		this.loadQueryInfluencers = producerContext.getLoadQueryInfluencers();

		this.expressionResolver = new PerQuerySpecSqlExpressionResolver(
				producerContext.getSessionFactory(),
				() -> getQuerySpecStack().getCurrent(),
				this::normalizeSqlExpression,
				this::collectSelection
		);
	}

	public SqlAstSelectDescriptor interpret(SqmSelectStatement statement) {
		fetchJoinsByParentPath = statement.getFetchJoinsByParentPath();
		if ( fetchJoinsByParentPath == null ) {
			fetchJoinsByParentPath = Collections.emptyMap();
		}
		return new SqlAstSelectDescriptorImpl(
				visitSelectStatement( statement ),
				domainResults,
				affectedTableNames()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultCreationState

	@Override
	public boolean fetchAllAttributes() {
		// todo (6.0) : need to expose this from the SQM
		return false;
	}

	private Stack<ColumnReferenceQualifier> resultCreationColumnReferenceQualifierStack = new StandardStack<>();

	@Override
	public Stack<ColumnReferenceQualifier> getColumnReferenceQualifierStack() {
		return resultCreationColumnReferenceQualifierStack;
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return expressionResolver;
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return getSqlAliasBaseManager();
	}

	@Override
	public LockOptions getLockOptions() {
		return getQueryOptions().getLockOptions();
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return getProducerContext().getSessionFactory();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	@Override
	public boolean shouldCreateShallowEntityResult() {
		// todo (6.0) : we also need to vary this for ctor result based on ctor sigs + user option
		return shallownessStack.getCurrent() != Shallowness.NONE;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// walker


	@Override
	public Object visitUpdateStatement(SqmUpdateStatement statement) {
		throw new AssertionFailure( "Not expecting UpdateStatement" );
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public Object visitInsertSelectStatement(SqmInsertSelectStatement statement) {
		throw new AssertionFailure( "Not expecting DeleteStatement" );
	}

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement statement) {
		final QuerySpec querySpec = visitQuerySpec( statement.getQuerySpec() );

		return new SelectStatement( querySpec );
	}


	// select o from Order o join fetch o.customer

	@Override
	public Void visitSelection(SqmSelection sqmSelection) {
		// todo (6.0) : this should actually be able to generate multiple SqlSelections
		final DomainResultProducer resultProducer = (DomainResultProducer) sqmSelection.getSelectableNode().accept( this );

		if ( getQuerySpecStack().depth() > 1 && Expression.class.isInstance( resultProducer ) ) {
			// we only need the QueryResults if we are in the top-level select-clause.
			// but we do need to at least resolve the sql selections
			getSqlSelectionResolver().resolveSqlSelection(
					(Expression) resultProducer,
					(BasicJavaDescriptor) sqmSelection.getJavaTypeDescriptor(),
					getProducerContext().getSessionFactory().getTypeConfiguration()
			);
			return null;
		}

		final DomainResult domainResult = resultProducer.createDomainResult(
				sqmSelection.getAlias(),
				this, this
		);

		domainResults.add( domainResult );

		return null;
	}

	private final CircularFetchDetector circularFetchDetector = new CircularFetchDetector();

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

			LockMode lockMode = LockMode.READ;
			FetchTiming fetchTiming = fetchable.getMappedFetchStrategy().getTiming();

			final Integer maximumFetchDepth = getSessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();
			// minus one because the root is not a fetch
			final int fetchDepth = getNavigableReferenceStack().depth() - 1;

			final SqmNavigableJoin fetchedJoin = findFetchedJoin(
					fetchParent,
					fetchable
			);

			final String alias;
			boolean joined;
			if ( fetchedJoin != null ) {
				fetchTiming = FetchTiming.IMMEDIATE;
				joined = true;

				lockMode = SqmSelectToSqlAstConverter.this.getLockOptions().getEffectiveLockMode(
						fetchedJoin.getIdentificationVariable()
				);

				alias = fetchedJoin.getIdentificationVariable();
			}
			else {
				// todo (6.0) : account for EntityGraph


				// Note that legacy Hibernate behavior for HQL processing was to stop here
				// in terms of defining immediate join fetches - they had to be
				// explicitly defined in the query (although we did add some support for
				// using JPA EntityGraphs to influence the fetches to be JPA compliant)
				joined = fetchTiming == FetchTiming.IMMEDIATE && fetchable.getMappedFetchStrategy().getStyle() == FetchStyle.JOIN;
				alias = null;
			}

			if ( fetchDepth == maximumFetchDepth ) {
				joined = false;
			}
			else if ( fetchDepth > maximumFetchDepth ) {
				return;
			}

			final Fetch fetch = fetchable.generateFetch(
					fetchParent,
					fetchTiming,
					joined,
					lockMode,
					alias,
					SqmSelectToSqlAstConverter.this,
					SqmSelectToSqlAstConverter.this
			);

			fetches.add( fetch );

		};

		NavigableContainer<?> navigableContainer = fetchParent.getNavigableContainer();
		navigableContainer.visitKeyFetchables( fetchableConsumer );
		navigableContainer.visitFetchables( fetchableConsumer );

		return fetches;
	}

	@Override
	public TableSpace getCurrentTableSpace() {
		// todo (6.0) : not sure this is a great impl given subqueries
		return getTableGroupStack().getCurrent().getTableSpace();
	}

	@Override
	public LockMode determineLockMode(String identificationVariable) {
		final LockOptions lockOptions = getLockOptions();
		return lockOptions.getScope() || identificationVariable == null
				? getLockOptions().getLockMode()
				: getLockOptions().getEffectiveLockMode( identificationVariable );
	}

	private SqmNavigableJoin findFetchedJoin(FetchParent fetchParent, Fetchable fetchable) {
		final Set<SqmNavigableJoin> explicitFetchJoins = fetchJoinsByParentPath.get( fetchParent.getNavigablePath() );

		if ( explicitFetchJoins != null ) {
			for ( SqmNavigableJoin explicitFetchJoin : explicitFetchJoins ) {
				final String fetchedAttributeName = explicitFetchJoin.getAttributeReference()
						.getReferencedNavigable()
						.getAttributeName();
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
		final JavaTypeDescriptor<Object> targetTypeDescriptor = interpretInstantiationTarget(
				instantiationTarget,
				getSessionFactory()
		);

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
	private <T> JavaTypeDescriptor<T> interpretInstantiationTarget(
			SqmDynamicInstantiationTarget instantiationTarget,
			SessionFactoryImplementor sessionFactory) {
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

		return sessionFactory.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( targetJavaType );
	}

	@Override
	public SqlExpressionResolver getSqlSelectionResolver() {
		return expressionResolver;
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
