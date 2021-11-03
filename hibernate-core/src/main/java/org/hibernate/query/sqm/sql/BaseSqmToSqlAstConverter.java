/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import jakarta.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.QueryException;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolver;
import org.hibernate.dialect.function.TimestampaddFunction;
import org.hibernate.dialect.function.TimestampdiffFunction;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.internal.FilterHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ConvertibleModelPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SqlExpressable;
import org.hibernate.metamodel.mapping.ValueMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedCollectionPart;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.NonAggregatedIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.CompositeSqmPathSource;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedRootJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.sql.exec.internal.VersionTypeSeedParameterSpecification;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.CastType;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.DynamicInstantiationNature;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.InterpretationException;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.sql.internal.BasicValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.DiscriminatedAssociationPathInterpretation;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.sql.internal.EmbeddableValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.EntityValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.NonAggregatedCompositeValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.PluralValuedSimplePathInterpretation;
import org.hibernate.query.sqm.sql.internal.SelfInterpretingSqmPath;
import org.hibernate.query.sqm.sql.internal.SqlAstProcessingStateImpl;
import org.hibernate.query.sqm.sql.internal.SqlAstQueryPartProcessingStateImpl;
import org.hibernate.query.sqm.sql.internal.SqmMapEntryResult;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.sql.internal.TypeHelper;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteTable;
import org.hibernate.query.sqm.tree.cte.SqmCteTableColumn;
import org.hibernate.query.sqm.tree.cte.SqmSearchClauseSpecification;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.AbstractSqmSpecificPluralPartPath;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmMaxElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMaxIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmMinElementPath;
import org.hibernate.query.sqm.tree.domain.SqmMinIndexPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmAny;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmByUnit;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmCollate;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmEvery;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmStar;
import org.hibernate.query.sqm.tree.expression.SqmSummarization;
import org.hibernate.query.sqm.tree.expression.SqmToDuration;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmExistsPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmAliasedNode;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.cte.SearchClauseSpecification;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Collate;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.from.CorrelatedTableGroup;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.VirtualTableGroup;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.spi.JdbcParameters;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.EntityGraphTraversalState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiation;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.StandardEntityGraphTraversalStateImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserVersionType;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.query.BinaryArithmeticOperator.ADD;
import static org.hibernate.query.BinaryArithmeticOperator.MULTIPLY;
import static org.hibernate.query.BinaryArithmeticOperator.SUBTRACT;
import static org.hibernate.query.TemporalUnit.DAY;
import static org.hibernate.query.TemporalUnit.EPOCH;
import static org.hibernate.query.TemporalUnit.NATIVE;
import static org.hibernate.query.TemporalUnit.SECOND;
import static org.hibernate.query.UnaryArithmeticOperator.UNARY_MINUS;
import static org.hibernate.type.spi.TypeConfiguration.isDuration;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmToSqlAstConverter<T extends Statement> extends BaseSemanticQueryWalker
		implements SqmTranslator<T>, DomainResultCreationState, JdbcTypeDescriptorIndicators {

	private static final Logger log = Logger.getLogger( BaseSqmToSqlAstConverter.class );

	private final SqlAstCreationContext creationContext;
	private final boolean jpaQueryComplianceEnabled;
	private final SqmStatement<?> statement;

	private final QueryOptions queryOptions;
	private final LoadQueryInfluencers loadQueryInfluencers;

	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindings domainParameterBindings;
	private final Map<SqmParameter,MappingModelExpressable> sqmParameterMappingModelTypes = new LinkedHashMap<>();
	private final Map<JpaCriteriaParameter<?>, Supplier<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions;
	private final List<DomainResult<?>> domainResults;
	private final EntityGraphTraversalState entityGraphTraversalState;

	private int fetchDepth;
	private boolean resolvingCircularFetch;
	private ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide;
	private SqmQueryPart<?> currentSqmQueryPart;

	private Map<String, FilterPredicate> collectionFilterPredicates;
	private OrderByFragmentConsumer orderByFragmentConsumer;

	private final Map<String, NavigablePath> joinPathBySqmJoinFullPath = new HashMap<>();

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();
	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>();
	private final Stack<FromClauseIndex> fromClauseIndexStack = new StandardStack<>();
	private SqlAstProcessingState lastPoppedProcessingState;
	private FromClauseIndex lastPoppedFromClauseIndex;
	private SqmJoin<?, ?> currentlyProcessingJoin;

	private final Stack<Clause> currentClauseStack = new StandardStack<>();

	private SqmByUnit appliedByUnit;
	private Expression adjustedTimestamp;
	private SqmExpressable<?> adjustedTimestampType; //TODO: remove this once we can get a Type directly from adjustedTimestamp
	private Expression adjustmentScale;
	private boolean negativeAdjustment;

	public BaseSqmToSqlAstConverter(
			SqlAstCreationContext creationContext,
			SqmStatement<?> statement,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings) {
		super( creationContext.getServiceRegistry() );

		this.creationContext = creationContext;
		this.jpaQueryComplianceEnabled = creationContext
				.getSessionFactory()
				.getSessionFactoryOptions()
				.getJpaCompliance()
				.isJpaQueryComplianceEnabled();

		this.statement = statement;

		if ( statement instanceof SqmSelectStatement<?> ) {
			// NOTE: note the difference here between `JpaSelection#getSelectionItems`
			//		and `SqmSelectClause#getSelections`.
			//
			//		- `#getSelectionItems` returns individual select-items.  "grouped" selections,
			//			such as dynamic-instantiation, unwrap themselves (possibly recursively).
			//			It is a JPA-defined method
			//
			//		- `#getSelections` returns top-level selections.  These are ultimately the
			//			domain-results of the query
			this.domainResults = new ArrayList<>(
					( (SqmSelectStatement<?>) statement ).getQueryPart()
							.getFirstQuerySpec()
							.getSelectClause()
							.getSelections()
							.size()
			);

			final AppliedGraph appliedGraph = queryOptions.getAppliedGraph();
			if ( appliedGraph != null && appliedGraph.getSemantic() != null && appliedGraph.getGraph() != null ) {
				this.entityGraphTraversalState = new StandardEntityGraphTraversalStateImpl(
						appliedGraph.getSemantic(), appliedGraph.getGraph() );
			}
			else {
				this.entityGraphTraversalState = null;
			}
		}
		else if ( statement instanceof SqmInsertSelectStatement ) {
			this.domainResults = new ArrayList<>(
					( (SqmInsertSelectStatement<?>) statement ).getSelectQueryPart()
							.getFirstQuerySpec()
							.getSelectClause()
							.getSelectionItems()
							.size()
			);
			this.entityGraphTraversalState = null;
		}
		else {
			this.domainResults = null;
			this.entityGraphTraversalState = null;
		}

		this.queryOptions = queryOptions;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.domainParameterXref = domainParameterXref;
		this.domainParameterBindings = domainParameterBindings;
		this.jpaCriteriaParamResolutions = domainParameterXref.getParameterResolutions()
				.getJpaCriteriaParamResolutions();
	}

	public Map<SqmParameter, MappingModelExpressable> getSqmParameterMappingModelExpressableResolutions() {
		return sqmParameterMappingModelTypes;
	}

	protected Stack<SqlAstProcessingState> getProcessingStateStack() {
		return processingStateStack;
	}

	protected void pushProcessingState(SqlAstProcessingState processingState) {
		pushProcessingState( processingState, new FromClauseIndex( getFromClauseIndex() ) );
	}

	protected void pushProcessingState(SqlAstProcessingState processingState, FromClauseIndex fromClauseIndex) {
		fromClauseIndexStack.push( fromClauseIndex );
		processingStateStack.push( processingState );
	}

	protected void popProcessingStateStack() {
		lastPoppedFromClauseIndex = fromClauseIndexStack.pop();
		lastPoppedProcessingState = processingStateStack.pop();
	}

	private QuerySpec currentQuerySpec() {
		return currentQueryPart().getLastQuerySpec();
	}

	private QueryPart currentQueryPart() {
		final SqlAstQueryPartProcessingState processingState = (SqlAstQueryPartProcessingState) getProcessingStateStack()
				.getCurrent();
		return processingState.getInflightQueryPart();
	}

	protected SqmAliasedNodeCollector currentSqlSelectionCollector() {
		return (SqmAliasedNodeCollector) getCurrentProcessingState().getSqlExpressionResolver();
	}

	protected SqmStatement<?> getStatement() {
		return statement;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlTypeDescriptorIndicators

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return creationContext.getSessionFactory().getTypeConfiguration();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return creationContext.getSessionFactory().getSessionFactoryOptions().getPreferredSqlTypeCodeForBoolean();
	}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FromClauseAccess

	@Override
	public TableGroup findTableGroupOnLeaf(NavigablePath navigablePath) {
		return getFromClauseAccess().findTableGroupOnLeaf( navigablePath );
	}

	@Override
	public TableGroup findTableGroup(NavigablePath navigablePath) {
		return getFromClauseAccess().findTableGroup( navigablePath );
	}

	@Override
	public ModelPart resolveModelPart(NavigablePath navigablePath) {
		// again, assume that the path refers to a TableGroup
		return getFromClauseAccess().findTableGroup( navigablePath ).getModelPart();
	}

	@Override
	public void registerTableGroup(NavigablePath navigablePath, TableGroup tableGroup) {
		throw new UnsupportedOperationException();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstCreationState

	@Override
	public SqlAstCreationContext getCreationContext() {
		return creationContext;
	}

	@Override
	public SqlAstProcessingState getCurrentProcessingState() {
		return processingStateStack.getCurrent();
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return getCurrentProcessingState().getSqlExpressionResolver();
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}

	@Override
	public void registerLockMode(String identificationVariable, LockMode explicitLockMode) {
		throw new UnsupportedOperationException( "Registering lock modes should only be done for result set mappings!" );
	}

	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	public FromClauseIndex getFromClauseIndex() {
		return (FromClauseIndex) getFromClauseAccess();
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();
		if ( fromClauseIndex == null ) {
			return lastPoppedFromClauseIndex;
		}
		else {
			return fromClauseIndex;
		}
	}

	@Override
	public Stack<Clause> getCurrentClauseStack() {
		return currentClauseStack;
	}
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Statements

	@Override
	public SqmTranslation<T> translate() {
		final SqmStatement<?> sqmStatement = getStatement();
		//noinspection unchecked
		final T statement = (T) sqmStatement.accept( this );
		return new StandardSqmTranslation<>(
				statement,
				getJdbcParamsBySqmParam(),
				sqmParameterMappingModelTypes,
				lastPoppedProcessingState.getSqlExpressionResolver(),
				getFromClauseAccess()
		);
	}

	@Override
	public Statement visitStatement(SqmStatement<?> sqmStatement) {
		return (Statement) sqmStatement.accept( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Update statement

	@Override
	public UpdateStatement visitUpdateStatement(SqmUpdateStatement sqmStatement) {
		Map<String, CteStatement> cteStatements = this.visitCteContainer( sqmStatement );

		final SqmRoot<?> sqmTarget = sqmStatement.getTarget();
		final String entityName = sqmTarget.getEntityName();

		final EntityPersister entityDescriptor = getCreationContext()
				.getDomainModel()
				.getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		pushProcessingState(
				new SqlAstProcessingStateImpl(
						getCurrentProcessingState(),
						this,
						getCurrentClauseStack()::getCurrent
				)
		);

		try {
			final NavigablePath rootPath = sqmTarget.getNavigablePath();
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					true,
					rootPath,
					sqmStatement.getRoot().getAlias(),
					() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates( additionalRestrictions, predicate ),
					this,
					getCreationContext()
			);

			if ( ! rootTableGroup.getTableReferenceJoins().isEmpty() ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM UPDATE" );
			}

			if ( sqmTarget.hasJoins() ) {
				throw new HibernateException( "SQM UPDATE does not support explicit joins" );
			}

			getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );

			final List<Assignment> assignments = visitSetClause( sqmStatement.getSetClause() );
			addVersionedAssignment( assignments::add, sqmStatement );

			final FilterPredicate filterPredicate = FilterHelper.createFilterPredicate(
					getLoadQueryInfluencers(),
					(Joinable) entityDescriptor,
					rootTableGroup,
					// todo (6.0): this is temporary until we implement proper alias support
					AbstractSqlAstTranslator.rendersTableReferenceAlias( Clause.UPDATE )
			);
			if ( filterPredicate != null ) {
				additionalRestrictions = SqlAstTreeHelper.combinePredicates( additionalRestrictions, filterPredicate );
			}

			Predicate suppliedPredicate = null;
			final SqmWhereClause whereClause = sqmStatement.getWhereClause();
			if ( whereClause != null && whereClause.getPredicate() != null ) {
				getCurrentClauseStack().push( Clause.WHERE );
				try {
					suppliedPredicate = (Predicate) whereClause.getPredicate().accept( this );
				}
				finally {
					getCurrentClauseStack().pop();
				}
			}

			return new UpdateStatement(
					sqmStatement.isWithRecursive(), cteStatements,
					rootTableGroup.getPrimaryTableReference(),
					assignments,
					SqlAstTreeHelper.combinePredicates( suppliedPredicate, additionalRestrictions ),
					Collections.emptyList()
			);
		}
		finally {
			popProcessingStateStack();
		}
	}

	private static void verifyManipulationImplicitJoin(TableGroup tableGroup) {
		//noinspection StatementWithEmptyBody
		if ( tableGroup instanceof LazyTableGroup && ( (LazyTableGroup) tableGroup ).getUnderlyingTableGroup() == null
				|| tableGroup instanceof VirtualTableGroup ) {
			// this is fine
		}
		else {
			// otherwise...
			throw new QueryException( "Manipulation query may only contain embeddable joins" );
		}
	}

	public void addVersionedAssignment(Consumer<Assignment> assignmentConsumer, SqmUpdateStatement<?> sqmStatement) {
		if ( !sqmStatement.isVersioned() ) {
			return;
		}
		final EntityPersister persister = creationContext.getDomainModel()
				.findEntityDescriptor( sqmStatement.getTarget().getEntityName() );
		if ( !persister.isVersioned() ) {
			throw new SemanticException( "increment option specified for update of non-versioned entity" );
		}

		final BasicType<?> versionType = persister.getVersionType();
		if ( versionType instanceof UserVersionType ) {
			throw new SemanticException( "user-defined version types not supported for increment option" );
		}

		final EntityVersionMapping versionMapping = persister.getVersionMapping();
		final List<ColumnReference> targetColumnReferences = BasicValuedPathInterpretation.from(
				(SqmBasicValuedSimplePath<?>) sqmStatement
						.getRoot()
						.get( versionMapping.getPartName() ),
				this,
				this,
				jpaQueryComplianceEnabled ).getColumnReferences();
		assert targetColumnReferences.size() == 1;

		final ColumnReference versionColumn = targetColumnReferences.get( 0 );
		final Expression value;
		if ( versionMapping.getJdbcMapping().getJdbcTypeDescriptor().isTemporal() ) {
			value = new VersionTypeSeedParameterSpecification( versionType, persister.getVersionJavaTypeDescriptor() );
		}
		else {
			value = new BinaryArithmeticExpression(
					versionColumn,
					ADD,
					new QueryLiteral<>( 1, versionType ),
					versionType
			);
		}
		assignmentConsumer.accept( new Assignment( versionColumn, value ) );
	}

	@Override
	public List<Assignment> visitSetClause(SqmSetClause setClause) {
		final List<Assignment> assignments = new ArrayList<>( setClause.getAssignments().size() );

		for ( SqmAssignment sqmAssignment : setClause.getAssignments() ) {
			final List<ColumnReference> targetColumnReferences = new ArrayList<>();

			pushProcessingState(
					new SqlAstProcessingStateImpl(
							getCurrentProcessingState(),
							this,
							getCurrentClauseStack()::getCurrent) {
						@Override
						public Expression resolveSqlExpression(
								String key,
								Function<SqlAstProcessingState, Expression> creator) {
							final Expression expression = getParentState()
									.getSqlExpressionResolver()
									.resolveSqlExpression( key, creator );
							assert expression instanceof ColumnReference;
							targetColumnReferences.add( (ColumnReference) expression );
							return expression;
						}
					},
					getFromClauseIndex()
			);

			final SqmPathInterpretation assignedPathInterpretation;
			try {
				assignedPathInterpretation = (SqmPathInterpretation) sqmAssignment.getTargetPath().accept( this );
			}
			finally {
				popProcessingStateStack();
			}

			inferrableTypeAccessStack.push( assignedPathInterpretation::getExpressionType );

			final List<ColumnReference> valueColumnReferences = new ArrayList<>();
			pushProcessingState(
					new SqlAstProcessingStateImpl(
							getCurrentProcessingState(),
							this,
							getCurrentClauseStack()::getCurrent) {
						@Override
						public Expression resolveSqlExpression(
								String key,
								Function<SqlAstProcessingState, Expression> creator) {
							final Expression expression = getParentState()
									.getSqlExpressionResolver()
									.resolveSqlExpression( key, creator );
							assert expression instanceof ColumnReference;
							valueColumnReferences.add( (ColumnReference) expression );
							return expression;
						}
					},
					getFromClauseIndex()
			);

			try {
				final SqmExpression<?> assignmentValue = sqmAssignment.getValue();
				final SqmParameter<?> assignmentValueParameter = getSqmParameter( assignmentValue );
				if ( assignmentValueParameter != null ) {
					consumeSqmParameter(
							assignmentValueParameter,
							(index, jdbcParameter) -> assignments.add(
									new Assignment(
											targetColumnReferences.get( index ),
											jdbcParameter
									)
							)
					);
				}
				else {
					final Expression valueExpression = (Expression) assignmentValue.accept( this );

					final int valueExprJdbcCount = getKeyExpressable( valueExpression.getExpressionType() ).getJdbcTypeCount();
					final int assignedPathJdbcCount = getKeyExpressable( assignedPathInterpretation.getExpressionType() )
							.getJdbcTypeCount();

					if ( valueExprJdbcCount != assignedPathJdbcCount ) {
						SqlTreeCreationLogger.LOGGER.debugf(
								"JDBC type count does not match in UPDATE assignment between the assigned-path and the assigned-value; " +
										"this will likely lead to problems executing the query"
						);
					}

					assert assignedPathJdbcCount == valueExprJdbcCount;

					for ( ColumnReference columnReference : targetColumnReferences ) {
						assignments.add(
								new Assignment( columnReference, valueExpression )
						);
					}
				}
			}
			finally {
				popProcessingStateStack();
				inferrableTypeAccessStack.pop();
			}

		}

		return assignments;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Delete statement

	@Override
	public DeleteStatement visitDeleteStatement(SqmDeleteStatement<?> statement) {
		Map<String, CteStatement> cteStatements = this.visitCteContainer( statement );

		final String entityName = statement.getTarget().getEntityName();
		final EntityPersister entityDescriptor = getCreationContext().getDomainModel()
				.getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		pushProcessingState(
				new SqlAstProcessingStateImpl(
						getCurrentProcessingState(),
						this,
						getCurrentClauseStack()::getCurrent
				)
		);

		try {
			final NavigablePath rootPath = statement.getTarget().getNavigablePath();
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					true,
					rootPath,
					statement.getRoot().getAlias(),
					() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates( additionalRestrictions, predicate ),
					this,
					getCreationContext()
			);
			getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );

			if ( !rootTableGroup.getTableReferenceJoins().isEmpty() ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM DELETE" );
			}

			final FilterPredicate filterPredicate = FilterHelper.createFilterPredicate(
					getLoadQueryInfluencers(),
					(Joinable) entityDescriptor,
					rootTableGroup,
					// todo (6.0): this is temporary until we implement proper alias support
					AbstractSqlAstTranslator.rendersTableReferenceAlias( Clause.DELETE )
			);
			if ( filterPredicate != null ) {
				additionalRestrictions = SqlAstTreeHelper.combinePredicates( additionalRestrictions, filterPredicate );
			}

			Predicate suppliedPredicate = null;
			final SqmWhereClause whereClause = statement.getWhereClause();
			if ( whereClause != null && whereClause.getPredicate() != null ) {
				getCurrentClauseStack().push( Clause.WHERE );
				try {
					suppliedPredicate = (Predicate) whereClause.getPredicate().accept( this );
				}
				finally {
					getCurrentClauseStack().pop();
				}
			}

			return new DeleteStatement(
					statement.isWithRecursive(),
					cteStatements,
					rootTableGroup.getPrimaryTableReference(),
					SqlAstTreeHelper.combinePredicates( suppliedPredicate, additionalRestrictions ),
					Collections.emptyList()
			);
		}
		finally {
			popProcessingStateStack();
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Insert-select statement

	@Override
	public InsertStatement visitInsertSelectStatement(SqmInsertSelectStatement<?> sqmStatement) {
		Map<String, CteStatement> cteStatements = this.visitCteContainer( sqmStatement );

		final String entityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister entityDescriptor = getCreationContext().getDomainModel()
				.getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		SqmQueryPart<?> selectQueryPart = sqmStatement.getSelectQueryPart();
		pushProcessingState(
				new SqlAstProcessingStateImpl(
						null,
						this,
						r -> new SqmAliasedNodePositionTracker(
								r,
								selectQueryPart.getFirstQuerySpec()
										.getSelectClause()
										.getSelections()
						),
						getCurrentClauseStack()::getCurrent
				)
		);
		currentClauseStack.push( Clause.INSERT );
		final InsertStatement insertStatement;
		boolean needsVersionInsert = entityDescriptor.isVersioned();

		try {
			final NavigablePath rootPath = sqmStatement.getTarget().getNavigablePath();
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					true,
					rootPath,
					sqmStatement.getTarget().getExplicitAlias(),
					() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates( additionalRestrictions, predicate ),
					this,
					getCreationContext()
			);

			if ( !rootTableGroup.getTableReferenceJoins().isEmpty()
					|| !rootTableGroup.getTableGroupJoins().isEmpty() ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM INSERT-SELECT" );
			}

			getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );

			insertStatement = new InsertStatement(
					sqmStatement.isWithRecursive(),
					cteStatements,
					rootTableGroup.getPrimaryTableReference(),
					Collections.emptyList()
			);

			final List<SqmPath> targetPaths = sqmStatement.getInsertionTargetPaths();
			if ( needsVersionInsert ) {
				final String versionAttributeName = entityDescriptor.getVersionMapping().getVersionAttribute().getAttributeName();
				for ( int i = 0; i < targetPaths.size(); i++ ) {
					final SqmPath<?> path = targetPaths.get( i );
					if ( versionAttributeName.equals( path.getNavigablePath().getLocalName() ) ) {
						needsVersionInsert = false;
					}
					final Assignable assignable = (Assignable) path.accept( this );
					insertStatement.addTargetColumnReferences( assignable.getColumnReferences() );
				}
				if ( needsVersionInsert ) {
					final List<ColumnReference> targetColumnReferences = BasicValuedPathInterpretation.from(
							(SqmBasicValuedSimplePath<?>) sqmStatement.getTarget()
									.get( versionAttributeName ),
							this,
							this,
							jpaQueryComplianceEnabled ).getColumnReferences();
					assert targetColumnReferences.size() == 1;

					insertStatement.addTargetColumnReferences( targetColumnReferences );
				}
			}
			else {
				for ( int i = 0; i < targetPaths.size(); i++ ) {
					final Assignable assignable = (Assignable) targetPaths.get( i ).accept( this );
					insertStatement.addTargetColumnReferences( assignable.getColumnReferences() );
				}
			}
		}
		finally {
			popProcessingStateStack();
			currentClauseStack.pop();
		}

		insertStatement.setSourceSelectStatement(
				visitQueryPart( selectQueryPart )
		);

		if ( needsVersionInsert ) {
			final Expression expression = new VersionTypeSeedParameterSpecification(
					entityDescriptor.getVersionMapping().getJdbcMapping(),
					entityDescriptor.getVersionJavaTypeDescriptor()
			);
			insertStatement.getSourceSelectStatement().forEachQuerySpec(
					querySpec -> {
						querySpec.getSelectClause().addSqlSelection(
								// The position is irrelevant as this is only needed for insert
								new SqlSelectionImpl( 1, 0, expression )
						);
					}
			);
		}

		return insertStatement;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Insert-values statement

	@Override
	public InsertStatement visitInsertValuesStatement(SqmInsertValuesStatement<?> sqmStatement) {
		Map<String, CteStatement> cteStatements = this.visitCteContainer( sqmStatement );
		final String entityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister entityDescriptor = getCreationContext().getDomainModel()
				.getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		pushProcessingState(
				new SqlAstProcessingStateImpl(
						null,
						this,
						getCurrentClauseStack()::getCurrent
				)
		);

		try {
			final NavigablePath rootPath = sqmStatement.getTarget().getNavigablePath();
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
					true,
					rootPath,
					sqmStatement.getTarget().getExplicitAlias(),
					() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates( additionalRestrictions, predicate ),
					this,
					getCreationContext()
			);

			if ( !rootTableGroup.getTableReferenceJoins().isEmpty()
					|| !rootTableGroup.getTableGroupJoins().isEmpty() ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM INSERT-SELECT" );
			}

			getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );

			final InsertStatement insertValuesStatement = new InsertStatement(
					sqmStatement.isWithRecursive(),
					cteStatements,
					rootTableGroup.getPrimaryTableReference(),
					Collections.emptyList()
			);

			List<SqmPath> targetPaths = sqmStatement.getInsertionTargetPaths();
			for ( SqmPath target : targetPaths ) {
				Assignable assignable = (Assignable) target.accept( this );
				insertValuesStatement.addTargetColumnReferences( assignable.getColumnReferences() );
			}

			List<SqmValues> valuesList = sqmStatement.getValuesList();
			for ( SqmValues sqmValues : valuesList ) {
				insertValuesStatement.getValuesList().add( visitValues( sqmValues ) );
			}

			return insertValuesStatement;
		}
		finally {
			popProcessingStateStack();
		}
	}

	@Override
	public Values visitValues(SqmValues sqmValues) {
		Values values = new Values();
		for ( SqmExpression<?> expression : sqmValues.getExpressions() ) {
			values.getExpressions().add( (Expression) expression.accept( this ) );
		}
		return values;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Select statement

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement<?> statement) {
		Map<String, CteStatement> cteStatements = this.visitCteContainer( statement );
		final QueryPart queryPart = visitQueryPart( statement.getQueryPart() );
		final List<DomainResult<?>> domainResults = queryPart.isRoot() ? this.domainResults : Collections.emptyList();
		return new SelectStatement( statement.isWithRecursive(), cteStatements, queryPart, domainResults );
	}

	@Override
	public DynamicInstantiation<?> visitDynamicInstantiation(SqmDynamicInstantiation<?> sqmDynamicInstantiation) {
		final SqmDynamicInstantiationTarget<?> instantiationTarget = sqmDynamicInstantiation.getInstantiationTarget();
		final DynamicInstantiationNature instantiationNature = instantiationTarget.getNature();
		final JavaType<Object> targetTypeDescriptor = interpretInstantiationTarget( instantiationTarget );

		final DynamicInstantiation<?> dynamicInstantiation = new DynamicInstantiation<>(
				instantiationNature,
				targetTypeDescriptor
		);

		for ( SqmDynamicInstantiationArgument<?> sqmArgument : sqmDynamicInstantiation.getArguments() ) {
			final SqmSelectableNode<?> selectableNode = sqmArgument.getSelectableNode();
			final DomainResultProducer<?> argumentResultProducer = (DomainResultProducer<?>) selectableNode.accept( this );
			dynamicInstantiation.addArgument( sqmArgument.getAlias(), argumentResultProducer, this );
		}

		dynamicInstantiation.complete();

		return dynamicInstantiation;
	}

	@SuppressWarnings("unchecked")
	private <X> JavaType<X> interpretInstantiationTarget(SqmDynamicInstantiationTarget<?> instantiationTarget) {
		final Class<X> targetJavaType;

		if ( instantiationTarget.getNature() == DynamicInstantiationNature.LIST ) {
			targetJavaType = (Class<X>) List.class;
		}
		else if ( instantiationTarget.getNature() == DynamicInstantiationNature.MAP ) {
			targetJavaType = (Class<X>) Map.class;
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
	public CteStatement visitCteStatement(SqmCteStatement<?> sqmCteStatement) {
		final CteTable cteTable = createCteTable(
				sqmCteStatement.getCteTable(),
				getCreationContext().getSessionFactory()
		);

		return new CteStatement(
				cteTable,
				visitStatement( sqmCteStatement.getCteDefinition() ),
				sqmCteStatement.getMaterialization(),
				sqmCteStatement.getSearchClauseKind(),
				visitSearchBySpecifications( cteTable, sqmCteStatement.getSearchBySpecifications() ),
				visitCycleColumns( cteTable, sqmCteStatement.getCycleColumns() ),
				findCteColumn( cteTable, sqmCteStatement.getCycleMarkColumn() ),
				sqmCteStatement.getCycleValue(),
				sqmCteStatement.getNoCycleValue()
		);
	}

	protected List<SearchClauseSpecification> visitSearchBySpecifications(
			CteTable cteTable,
			List<SqmSearchClauseSpecification> searchBySpecifications) {
		if ( searchBySpecifications == null || searchBySpecifications.isEmpty() ) {
			return null;
		}
		final int size = searchBySpecifications.size();
		final List<SearchClauseSpecification> searchClauseSpecifications = new ArrayList<>( size );
		for ( int i = 0; i < size; i++ ) {
			final SqmSearchClauseSpecification specification = searchBySpecifications.get( i );
			forEachCteColumn(
					cteTable,
					specification.getCteColumn(),
					cteColumn -> {
						searchClauseSpecifications.add(
								new SearchClauseSpecification(
										cteColumn,
										specification.getSortOrder(),
										specification.getNullPrecedence()
								)
						);
					}
			);
		}

		return searchClauseSpecifications;
	}

	protected CteColumn findCteColumn(CteTable cteTable, SqmCteTableColumn cteColumn) {
		if ( cteColumn == null ) {
			return null;
		}
		final List<CteColumn> cteColumns = cteTable.getCteColumns();
		final int size = cteColumns.size();
		for ( int i = 0; i < size; i++ ) {
			final CteColumn column = cteColumns.get( i );
			if ( cteColumn.getColumnName().equals( column.getColumnExpression() ) ) {
				return column;
			}
		}
		throw new IllegalArgumentException(
				String.format(
						"Couldn't find cte column %s in cte %s!",
						cteColumn.getColumnName(),
						cteTable.getTableExpression()
				)
		);
	}

	protected void forEachCteColumn(CteTable cteTable, SqmCteTableColumn cteColumn, Consumer<CteColumn> consumer) {
		final List<CteColumn> cteColumns = cteTable.getCteColumns();
		final int size = cteColumns.size();
		for ( int i = 0; i < size; i++ ) {
			final CteColumn column = cteColumns.get( i );
			if ( cteColumn.getColumnName().equals( column.getColumnExpression() ) ) {
				consumer.accept( column );
			}
		}
	}

	protected List<CteColumn> visitCycleColumns(CteTable cteTable, List<SqmCteTableColumn> cycleColumns) {
		if ( cycleColumns == null || cycleColumns.isEmpty() ) {
			return null;
		}
		final int size = cycleColumns.size();
		final List<CteColumn> columns = new ArrayList<>( size );
		for ( int i = 0; i < size; i++ ) {
			forEachCteColumn(
					cteTable,
					cycleColumns.get( i ),
					columns::add
			);
		}
		return columns;
	}

	public static CteTable createCteTable(SqmCteTable sqmCteTable, SessionFactoryImplementor factory) {
		final List<SqmCteTableColumn> sqmCteColumns = sqmCteTable.getColumns();
		final List<CteColumn> sqlCteColumns = new ArrayList<>( sqmCteColumns.size() );

		for ( int i = 0; i < sqmCteColumns.size(); i++ ) {
			final SqmCteTableColumn sqmCteTableColumn = sqmCteColumns.get( i );
			ModelPart modelPart = sqmCteTableColumn.getType();
			if ( modelPart instanceof Association ) {
				modelPart = ( (Association) modelPart ).getForeignKeyDescriptor();
			}
			if ( modelPart instanceof EmbeddableValuedModelPart ) {
				modelPart.forEachJdbcType(
						(index, jdbcMapping) -> {
							sqlCteColumns.add(
									new CteColumn(
											sqmCteTableColumn.getColumnName() + "_" + index,
											jdbcMapping
									)
							);
						}
				);
			}
			else {
				sqlCteColumns.add(
						new CteColumn(
								sqmCteTableColumn.getColumnName(),
								( (BasicValuedMapping) modelPart ).getJdbcMapping()
						)
				);
			}
		}

		return new CteTable(
				sqmCteTable.getCteName(),
				sqlCteColumns,
				factory
		);
	}

	@Override
	public Map<String, CteStatement> visitCteContainer(SqmCteContainer consumer) {
		final Collection<SqmCteStatement<?>> sqmCteStatements = consumer.getCteStatements();
		final Map<String, CteStatement> cteStatements = new LinkedHashMap<>( sqmCteStatements.size() );
		for ( SqmCteStatement<?> sqmCteStatement : sqmCteStatements ) {
			final CteStatement cteStatement = visitCteStatement( sqmCteStatement );
			cteStatements.put( cteStatement.getCteTable().getTableExpression(), cteStatement );
		}
		return cteStatements;
	}

	private boolean trackSelectionsForGroup;

	@Override
	public QueryPart visitQueryPart(SqmQueryPart<?> queryPart) {
		return (QueryPart) super.visitQueryPart( queryPart );
	}

	@Override
	public QueryGroup visitQueryGroup(SqmQueryGroup<?> queryGroup) {
		final List<? extends SqmQueryPart<?>> queryParts = queryGroup.getQueryParts();
		final int size = queryParts.size();
		final List<QueryPart> newQueryParts = new ArrayList<>( size );
		final QueryGroup group = new QueryGroup(
				getProcessingStateStack().isEmpty(),
				queryGroup.getSetOperator(),
				newQueryParts
		);

		if ( queryGroup.getOrderByClause() != null && queryGroup.getOrderByClause().hasPositionalSortItem() ) {
			trackSelectionsForGroup = true;
		}

		final SqlAstQueryPartProcessingStateImpl processingState = new SqlAstQueryPartProcessingStateImpl(
				group,
				getCurrentProcessingState(),
				this,
				DelegatingSqmAliasedNodeCollector::new,
				currentClauseStack::getCurrent
		);
		final DelegatingSqmAliasedNodeCollector collector = (DelegatingSqmAliasedNodeCollector) processingState
				.getSqlExpressionResolver();
		final SqmQueryPart<?> sqmQueryPart = currentSqmQueryPart;
		currentSqmQueryPart = queryGroup;
		pushProcessingState( processingState );

		try {
			newQueryParts.add( visitQueryPart( queryParts.get( 0 ) ) );

			collector.setSqmAliasedNodeCollector(
					(SqmAliasedNodeCollector) lastPoppedProcessingState.getSqlExpressionResolver()
			);

			visitOrderByOffsetAndFetch( queryGroup, group );

			trackSelectionsForGroup = false;
			for ( int i = 1; i < size; i++ ) {
				newQueryParts.add( visitQueryPart( queryParts.get( i ) ) );
			}

			return group;
		}
		finally {
			popProcessingStateStack();
			currentSqmQueryPart = sqmQueryPart;
		}
	}

	@Override
	public QuerySpec visitQuerySpec(SqmQuerySpec<?> sqmQuerySpec) {
		final boolean topLevel = getProcessingStateStack().isEmpty();
		final QuerySpec sqlQuerySpec = new QuerySpec(
				topLevel,
				sqmQuerySpec.getFromClause().getNumberOfRoots()
		);
		final SqmSelectClause selectClause = sqmQuerySpec.getSelectClause();

		Predicate originalAdditionalRestrictions = additionalRestrictions;
		additionalRestrictions = null;

		final boolean trackAliasedNodePositions;
		if ( trackSelectionsForGroup ) {
			trackAliasedNodePositions = true;
		}
		else if ( sqmQuerySpec.getOrderByClause() != null && sqmQuerySpec.getOrderByClause().hasPositionalSortItem() ) {
			trackAliasedNodePositions = true;
		}
		else if ( sqmQuerySpec.hasPositionalGroupItem() ) {
			trackAliasedNodePositions = true;
		}
		else {
			// Since JPA Criteria queries can use the same expression object in order or group by items,
			// we need to track the positions to be able to replace the expression in the items with alias references
			// Also see #resolveGroupOrOrderByExpression for more details
			trackAliasedNodePositions = statement.getQuerySource() == SqmQuerySource.CRITERIA
				&& ( sqmQuerySpec.getOrderByClause() != null || !sqmQuerySpec.getGroupByClauseExpressions().isEmpty() );
		}

		final SqlAstProcessingState processingState;
		if ( trackAliasedNodePositions ) {
			processingState = new SqlAstQueryPartProcessingStateImpl(
					sqlQuerySpec,
					getCurrentProcessingState(),
					this,
					r -> new SqmAliasedNodePositionTracker(
							r,
							selectClause.getSelections()
					),
					currentClauseStack::getCurrent
			);
		}
		else {
			processingState = new SqlAstQueryPartProcessingStateImpl(
					sqlQuerySpec,
					getCurrentProcessingState(),
					this,
					currentClauseStack::getCurrent
			);
		}

		final SqmQueryPart<?> sqmQueryPart = currentSqmQueryPart;
		currentSqmQueryPart = sqmQuerySpec;
		pushProcessingState( processingState );

		try {
			if ( topLevel ) {
				orderByFragmentConsumer = new StandardOrderByFragmentConsumer();
			}

			// we want to visit the from-clause first
			visitFromClause( sqmQuerySpec.getFromClause() );

			visitSelectClause( selectClause );

			final SqmWhereClause whereClause = sqmQuerySpec.getWhereClause();
			if ( whereClause != null && whereClause.getPredicate() != null ) {
				currentClauseStack.push( Clause.WHERE );
				try {
					sqlQuerySpec.applyPredicate( (Predicate) whereClause.getPredicate().accept( this ) );
				}
				finally {
					currentClauseStack.pop();
				}
			}

			sqlQuerySpec.setGroupByClauseExpressions( visitGroupByClause( sqmQuerySpec.getGroupByClauseExpressions() ) );
			if ( sqmQuerySpec.getHavingClausePredicate() != null ) {
				sqlQuerySpec.setHavingClauseRestrictions( visitHavingClause( sqmQuerySpec.getHavingClausePredicate() ) );
			}

			visitOrderByOffsetAndFetch( sqmQuerySpec, sqlQuerySpec );

			if ( topLevel && statement instanceof SqmSelectStatement<?> ) {
				orderByFragmentConsumer.visitFragments(
						(orderByFragment, tableGroup) -> {
							orderByFragment.apply( sqlQuerySpec, tableGroup, this );
						}
				);
				orderByFragmentConsumer = null;
				applyCollectionFilterPredicates( sqlQuerySpec );
			}

			joinPathBySqmJoinFullPath.clear();
			return sqlQuerySpec;
		}
		finally {
			if ( additionalRestrictions != null ) {
				sqlQuerySpec.applyPredicate( additionalRestrictions );
			}
			additionalRestrictions = originalAdditionalRestrictions;
			popProcessingStateStack();
			currentSqmQueryPart = sqmQueryPart;
		}
	}

	protected void visitOrderByOffsetAndFetch(SqmQueryPart<?> sqmQueryPart, QueryPart sqlQueryPart) {
		if ( sqmQueryPart.getOrderByClause() != null ) {
			currentClauseStack.push( Clause.ORDER );
			try {
				for ( SqmSortSpecification sortSpecification : sqmQueryPart.getOrderByClause()
						.getSortSpecifications() ) {
					final SortSpecification specification = visitSortSpecification( sortSpecification );
					if ( specification != null ) {
						sqlQueryPart.addSortSpecification( specification );
					}
				}
			}
			finally {
				currentClauseStack.pop();
			}
		}

		sqlQueryPart.setOffsetClauseExpression( visitOffsetExpression( sqmQueryPart.getOffsetExpression() ) );
		sqlQueryPart.setFetchClauseExpression(
				visitFetchExpression( sqmQueryPart.getFetchExpression() ),
				sqmQueryPart.getFetchClauseType()
		);
	}

	private TableGroup findTableGroupByPath(NavigablePath navigablePath) {
		return getFromClauseAccess().getTableGroup( navigablePath );
	}

	private interface OrderByFragmentConsumer {
		void accept(OrderByFragment orderByFragment, TableGroup tableGroup);

		void visitFragments(BiConsumer<OrderByFragment,TableGroup> consumer);
	}

	private static class StandardOrderByFragmentConsumer implements OrderByFragmentConsumer {
		private Map<OrderByFragment, TableGroup> fragments;

		@Override
		public void accept(OrderByFragment orderByFragment, TableGroup tableGroup) {
			if ( fragments == null ) {
				fragments = new LinkedHashMap<>();
			}
			fragments.put( orderByFragment, tableGroup );
		}

		@Override
		public void visitFragments(BiConsumer<OrderByFragment, TableGroup> consumer) {
			if ( fragments == null || fragments.isEmpty() ) {
				return;
			}

			fragments.forEach( consumer );
		}
	}

	protected void applyCollectionFilterPredicates(QuerySpec sqlQuerySpec) {
		final List<TableGroup> roots = sqlQuerySpec.getFromClause().getRoots();
		if ( roots != null && roots.size() == 1 ) {
			final TableGroup root = roots.get( 0 );
			final ModelPartContainer modelPartContainer = root.getModelPart();
			final EntityPersister entityPersister = modelPartContainer.findContainingEntityMapping().getEntityPersister();
			assert entityPersister instanceof Joinable;
			final FilterPredicate filterPredicate = FilterHelper.createFilterPredicate(
					getLoadQueryInfluencers(), (Joinable) entityPersister, root
			);
			if ( filterPredicate != null ) {
				sqlQuerySpec.applyPredicate( filterPredicate );
			}
			if ( CollectionHelper.isNotEmpty( collectionFilterPredicates ) ) {
				root.getTableGroupJoins().forEach(
						tableGroupJoin -> {
							collectionFilterPredicates.forEach( (alias, predicate) -> {
								if ( tableGroupJoin.getJoinedGroup().getGroupAlias().equals( alias ) ) {
									tableGroupJoin.applyPredicate( predicate );
								}
							} );
						}
				);
			}
		}
	}

	@Override
	public SelectClause visitSelectClause(SqmSelectClause selectClause) {
		currentClauseStack.push( Clause.SELECT );
		try {
			final SelectClause sqlSelectClause = currentQuerySpec().getSelectClause();
			if ( selectClause == null ) {
				final SqmFrom<?, ?> implicitSelection = determineImplicitSelection( (SqmQuerySpec<?>) currentSqmQueryPart );
				visitSelection( new SqmSelection<>( implicitSelection, implicitSelection.nodeBuilder() ) );
			}
			else {
				super.visitSelectClause( selectClause );
				sqlSelectClause.makeDistinct( selectClause.isDistinct() );
			}
			return sqlSelectClause;
		}
		finally {
			currentClauseStack.pop();
		}
	}

	protected SqmFrom<?, ?> determineImplicitSelection(SqmQuerySpec<?> querySpec) {
		// Note that this is different from org.hibernate.query.hql.internal.SemanticQueryBuilder.buildInferredSelectClause
		return querySpec.getFromClause().getRoots().get( 0 );
	}

	@Override
	public Void visitSelection(SqmSelection<?> sqmSelection) {
		final List<Map.Entry<String, DomainResultProducer<?>>> resultProducers;
		final SqmSelectableNode<?> selectionNode = sqmSelection.getSelectableNode();
		if ( selectionNode instanceof SqmJpaCompoundSelection<?> ) {
			final SqmJpaCompoundSelection<?> selectableNode = (SqmJpaCompoundSelection<?>) selectionNode;
			resultProducers = new ArrayList<>( selectableNode.getSelectionItems().size() );
			for ( SqmSelectableNode<?> selectionItem : selectableNode.getSelectionItems() ) {
				if ( selectionItem instanceof SqmPath<?> ) {
					prepareForSelection( (SqmPath<?>) selectionItem );
				}
				resultProducers.add(
						new AbstractMap.SimpleEntry<>(
								selectionItem.getAlias(),
								(DomainResultProducer<?>) selectionItem.accept( this )
						)
				);
			}

		}
		else {
			if ( selectionNode instanceof SqmPath<?> ) {
				prepareForSelection( (SqmPath<?>) selectionNode );
			}
			resultProducers = Collections.singletonList(
					new AbstractMap.SimpleEntry<>(
						sqmSelection.getAlias(),
						(DomainResultProducer<?>) selectionNode.accept( this )
					)
			);
		}

		final Stack<SqlAstProcessingState> processingStateStack = getProcessingStateStack();
		final boolean needsDomainResults = domainResults != null && currentClauseContributesToTopLevelSelectClause();
		final boolean collectDomainResults;
		if ( processingStateStack.depth() == 1 ) {
			collectDomainResults = needsDomainResults;
		}
		else {
			final SqlAstProcessingState current = processingStateStack.getCurrent();
			// Since we only want to create domain results for the first/left-most query spec within query groups,
			// we have to check if the current query spec is the left-most.
			// This is the case when all upper level in-flight query groups are still empty
			collectDomainResults = needsDomainResults && processingStateStack.findCurrentFirst(
					processingState -> {
						if ( !( processingState instanceof SqlAstQueryPartProcessingState ) ) {
							return Boolean.FALSE;
						}
						if ( processingState == current ) {
							return null;
						}
						final QueryPart part = ( (SqlAstQueryPartProcessingState) processingState ).getInflightQueryPart();
						if ( part instanceof QueryGroup ) {
							if ( ( (QueryGroup) part ).getQueryParts().isEmpty() ) {
								return null;
							}
						}
						return Boolean.FALSE;
					}
			) == null;
		}
		// this `currentSqlSelectionCollector().next()` is meant solely for resolving
		// literal reference to a selection-item in the order-by or group-by clause.
		// in the case of `DynamicInstantiation`, that ordering should ignore that
		// level here.  visiting the dynamic-instantiation will manage this for its
		// arguments
		if ( collectDomainResults ) {
			resultProducers.forEach(
					entry -> {
						if ( !( entry.getValue() instanceof DynamicInstantiation<?> ) ) {
							currentSqlSelectionCollector().next();
						}
						domainResults.add( entry.getValue().createDomainResult( entry.getKey(), this ) );
					}
			);
		}
		else if ( needsDomainResults ) {
			// We just create domain results for the purpose of creating selections
			// This is necessary for top-level query specs within query groups to avoid cycles
			resultProducers.forEach(
					entry -> {
						if ( !( entry.getValue() instanceof DynamicInstantiation<?> ) ) {
							currentSqlSelectionCollector().next();
						}
						entry.getValue().createDomainResult( entry.getKey(), this );
					}
			);
		}
		else {
			resultProducers.forEach(
					entry -> {
						if ( !( entry.getValue() instanceof DynamicInstantiation<?> ) ) {
							currentSqlSelectionCollector().next();
						}
						entry.getValue().applySqlSelections( this );
					}
			);
		}
		return null;
	}

	private boolean currentClauseContributesToTopLevelSelectClause() {
		// The current clause contributes to the top level select if the clause stack contains just SELECT
		return currentClauseStack.findCurrentFirst( clause -> clause == Clause.SELECT ? null : clause ) == null;
	}

	protected Expression resolveGroupOrOrderByExpression(SqmExpression<?> groupByClauseExpression) {
		final int sqmPosition;
		if ( groupByClauseExpression instanceof SqmAliasedNodeRef ) {
			final int aliasedNodeOrdinal = ( (SqmAliasedNodeRef) groupByClauseExpression ).getPosition();
			sqmPosition = aliasedNodeOrdinal - 1;
		}
		else if ( statement.getQuerySource() == SqmQuerySource.CRITERIA ) {
			// In JPA Criteria we could be using the same expression object for the group/order by and select item
			// We try to find the select item position for this expression here which is not necessarily just an optimization.
			// This is vital to enable the support for parameters in these expressions.
			// Databases usually don't know if a parameter marker will have the same value as another parameter marker
			// and due to that, a database usually complains when seeing something like
			// `select ?, count(*) from dual group by ?` saying that there is a missing group by for the first `?`
			// To avoid this issue, we determine the position and let the SqlAstTranslator handle the rest.
			// Usually it will render `select ?, count(*) from dual group by 1` if supported
			// or force rendering the parameter as literal instead so that the database can see the grouping is fine
			final SqmQuerySpec<?> querySpec = currentSqmQueryPart.getFirstQuerySpec();
			sqmPosition = indexOfExpression( querySpec.getSelectClause().getSelections(), groupByClauseExpression );
		}
		else {
			sqmPosition = -1;
		}
		if ( sqmPosition != -1 ) {
			final List<SqlSelection> selections = currentSqlSelectionCollector().getSelections( sqmPosition );
			assert selections != null : String.format( Locale.ROOT, "No SqlSelections for SQM position `%s`", sqmPosition );
			final List<Expression> expressions = new ArrayList<>( selections.size() );
			OUTER: for ( int i = 0; i < selections.size(); i++ ) {
				final SqlSelection selection = selections.get( i );
				// We skip duplicate selections which can occur when grouping/ordering by an entity alias.
				// Duplication happens because the primary key of an entity usually acts as FK target of collections
				// which is, just like the identifier itself, also registered as selection
				for ( int j = 0; j < i; j++ ) {
					if ( selections.get( j ) == selection ) {
						continue OUTER;
					}
				}
				if ( currentSqmQueryPart instanceof SqmQueryGroup<?> ) {
					// Reusing the SqlSelection for query groups would be wrong because the aliases do no exist
					// So we have to use a literal expression in a new SqlSelection instance to refer to the position
					expressions.add(
							new SqlSelectionExpression(
									new SqlSelectionImpl(
											selection.getJdbcResultSetIndex(),
											selection.getValuesArrayPosition(),
											new QueryLiteral<>(
													selection.getValuesArrayPosition(),
													basicType( Integer.class )
											)
									)
							)
					);
				}
				else {
					expressions.add( new SqlSelectionExpression( selection ) );
				}
			}

			if ( expressions.size() == 1 ) {
				return expressions.get( 0 );
			}

			return new SqlTuple( expressions, null );
		}

		return (Expression) groupByClauseExpression.accept( this );
	}

	private int indexOfExpression(List<? extends SqmAliasedNode<?>> selections, SqmExpression<?> node) {
		final int result = indexOfExpression( 0, selections, node );
		if ( result < 1 ) {
			return -result;
		}
		else {
			return -1;
		}
	}

	private int indexOfExpression(int offset, List<? extends SqmAliasedNode<?>> selections, SqmExpression<?> node) {
		// The idea of this method is that we return the negated index of the position at which we found the node
		// and if we didn't find the node, we return the offset + size to allow for recursive invocation
		// Encoding this into the integer allows us to avoid some kind of mutable state to handle size/offset
		for ( int i = 0; i < selections.size(); i++ ) {
			final SqmSelectableNode<?> selectableNode = selections.get( i ).getSelectableNode();
			if ( selectableNode instanceof SqmDynamicInstantiation<?> ) {
				final int subResult = indexOfExpression(
						offset + i,
						( (SqmDynamicInstantiation<?>) selectableNode ).getArguments(),
						node
				);
				if ( subResult < 0 ) {
					return subResult;
				}
				offset = subResult - i;
			}
			else if ( selectableNode instanceof SqmJpaCompoundSelection<?> ) {
				final List<SqmSelectableNode<?>> selectionItems = ( (SqmJpaCompoundSelection<?>) selectableNode ).getSelectionItems();
				for ( int j = 0; j < selectionItems.size(); j++ ) {
					if ( selectionItems.get( j ) == node ) {
						return -( offset + i + j );
					}
				}
				offset += selectionItems.size();
			}
			else {
				if ( selectableNode == node ) {
					return -( offset + i );
				}
			}
		}
		return offset + selections.size();
	}

	private boolean selectClauseContains(SqmFrom<?, ?> from) {
		final SqmQuerySpec<?> sqmQuerySpec = (SqmQuerySpec<?>) currentSqmQueryPart;
		final List<SqmSelection<?>> selections = sqmQuerySpec.getSelectClause() == null
				? Collections.emptyList()
				: sqmQuerySpec.getSelectClause().getSelections();
		if ( selections.isEmpty() && from instanceof SqmRoot<?> ) {
			return true;
		}
		for ( SqmSelection<?> selection : selections ) {
			if ( selection.getSelectableNode() == from ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<Expression> visitGroupByClause(List<SqmExpression<?>> groupByClauseExpressions) {
		if ( !groupByClauseExpressions.isEmpty() ) {
			currentClauseStack.push( Clause.GROUP );
			try {
				final List<Expression> expressions = new ArrayList<>( groupByClauseExpressions.size() );
				for ( SqmExpression<?> groupByClauseExpression : groupByClauseExpressions ) {
					expressions.add( resolveGroupOrOrderByExpression( groupByClauseExpression ) );
				}
				return expressions;
			}
			finally {
				currentClauseStack.pop();
			}
		}
		return Collections.emptyList();
	}

	@Override
	public Predicate visitHavingClause(SqmPredicate sqmPredicate) {
		if ( sqmPredicate == null ) {
			return null;
		}
		currentClauseStack.push( Clause.HAVING );
		try {
			return (Predicate) sqmPredicate.accept( this );
		}
		finally {
			currentClauseStack.pop();
		}
	}

	@Override
	public Void visitOrderByClause(SqmOrderByClause orderByClause) {
		super.visitOrderByClause( orderByClause );
		return null;
	}

	@Override
	public SortSpecification visitSortSpecification(SqmSortSpecification sortSpecification) {
		final Expression expression = resolveGroupOrOrderByExpression( sortSpecification.getSortExpression() );
		if ( expression == null ) {
			return null;
		}
		return new SortSpecification(
				expression,
				null,
				sortSpecification.getSortOrder(),
				sortSpecification.getNullPrecedence()
		);
	}

	public QuerySpec visitOffsetAndFetchExpressions(QuerySpec sqlQuerySpec, SqmQuerySpec<?> sqmQuerySpec) {
		final Expression offsetExpression = visitOffsetExpression( sqmQuerySpec.getOffsetExpression() );
		final Expression fetchExpression = visitFetchExpression( sqmQuerySpec.getFetchExpression() );
		sqlQuerySpec.setOffsetClauseExpression( offsetExpression );
		sqlQuerySpec.setFetchClauseExpression( fetchExpression, sqmQuerySpec.getFetchClauseType() );
		return sqlQuerySpec;
	}

	@Override
	public Expression visitOffsetExpression(SqmExpression<?> expression) {
		if ( expression == null ) {
			return null;
		}

		currentClauseStack.push( Clause.OFFSET );
		try {
			return (Expression) expression.accept( this );
		}
		finally {
			currentClauseStack.pop();
		}
	}

	@Override
	public Expression visitFetchExpression(SqmExpression<?> expression) {
		if ( expression == null ) {
			return null;
		}

		currentClauseStack.push( Clause.FETCH );
		try {
			return (Expression) expression.accept( this );
		}
		finally {
			currentClauseStack.pop();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FROM clause

	@Override
	public Void visitFromClause(SqmFromClause sqmFromClause) {
		currentClauseStack.push( Clause.FROM );

		try {
			sqmFromClause.visitRoots( this::consumeFromClauseRoot );
		}
		finally {
			currentClauseStack.pop();
		}

		return null;
	}

	protected Predicate additionalRestrictions;

	@SuppressWarnings("WeakerAccess")
	protected void consumeFromClauseRoot(SqmRoot<?> sqmRoot) {
		log.tracef( "Resolving SqmRoot [%s] to TableGroup", sqmRoot );
		final FromClauseIndex fromClauseIndex = getFromClauseIndex();
		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
			log.tracef( "Already resolved SqmRoot [%s] to TableGroup", sqmRoot );
		}
		final QuerySpec currentQuerySpec = currentQuerySpec();
		final TableGroup tableGroup;
		if ( sqmRoot.isCorrelated() ) {
			final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
			final EntityPersister entityDescriptor = resolveEntityPersister( sqmRoot.getReferencedPathSource() );
			if ( sqmRoot.containsOnlyInnerJoins() ) {
				// If we have just inner joins against a correlated root, we can render the joins as references
				final SqmFrom<?, ?> from;
				// If we correlate a join, we have to create a special SqmRoot shell called SqmCorrelatedRootJoin.
				// The only purpose of that is to serve as SqmRoot, which is needed for the FROM clause.
				// It will always contain just a single correlated join though, which is what is actually correlated
				if ( sqmRoot instanceof SqmCorrelatedRootJoin<?> ) {
					assert sqmRoot.getSqmJoins().size() == 1;
					assert sqmRoot.getSqmJoins().get( 0 ).isCorrelated();
					from = sqmRoot.getSqmJoins().get( 0 );
				}
				else {
					from = sqmRoot;
				}
				final TableGroup parentTableGroup = fromClauseIndex.findTableGroup(
						from.getCorrelationParent().getNavigablePath()
				);
				final SqlAliasBase sqlAliasBase = sqlAliasBaseManager.createSqlAliasBase( parentTableGroup.getGroupAlias() );
				tableGroup = new CorrelatedTableGroup(
						parentTableGroup,
						sqlAliasBase,
						currentQuerySpec,
						predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates(
								additionalRestrictions,
								predicate
						),
						sessionFactory
				);

				log.tracef( "Resolved SqmRoot [%s] to correlated TableGroup [%s]", sqmRoot, tableGroup );
				consumeExplicitJoins( from, tableGroup );
				return;
			}
			else {
				final TableGroup parentTableGroup = fromClauseIndex.findTableGroup(
						sqmRoot.getCorrelationParent().getNavigablePath()
				);
				// If we have non-inner joins against a correlated root, we must render the root with a correlation predicate
				tableGroup = entityDescriptor.createRootTableGroup(
						true,
						sqmRoot.getNavigablePath(),
						sqmRoot.getExplicitAlias(),
						() -> predicate -> {},
						this,
						creationContext
				);
				final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
				final NavigablePath navigablePath = sqmRoot.getNavigablePath().append( identifierMapping.getNavigableRole().getNavigableName() );
				final int jdbcTypeCount = identifierMapping.getJdbcTypeCount();
				if ( jdbcTypeCount == 1 ) {
					identifierMapping.forEachSelectable(
							(index, selectable) -> additionalRestrictions = SqlAstTreeHelper.combinePredicates(
									additionalRestrictions,
									new ComparisonPredicate(
											new ColumnReference(
													parentTableGroup.getTableReference( navigablePath, selectable.getContainingTableExpression() ),
													selectable,
													sessionFactory
											),
											ComparisonOperator.EQUAL,
											new ColumnReference(
													tableGroup.getTableReference( navigablePath, selectable.getContainingTableExpression() ),
													selectable,
													sessionFactory
											)
									)
							)
					);
				}
				else {
					final List<Expression> lhs = new ArrayList<>( jdbcTypeCount );
					final List<Expression> rhs = new ArrayList<>( jdbcTypeCount );
					identifierMapping.forEachSelectable(
							(index, selectable) -> {
								lhs.add(
										new ColumnReference(
												parentTableGroup.getTableReference( navigablePath, selectable.getContainingTableExpression() ),
												selectable,
												sessionFactory
										)
								);
								rhs.add(
										new ColumnReference(
												tableGroup.getTableReference( navigablePath, selectable.getContainingTableExpression() ),
												selectable,
												sessionFactory
										)
								);
							}
					);
					additionalRestrictions = SqlAstTreeHelper.combinePredicates(
							additionalRestrictions,
							new ComparisonPredicate(
									new SqlTuple( lhs, identifierMapping ),
									ComparisonOperator.EQUAL,
									new SqlTuple( rhs, identifierMapping )
							)
					);
				}
			}
		}
		else {
			final EntityPersister entityDescriptor = resolveEntityPersister( sqmRoot.getReferencedPathSource() );
			tableGroup = entityDescriptor.createRootTableGroup(
					true,
					sqmRoot.getNavigablePath(),
					sqmRoot.getExplicitAlias(),
					() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates(
							additionalRestrictions,
							predicate
					),
					this,
					creationContext
			);
			final FilterPredicate filterPredicate = FilterHelper.createFilterPredicate(
					getLoadQueryInfluencers(),
					(Joinable) entityDescriptor,
					tableGroup
			);
			if ( filterPredicate != null ) {
				currentQuerySpec.applyPredicate( filterPredicate );
			}
		}

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, tableGroup );

		fromClauseIndex.register( sqmRoot, tableGroup );
		currentQuerySpec.getFromClause().addRoot( tableGroup );

		if ( sqmRoot.getOrderedJoins() == null ) {
			consumeExplicitJoins( sqmRoot, tableGroup );
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.tracef( "Visiting explicit joins for `%s`", sqmRoot.getNavigablePath() );
			}
			TableGroup lastTableGroup = tableGroup;
			for ( SqmJoin<?, ?> join : sqmRoot.getOrderedJoins() ) {
				final TableGroup ownerTableGroup;
				if ( join.getLhs() == null ) {
					ownerTableGroup = tableGroup;
				}
				else {
					if ( join.getLhs() instanceof SqmCorrelation<?, ?> ) {
						ownerTableGroup = fromClauseIndex.findTableGroup(
								( (SqmCorrelation<?, ?>) join.getLhs() ).getCorrelatedRoot().getNavigablePath()
						);
					}
					else {
						ownerTableGroup = fromClauseIndex.findTableGroup( join.getLhs().getNavigablePath() );
					}
				}
				assert ownerTableGroup != null;
				lastTableGroup = consumeExplicitJoin( join, lastTableGroup, ownerTableGroup, false );
			}
		}
	}

	private EntityPersister resolveEntityPersister(EntityDomainType<?> entityDomainType) {
		return creationContext.getDomainModel().getEntityDescriptor( entityDomainType.getHibernateEntityName() );
	}

	protected void consumeExplicitJoins(SqmFrom<?, ?> sqmFrom, TableGroup lhsTableGroup) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Visiting explicit joins for `%s`", sqmFrom.getNavigablePath() );
		}
		sqmFrom.visitSqmJoins(
				sqmJoin -> consumeExplicitJoin( sqmJoin, lhsTableGroup, lhsTableGroup, true )
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected TableGroup consumeExplicitJoin(
			SqmJoin<?, ?> sqmJoin,
			TableGroup lhsTableGroup,
			TableGroup ownerTableGroup,
			boolean transitive) {
		if ( sqmJoin instanceof SqmAttributeJoin<?, ?> ) {
			return consumeAttributeJoin( ( (SqmAttributeJoin<?, ?>) sqmJoin ), lhsTableGroup, ownerTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmCrossJoin<?> ) {
			return consumeCrossJoin( ( (SqmCrossJoin<?>) sqmJoin ), lhsTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmEntityJoin<?> ) {
			return consumeEntityJoin( ( (SqmEntityJoin<?>) sqmJoin ), lhsTableGroup, transitive );
		}
		else {
			throw new InterpretationException( "Could not resolve SqmJoin [" + sqmJoin.getNavigablePath() + "] to TableGroupJoin" );
		}
	}

	private TableGroup consumeAttributeJoin(
			SqmAttributeJoin<?, ?> sqmJoin,
			TableGroup lhsTableGroup,
			TableGroup ownerTableGroup,
			boolean transitive) {

		final SqmPathSource<?> pathSource = sqmJoin.getReferencedPathSource();
		final SqmJoinType sqmJoinType = sqmJoin.getSqmJoinType();

		final TableGroupJoin joinedTableGroupJoin;
		final TableGroup joinedTableGroup;

		final NavigablePath sqmJoinNavigablePath = sqmJoin.getNavigablePath();
		final NavigablePath parentNavigablePath = sqmJoinNavigablePath.getParent();

		final ModelPart modelPart = ownerTableGroup.getModelPart().findSubPart(
				pathSource.getPathName(),
				SqmMappingModelHelper.resolveExplicitTreatTarget( sqmJoin, this )
		);

		final NavigablePath joinPath;
		if ( pathSource instanceof PluralPersistentAttribute ) {
			assert modelPart instanceof PluralAttributeMapping;

			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) modelPart;

			joinPath = getJoinNavigablePath(
					sqmJoinNavigablePath,
					parentNavigablePath,
					pluralAttributeMapping.getPartName()
			);

			joinPathBySqmJoinFullPath.put(
					sqmJoin.getNavigablePath().getFullPath(),
					joinPath.append( CollectionPart.Nature.ELEMENT.getName() )
			);

			joinedTableGroupJoin = pluralAttributeMapping.createTableGroupJoin(
					joinPath,
					ownerTableGroup,
					sqmJoin.getExplicitAlias(),
					sqmJoinType.getCorrespondingSqlJoinType(),
					sqmJoin.isFetched(),
					this
			);
		}
		else {
			assert modelPart instanceof TableGroupJoinProducer;

			joinPath = getJoinNavigablePath( sqmJoinNavigablePath, parentNavigablePath, modelPart.getPartName() );

			joinedTableGroupJoin = ( (TableGroupJoinProducer) modelPart ).createTableGroupJoin(
					joinPath,
					ownerTableGroup,
					sqmJoin.getExplicitAlias(),
					sqmJoinType.getCorrespondingSqlJoinType(),
					sqmJoin.isFetched(),
					this
			);

			// Since this is an explicit join, we force the initialization of a possible lazy table group
			// to retain the cardinality, but only if this is a non-trivial attribute join.
			// Left or inner singular attribute joins without a predicate can be safely optimized away
			if ( sqmJoin.getJoinPredicate() != null || sqmJoinType != SqmJoinType.INNER && sqmJoinType != SqmJoinType.LEFT ) {
				joinedTableGroupJoin.getJoinedGroup().getPrimaryTableReference();
			}
		}

		joinedTableGroup = joinedTableGroupJoin.getJoinedGroup();
		lhsTableGroup.addTableGroupJoin( joinedTableGroupJoin );

		getFromClauseIndex().register( sqmJoin, joinedTableGroup, joinPath );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			if ( sqmJoin.isFetched() ) {
				QueryLogging.QUERY_MESSAGE_LOGGER.debugf( "Join fetch [" + sqmJoinNavigablePath + "] is restricted" );
			}

			if ( joinedTableGroupJoin == null ) {
				throw new IllegalStateException();
			}

			final SqmJoin<?, ?> oldJoin = currentlyProcessingJoin;
			currentlyProcessingJoin = sqmJoin;
			joinedTableGroupJoin.applyPredicate( (Predicate) sqmJoin.getJoinPredicate().accept( this ) );
			currentlyProcessingJoin = oldJoin;
		}

		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, joinedTableGroup );
		}
		return joinedTableGroup;
	}

	private NavigablePath getJoinNavigablePath(
			NavigablePath sqmJoinNavigablePath,
			NavigablePath parentNavigablePath, String partName) {
		if ( parentNavigablePath == null ) {
			return sqmJoinNavigablePath;
		}
		else {
			final NavigablePath elementNavigablePath = joinPathBySqmJoinFullPath.get( parentNavigablePath.getFullPath() );
			if ( elementNavigablePath == null ) {
				return sqmJoinNavigablePath;
			}
			else {
				return elementNavigablePath.append( partName );
			}
		}
	}

	private TableGroup consumeCrossJoin(SqmCrossJoin<?> sqmJoin, TableGroup lhsTableGroup, boolean transitive) {
		final EntityPersister entityDescriptor = resolveEntityPersister( sqmJoin.getReferencedPathSource() );

		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
				true,
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates(
						additionalRestrictions,
						predicate
				),
				this,
				getCreationContext()
		);

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				sqmJoin.getNavigablePath(),
				SqlAstJoinType.CROSS,
				tableGroup
		);

		lhsTableGroup.addTableGroupJoin( tableGroupJoin );

		getFromClauseIndex().register( sqmJoin, tableGroup );

		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
		}
		return tableGroup;
	}

	private TableGroup consumeEntityJoin(SqmEntityJoin<?> sqmJoin, TableGroup lhsTableGroup, boolean transitive) {
		final EntityPersister entityDescriptor = resolveEntityPersister( sqmJoin.getReferencedPathSource() );

		final SqlAstJoinType correspondingSqlJoinType = sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType();
		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
				correspondingSqlJoinType == SqlAstJoinType.INNER || correspondingSqlJoinType == SqlAstJoinType.CROSS ,
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				() -> predicate -> additionalRestrictions = SqlAstTreeHelper.combinePredicates(
						additionalRestrictions,
						predicate
				),
				this,
				getCreationContext()
		);
		getFromClauseIndex().register( sqmJoin, tableGroup );

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				sqmJoin.getNavigablePath(),
				correspondingSqlJoinType,
				tableGroup,
				null
		);

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			final SqmJoin<?, ?> oldJoin = currentlyProcessingJoin;
			currentlyProcessingJoin = sqmJoin;
			tableGroupJoin.applyPredicate( (Predicate) sqmJoin.getJoinPredicate().accept( this ) );
			currentlyProcessingJoin = oldJoin;
		}

		// Note that we add the entity join after processing the predicate because implicit joins needed in there
		// can be just ordered right before the entity join without changing the semantics
		lhsTableGroup.addTableGroupJoin( tableGroupJoin );
		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
		}
		return tableGroup;
	}

	private <X> X prepareReusablePath(SqmPath<?> sqmPath, Supplier<X> supplier) {
		final Consumer<TableGroup> implicitJoinChecker;
		if ( getCurrentProcessingState() instanceof SqlAstQueryPartProcessingState ) {
			implicitJoinChecker = tg -> {};
		}
		else {
			implicitJoinChecker = BaseSqmToSqlAstConverter::verifyManipulationImplicitJoin;
		}
		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();
		final boolean useInnerJoin = currentClauseStack.getCurrent() == Clause.SELECT;
		prepareReusablePath( fromClauseIndex, sqmPath, useInnerJoin, implicitJoinChecker );
		return supplier.get();
	}

	private TableGroup prepareReusablePath(
			FromClauseIndex fromClauseIndex,
			JpaPath<?> sqmPath,
			boolean useInnerJoin,
			Consumer<TableGroup> implicitJoinChecker) {
		final JpaPath<?> parentPath = sqmPath.getParentPath();
		final TableGroup tableGroup = fromClauseIndex.findTableGroup( parentPath.getNavigablePath() );
		if ( tableGroup == null ) {
			final TableGroup parentTableGroup = prepareReusablePath(
					fromClauseIndex,
					parentPath,
					useInnerJoin,
					implicitJoinChecker
			);
			final TableGroup newTableGroup = createTableGroup( parentTableGroup, (SqmPath<?>) parentPath, useInnerJoin );
			if ( newTableGroup != null ) {
				implicitJoinChecker.accept( newTableGroup );
			}
			return newTableGroup;
		}
		return tableGroup;
	}

	private void prepareForSelection(SqmPath<?> joinedPath) {
		final SqmPath<?> lhsPath = joinedPath.getLhs();
		final FromClauseIndex fromClauseIndex = getFromClauseIndex();
		final TableGroup tableGroup = fromClauseIndex.findTableGroup( joinedPath.getNavigablePath() );
		if ( tableGroup == null ) {
			prepareReusablePath( joinedPath, () -> null );

			final NavigablePath navigablePath;
			if ( CollectionPart.Nature.fromNameExact( joinedPath.getNavigablePath().getUnaliasedLocalName() ) != null ) {
				navigablePath = lhsPath.getLhs().getNavigablePath();
			}
			else {
				navigablePath = lhsPath.getNavigablePath();
			}
			// todo (6.0): check again if this really is a "requirement" by the JPA spec and document the reference if it is.
			//  The additional join will filter rows, so there would be no way to just select the FK for a nullable association
			final boolean useInnerJoin = true;
			// INNER join semantics are required per the JPA spec for select items.
			createTableGroup( fromClauseIndex.getTableGroup( navigablePath ), joinedPath, useInnerJoin );
		}
		// When we select a treated path, we must add the type restriction as where clause predicate
		if ( joinedPath instanceof SqmTreatedPath<?, ?> ) {
			final SqmTreatedPath<?, ?> treatedPath = (SqmTreatedPath<?, ?>) joinedPath;
			// todo (6.0): the persister and the table group should participate so we can optimize the joins/selects
			currentQuerySpec().applyPredicate(
					createTreatTypeRestriction( treatedPath.getWrappedPath(), treatedPath.getTreatTarget() )
			);
		}
	}

	private TableGroup createTableGroup(TableGroup parentTableGroup, SqmPath<?> joinedPath, boolean useInnerJoin) {
		final SqmPath<?> lhsPath = joinedPath.getLhs();
		final FromClauseIndex fromClauseIndex = getFromClauseIndex();
		final ModelPart subPart = parentTableGroup.getModelPart().findSubPart(
				joinedPath.getReferencedPathSource().getPathName(),
				lhsPath instanceof SqmTreatedPath
						? resolveEntityPersister( ( (SqmTreatedPath<?, ?>) lhsPath ).getTreatTarget() )
						: null
		);

		final TableGroup tableGroup;
		if ( subPart instanceof TableGroupJoinProducer ) {
			final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) subPart;
			final SqlAstJoinType defaultSqlAstJoinType;

			// The check for joinProducer being an instance of PluralAttributeMapping is necessary
			// for cases where we are consuming a reusable path for special case in which de-referencing a plural path is allowed,
			// i.e.`select key(s.addresses) from Student s`.
			if ( useInnerJoin || joinProducer instanceof PluralAttributeMapping ) {
				defaultSqlAstJoinType = SqlAstJoinType.INNER;
			}
			else {
				defaultSqlAstJoinType = joinProducer.getDefaultSqlAstJoinType( parentTableGroup );
			}
			if ( fromClauseIndex.findTableGroupOnLeaf( parentTableGroup.getNavigablePath() ) == null ) {
				final QuerySpec querySpec = currentQuerySpec();
				// The parent table group is on a parent query, so we need a root table group
				tableGroup = joinProducer.createRootTableGroupJoin(
						joinedPath.getNavigablePath(),
						parentTableGroup,
						null,
						defaultSqlAstJoinType,
						false,
						querySpec::applyPredicate,
						this
				);
				// Force initialization of a possible lazy table group
				tableGroup.getPrimaryTableReference();
				querySpec.getFromClause().addRoot( tableGroup );
			}
			else {
				final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
						joinedPath.getNavigablePath(),
						parentTableGroup,
						null,
						defaultSqlAstJoinType,
						false,
						this
				);
				// Implicit joins in the ON clause of attribute joins need to be added as nested table group joins
				// We don't have to do that for entity joins etc. as these do not have an inherent dependency on the lhs.
				// We can just add the implicit join before the currently processing join
				// See consumeEntityJoin for details
				final boolean nested = currentClauseStack.getCurrent() == Clause.FROM
						&& currentlyProcessingJoin instanceof SqmAttributeJoin<?, ?>;
				if ( nested ) {
					parentTableGroup.addNestedTableGroupJoin( tableGroupJoin );
				}
				else {
					parentTableGroup.addTableGroupJoin( tableGroupJoin );
				}
				tableGroup = tableGroupJoin.getJoinedGroup();
			}

			fromClauseIndex.register( joinedPath, tableGroup );
		}
		else {
			tableGroup = null;
		}
		return tableGroup;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath handling
	//		- Note that SqmFrom references defined in the FROM-clause are already
	//			handled during `#visitFromClause`

	@Override
	public Expression visitRootPath(SqmRoot<?> sqmRoot) {
		final TableGroup resolved = getFromClauseAccess().findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolved != null ) {
			log.tracef( "SqmRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolved );
			return visitTableGroup( resolved, sqmRoot );
		}

		throw new InterpretationException( "SqmRoot not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> sqmJoin) {
		// todo (6.0) : have this resolve to TableGroup instead?
		//		- trying to remove tracking of TableGroupJoin in the x-refs

		final TableGroup existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmAttributeJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmAttributeJoin not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitCrossJoin(SqmCrossJoin<?> sqmJoin) {
		// todo (6.0) : have this resolve to TableGroup instead?
		//		- trying to remove tracking of TableGroupJoin in the x-refs

		final TableGroup existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmCrossJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmCrossJoin not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitQualifiedEntityJoin(SqmEntityJoin sqmJoin) {
		// todo (6.0) : have this resolve to TableGroup instead?
		//		- trying to remove tracking of TableGroupJoin in the x-refs

		final TableGroup existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmEntityJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmEntityJoin not yet resolved to TableGroup" );
	}

	private Expression visitTableGroup(TableGroup tableGroup, SqmFrom<?, ?> path) {
		final ModelPartContainer modelPart = tableGroup.getModelPart();
		final ModelPart keyPart;
		final ModelPart resultPart;
		if ( modelPart instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) modelPart;
			keyPart = toOneAttributeMapping.findSubPart( toOneAttributeMapping.getTargetKeyPropertyName() );
			resultPart = modelPart;
		}
		else if ( modelPart instanceof PluralAttributeMapping ) {
			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) modelPart;
			final CollectionPart elementDescriptor = pluralAttributeMapping.getElementDescriptor();
			if ( elementDescriptor instanceof EntityCollectionPart ) {
				final EntityCollectionPart entityCollectionPart = (EntityCollectionPart) elementDescriptor;
				if ( entityCollectionPart.getKeyTargetMatchPart() instanceof NonAggregatedIdentifierMappingImpl ) {
					keyPart = entityCollectionPart.getElementForeignKeyDescriptor();
				}
				else {
					keyPart = entityCollectionPart.getKeyTargetMatchPart();
				}
			}
			else {
				keyPart = elementDescriptor;
			}
			resultPart = elementDescriptor;
		}
		else if ( modelPart instanceof EntityCollectionPart ) {
			keyPart = ( (EntityCollectionPart) modelPart ).getKeyTargetMatchPart();
			resultPart = modelPart;
		}
		else if ( modelPart instanceof EntityMappingType ) {
			keyPart = ( (EntityMappingType) modelPart ).getIdentifierMapping();
			resultPart = modelPart;
		}
		else {
			keyPart = modelPart;
			resultPart = modelPart;
		}
		final NavigablePath navigablePath;
		if ( resultPart == modelPart ) {
			navigablePath = tableGroup.getNavigablePath();
		}
		else {
			navigablePath = tableGroup.getNavigablePath().append( resultPart.getPartName() );
		}

		final Expression result;
		if ( resultPart instanceof EntityValuedModelPart ) {
			final EntityValuedModelPart mapping = (EntityValuedModelPart) resultPart;
			final boolean expandToAllColumns;
			if ( currentClauseStack.getCurrent() == Clause.GROUP ) {
				// When the table group is known to be fetched i.e. a fetch join
				// but also when the from clause is part of the select clause
				// we need to expand to all columns, as we also expand this to all columns in the select clause
				expandToAllColumns = tableGroup.isFetched() || selectClauseContains( path );
			}
			else {
				expandToAllColumns = false;
			}
			final TableGroup parentGroupToUse = findTableGroup( navigablePath.getParent() );
			result = EntityValuedPathInterpretation.from(
					navigablePath,
					parentGroupToUse == null ? tableGroup : parentGroupToUse,
					mapping,
					expandToAllColumns,
					this
			);
		}
		else if ( resultPart instanceof EmbeddableValuedModelPart ) {
			final EmbeddableValuedModelPart mapping = (EmbeddableValuedModelPart) keyPart;
			result = new EmbeddableValuedPathInterpretation<>(
					mapping.toSqlExpression(
							tableGroup,
							currentClauseStack.getCurrent(),
							this,
							getSqlAstCreationState()
					),
					navigablePath,
					(EmbeddableValuedModelPart) resultPart,
					tableGroup
			);
		}
		else {
			assert resultPart instanceof BasicValuedModelPart;
			final BasicValuedModelPart mapping = (BasicValuedModelPart) keyPart;
			final TableReference tableReference = tableGroup.resolveTableReference(
					navigablePath.append( keyPart.getPartName() ),
					mapping.getContainingTableExpression()
			);

			final Expression expression = getSqlExpressionResolver().resolveSqlExpression(
					SqlExpressionResolver.createColumnReferenceKey(
							tableReference,
							mapping.getSelectionExpression()
					),
					sacs -> new ColumnReference(
							tableReference.getIdentificationVariable(),
							mapping,
							getCreationContext().getSessionFactory()
					)
			);
			final ColumnReference columnReference;
			if ( expression instanceof ColumnReference ) {
				columnReference = (ColumnReference) expression;
			}
			else if ( expression instanceof SqlSelectionExpression ) {
				final Expression selectedExpression = ( (SqlSelectionExpression) expression ).getSelection().getExpression();
				assert selectedExpression instanceof ColumnReference;
				columnReference = (ColumnReference) selectedExpression;
			}
			else {
				throw new UnsupportedOperationException( "Unsupported basic-valued path expression : " + expression );
			}
			result = new BasicValuedPathInterpretation<>(
					columnReference,
					navigablePath,
					(BasicValuedModelPart) resultPart,
					tableGroup
			);
		}

		return withTreatRestriction( result, path );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath

	@Override
	public Expression visitBasicValuedPath(SqmBasicValuedSimplePath<?> sqmPath) {
		final BasicValuedPathInterpretation<?> path = prepareReusablePath(
				sqmPath,
				() -> BasicValuedPathInterpretation.from(
						sqmPath,
						this,
						this,
						jpaQueryComplianceEnabled
				)
		);
		Expression result = path;
		if ( isDuration( sqmPath.getNodeType() ) ) {

			// Durations are stored (at least by default)
			// in a NUMERIC column in seconds with fractional
			// seconds in the decimal places
			// which we need to convert to the given unit
			//
			// This does not work at all for a Duration
			// mapped to a VARCHAR column, in which case
			// we would need to parse the weird format
			// defined by java.time.Duration (a bit hard
			// to do without some custom function).
			// Nor does it work for databases which have
			// a well-defined INTERVAL type, but that is
			// something we could implement.

			//first let's apply the propagated scale
			Expression scaledExpression = applyScale( toSqlExpression( path ) );

			// we use SECOND, not NATIVE, as the unit
			// because that's how a Duration is persisted
			// in a database table column, and how it's
			// returned to a Java client

			if ( adjustedTimestamp != null ) {
				if ( appliedByUnit != null ) {
					throw new IllegalStateException();
				}
				// we're adding this variable duration to the
				// given date or timestamp, producing an
				// adjusted date or timestamp
				result = timestampadd().expression(
						(AllowableFunctionReturnType<?>) adjustedTimestampType,
						new DurationUnit( SECOND, basicType( Long.class ) ),
						scaledExpression,
						adjustedTimestamp
				);
			}
			else if ( appliedByUnit != null ) {
				// we're applying the 'by unit' operator,
				// producing a literal scalar value, so
				// we must convert this duration from
				// nanoseconds to the given unit

				JdbcMappingContainer durationType = scaledExpression.getExpressionType();
				Duration duration;
				if ( durationType.getJdbcMappings()
						.get( 0 )
						.getJdbcTypeDescriptor()
						.isInterval() ) {
					// For interval types, we need to extract the epoch for integer arithmetic for the 'by unit' operator
					duration = new Duration(
							extractEpoch( scaledExpression ),
							SECOND,
							(BasicValuedMapping) durationType
					);
				}
				else {
					// The absolute value of the expression is in seconds
					// as the fractional seconds are in the fraction part as can be seen in DurationJavaType
					duration = new Duration( scaledExpression, SECOND, (BasicValuedMapping) durationType );
				}

				TemporalUnit appliedUnit = appliedByUnit.getUnit().getUnit();
				BasicValuedMapping scalarType = (BasicValuedMapping) appliedByUnit.getNodeType();
				result = new Conversion( duration, appliedUnit, scalarType );
			}
			else {
				// a "bare" Duration value in nanoseconds
				result = scaledExpression;
			}
		}

		return withTreatRestriction( result, sqmPath );
	}

	private Expression extractEpoch(Expression intervalExpression) {
		final BasicType<Integer> intType = getTypeConfiguration().getBasicTypeForJavaType( Integer.class );
		return new SelfRenderingFunctionSqlAstExpression(
				"extract",
				(sqlAppender, sqlAstArguments, walker) ->
						new PatternRenderer(
								creationContext.getSessionFactory()
										.getJdbcServices()
										.getDialect()
										.extractPattern( EPOCH )
						).render( sqlAppender, sqlAstArguments, walker ),
				Arrays.asList( new ExtractUnit( EPOCH, intType ), intervalExpression ),
				intType,
				intType
		);
	}

	@Override
	public Expression visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath<?> sqmPath) {
		return withTreatRestriction(
				prepareReusablePath(
						sqmPath,
						() -> EmbeddableValuedPathInterpretation.from( sqmPath, this, this, jpaQueryComplianceEnabled )
				),
				sqmPath
		);
	}

	@Override
	public Expression visitAnyValuedValuedPath(SqmAnyValuedSimplePath<?> sqmPath) {
		return withTreatRestriction(
				prepareReusablePath( sqmPath, () -> DiscriminatedAssociationPathInterpretation.from( sqmPath, this ) ),
				sqmPath
		);
	}

	@Override
	public Expression visitNonAggregatedCompositeValuedPath(NonAggregatedCompositeSimplePath<?> sqmPath) {
		return withTreatRestriction(
				prepareReusablePath(
						sqmPath,
						() -> NonAggregatedCompositeValuedPathInterpretation.from( sqmPath, this, this )
				),
				sqmPath
		);
	}

	@Override
	public Expression visitEntityValuedPath(SqmEntityValuedSimplePath<?> sqmPath) {
		return withTreatRestriction(
				prepareReusablePath( sqmPath, () -> EntityValuedPathInterpretation.from( sqmPath, this ) ),
				sqmPath
		);
	}

	@Override
	public Expression visitPluralValuedPath(SqmPluralValuedSimplePath<?> sqmPath) {
		return withTreatRestriction(
				prepareReusablePath( sqmPath, () -> PluralValuedSimplePathInterpretation.from( sqmPath, this ) ),
				sqmPath
		);
	}

	@Override
	public Object visitSelfInterpretingSqmPath(SelfInterpretingSqmPath<?> sqmPath) {
		return prepareReusablePath( sqmPath, () -> sqmPath.interpret( this, this, jpaQueryComplianceEnabled ) );
	}

	@Override
	public Expression visitMaxElementPath(SqmMaxElementPath<?> path) {
		return createCorrelatedAggregateSubQuery( path, false, true );
	}

	@Override
	public Expression visitMinElementPath(SqmMinElementPath<?> path) {
		return createCorrelatedAggregateSubQuery( path, false, false );
	}

	@Override
	public Expression visitMaxIndexPath(SqmMaxIndexPath<?> path) {
		return createCorrelatedAggregateSubQuery( path, true, true );
	}

	@Override
	public Expression visitMinIndexPath(SqmMinIndexPath<?> path) {
		return createCorrelatedAggregateSubQuery( path, true, false );
	}

	@Override
	public Expression visitPluralAttributeSizeFunction(SqmCollectionSize function) {
		final SqmPath<?> pluralPath = function.getPluralPath();

		prepareReusablePath( pluralPath, () -> null );
		final TableGroup parentTableGroup = getFromClauseAccess().getTableGroup( pluralPath.getNavigablePath().getParent() );
		assert parentTableGroup != null;

		final PluralAttributeMapping collectionPart = (PluralAttributeMapping) parentTableGroup.getModelPart().findSubPart(
				pluralPath.getNavigablePath().getUnaliasedLocalName(),
				null
		);
		assert collectionPart != null;

		final QuerySpec subQuerySpec = new QuerySpec( false );
		pushProcessingState(
				new SqlAstQueryPartProcessingStateImpl(
						subQuerySpec,
						getCurrentProcessingState(),
						this,
						currentClauseStack::getCurrent
				)
		);
		try {
			final TableGroup tableGroup = collectionPart.createRootTableGroup(
					true,
					pluralPath.getNavigablePath(),
					null,
					() -> subQuerySpec::applyPredicate,
					this,
					creationContext
			);

			getFromClauseAccess().registerTableGroup( pluralPath.getNavigablePath(), tableGroup );
			subQuerySpec.getFromClause().addRoot( tableGroup );

			final AbstractSqmSelfRenderingFunctionDescriptor functionDescriptor = (AbstractSqmSelfRenderingFunctionDescriptor) creationContext
					.getSessionFactory()
					.getQueryEngine()
					.getSqmFunctionRegistry()
					.findFunctionDescriptor( "count" );
			final BasicType<Integer> integerType = creationContext.getDomainModel()
					.getTypeConfiguration()
					.getBasicTypeForJavaType( Integer.class );
			final Expression expression = new SelfRenderingAggregateFunctionSqlAstExpression(
					functionDescriptor.getName(),
					functionDescriptor::render,
					Collections.singletonList( new QueryLiteral<>( 1, integerType ) ),
					null,
					integerType,
					integerType
			);
			subQuerySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 1, 0, expression ) );

			subQuerySpec.applyPredicate(
					collectionPart.getKeyDescriptor().generateJoinPredicate(
							parentTableGroup,
							tableGroup,
							SqlAstJoinType.INNER,
							getSqlExpressionResolver(),
							creationContext
					)
			);
		}
		finally {
			popProcessingStateStack();
		}
		return subQuerySpec;
	}

	@Override
	public Object visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath<?> path) {
		// SemanticQueryBuilder applies the index expression to the generated join
		return path.getLhs().accept( this );
	}

	@Override
	public Object visitMapEntryFunction(SqmMapEntryReference<?, ?> entryRef) {
		final SqmPath<?> mapPath = entryRef.getMapPath();
		prepareReusablePath( mapPath, () -> null );

		final NavigablePath mapNavigablePath = mapPath.getNavigablePath();
		final TableGroup tableGroup = getFromClauseAccess().resolveTableGroup(
				mapNavigablePath,
				(navigablePath) -> {
					final TableGroup parentTableGroup = getFromClauseAccess().getTableGroup( mapNavigablePath.getParent() );
					final PluralAttributeMapping mapAttribute = (PluralAttributeMapping) parentTableGroup.getModelPart().findSubPart( mapNavigablePath.getLocalName(), null );

					final TableGroupJoin tableGroupJoin = mapAttribute.createTableGroupJoin(
							mapNavigablePath,
							parentTableGroup,
							null,
							SqlAstJoinType.INNER,
							false,
							sqlAliasBaseManager,
							getSqlExpressionResolver(),
							creationContext
					);

					parentTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);

		final PluralAttributeMapping mapDescriptor = (PluralAttributeMapping) tableGroup.getModelPart();

		final CollectionPart indexDescriptor = mapDescriptor.getIndexDescriptor();
		final NavigablePath indexNavigablePath = mapNavigablePath.append( indexDescriptor.getPartName() );
		final DomainResult<Object> indexResult = indexDescriptor.createDomainResult(
				indexNavigablePath,
				tableGroup,
				null,
				this
		);

		final CollectionPart valueDescriptor = mapDescriptor.getElementDescriptor();
		final NavigablePath valueNavigablePath = mapNavigablePath.append( valueDescriptor.getPartName() );
		final DomainResult<Object> valueResult = valueDescriptor.createDomainResult(
				valueNavigablePath,
				tableGroup,
				null,
				this
		);

		return new DomainResultProducer<Map.Entry<Object, Object>>() {
			@Override
			public DomainResult<Map.Entry<Object, Object>> createDomainResult(
					String resultVariable,
					DomainResultCreationState creationState) {
				final JavaType<Map.Entry<Object, Object>> mapEntryDescriptor = getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.resolveDescriptor( Map.Entry.class );
				return new SqmMapEntryResult<>( indexResult, valueResult, resultVariable, mapEntryDescriptor );
			}
		};
	}

	protected Expression createCorrelatedAggregateSubQuery(
			AbstractSqmSpecificPluralPartPath<?> pluralPartPath,
			boolean index,
			boolean max) {
		prepareReusablePath( pluralPartPath, () -> null );

		final PluralAttributeMapping mappingModelExpressable = (PluralAttributeMapping) determineValueMapping(
				pluralPartPath.getPluralDomainPath() );
		final FromClauseAccess parentFromClauseAccess = getFromClauseAccess();
		final QuerySpec subQuerySpec = new QuerySpec( false );
		pushProcessingState(
				new SqlAstQueryPartProcessingStateImpl(
						subQuerySpec,
						getCurrentProcessingState(),
						this,
						currentClauseStack::getCurrent
				)
		);
		try {
			final TableGroup tableGroup = mappingModelExpressable.createRootTableGroup(
					true,
					pluralPartPath.getNavigablePath(),
					null,
					() -> subQuerySpec::applyPredicate,
					this,
					creationContext
			);

			getFromClauseAccess().registerTableGroup( pluralPartPath.getNavigablePath(), tableGroup );
			subQuerySpec.getFromClause().addRoot( tableGroup );

			final AbstractSqmSelfRenderingFunctionDescriptor functionDescriptor = (AbstractSqmSelfRenderingFunctionDescriptor) creationContext
					.getSessionFactory()
					.getQueryEngine()
					.getSqmFunctionRegistry()
					.findFunctionDescriptor( max ? "max" : "min" );
			final CollectionPart collectionPart = index
					? mappingModelExpressable.getIndexDescriptor()
					: mappingModelExpressable.getElementDescriptor();
			final ModelPart modelPart;
			if ( collectionPart instanceof EntityAssociationMapping ) {
				modelPart = ( (EntityAssociationMapping) collectionPart ).getKeyTargetMatchPart();
			}
			else {
				modelPart = collectionPart;
			}
			final List<Expression> arguments = new ArrayList<>( 1 );
			final NavigablePath navigablePath = pluralPartPath.getNavigablePath();
			final int jdbcTypeCount = modelPart.getJdbcTypeCount();
			final List<Expression> tupleElements;
			if ( jdbcTypeCount == 1 ) {
				tupleElements = arguments;
			}
			else {
				tupleElements = new ArrayList<>( jdbcTypeCount );
			}
			modelPart.forEachSelectable(
					(selectionIndex, selectionMapping) -> {
						tupleElements.add(
								new ColumnReference(
										tableGroup.getTableReference(
												navigablePath,
												selectionMapping.getContainingTableExpression()
										),
										selectionMapping,
										creationContext.getSessionFactory()
								)
						);
					}
			);
			if ( jdbcTypeCount != 1 ) {
				arguments.add( new SqlTuple( tupleElements, modelPart ) );
			}
			final Expression expression = new SelfRenderingAggregateFunctionSqlAstExpression(
					functionDescriptor.getName(),
					functionDescriptor::render,
					(List<SqlAstNode>) (List<?>) arguments,
					null,
					(AllowableFunctionReturnType<?>) modelPart.getJdbcMappings().get( 0 ),
					modelPart
			);
			subQuerySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( 1, 0, expression ) );

			subQuerySpec.applyPredicate(
					mappingModelExpressable.getKeyDescriptor().generateJoinPredicate(
							parentFromClauseAccess.findTableGroup( pluralPartPath.getPluralDomainPath().getNavigablePath().getParent() ),
							tableGroup,
							SqlAstJoinType.INNER,
							getSqlExpressionResolver(),
							creationContext
					)
			);
		}
		finally {
			popProcessingStateStack();
		}
		return subQuerySpec;
	}

	private Expression withTreatRestriction(Expression expression, SqmPath<?> path) {
		final SqmPath<?> lhs = path.getLhs();
		if ( lhs instanceof SqmTreatedPath<?, ?> ) {
			final SqmTreatedPath<?, ?> treatedPath = (SqmTreatedPath<?, ?>) lhs;
			final Class<?> treatTargetJavaType = treatedPath.getTreatTarget().getJavaType();
			final Class<?> originalJavaType = treatedPath.getWrappedPath().getJavaType();
			if ( treatTargetJavaType.isAssignableFrom( originalJavaType ) ) {
				// Treating a node to a super type can be ignored
				return expression;
			}
			return createCaseExpression( treatedPath.getWrappedPath(), treatedPath.getTreatTarget(), expression );
		}
		return expression;
	}

	private Expression createCaseExpression(SqmPath<?> lhs, EntityDomainType<?> treatTarget, Expression expression) {
		final BasicValuedMapping mappingModelExpressable = (BasicValuedMapping) expression.getExpressionType();
		final List<CaseSearchedExpression.WhenFragment> whenFragments = new ArrayList<>( 1 );
		whenFragments.add(
				new CaseSearchedExpression.WhenFragment(
						createTreatTypeRestriction( lhs, treatTarget ),
						expression
				)
		);
		return new CaseSearchedExpression(
				mappingModelExpressable,
				whenFragments,
				new QueryLiteral<>( null, mappingModelExpressable )
		);
	}

	private Predicate createTreatTypeRestriction(SqmPath<?> lhs, EntityDomainType<?> treatTarget) {
		final MappingMetamodel domainModel = getCreationContext().getDomainModel();
		final EntityPersister entityDescriptor = domainModel.findEntityDescriptor( treatTarget.getHibernateEntityName() );
		final Set<String> subclassEntityNames = entityDescriptor.getEntityMetamodel().getSubclassEntityNames();
		final Expression typeExpression = (Expression) lhs.type().accept( this );
		if ( subclassEntityNames.size() == 1 ) {
			return new ComparisonPredicate(
					typeExpression,
					ComparisonOperator.EQUAL,
					new EntityTypeLiteral( entityDescriptor )
			);
		}
		else {
			final List<Expression> typeLiterals = new ArrayList<>( subclassEntityNames.size() );
			for ( String subclassEntityName : subclassEntityNames ) {
				typeLiterals.add( new EntityTypeLiteral( domainModel.findEntityDescriptor( subclassEntityName ) ) );
			}
			return new InListPredicate( typeExpression, typeLiterals );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// General expressions

	@Override
	public Expression visitLiteral(SqmLiteral<?> literal) {
		final Supplier<MappingModelExpressable> inferableTypeAccess = inferrableTypeAccessStack.getCurrent();

		if ( literal instanceof SqmLiteralNull ) {
			MappingModelExpressable<?> mappingModelExpressable = inferableTypeAccess.get();
			if ( mappingModelExpressable == null ) {
				mappingModelExpressable = determineCurrentExpressable( literal );
			}
			if ( mappingModelExpressable instanceof BasicValuedMapping ) {
				return new QueryLiteral<>( null, (BasicValuedMapping) mappingModelExpressable );
			}
			final MappingModelExpressable<?> keyExpressable = getKeyExpressable( mappingModelExpressable );
			if ( keyExpressable == null ) {
				// Default to the Object type
				return new QueryLiteral<>( null, basicType( Object.class ) );
			}

			final List<Expression> expressions = new ArrayList<>( keyExpressable.getJdbcTypeCount() );

			keyExpressable.forEachJdbcType(
					(index, jdbcMapping) -> expressions.add(
							new QueryLiteral<>(
									null,
									(BasicValuedMapping) jdbcMapping
							)
					)
			);
			return new SqlTuple( expressions, mappingModelExpressable );
		}

		final MappingModelExpressable<?> inferableExpressable = inferableTypeAccess.get();

		if ( inferableExpressable instanceof ConvertibleModelPart ) {
			final ConvertibleModelPart convertibleModelPart = (ConvertibleModelPart) inferableExpressable;
			final BasicValueConverter<Object, Object> valueConverter = convertibleModelPart.getValueConverter();

			if ( valueConverter != null ) {
				final Object literalValue = literal.getLiteralValue();
				final Object sqlLiteralValue;

				if ( valueConverter.getDomainJavaDescriptor().getJavaTypeClass().isInstance( literalValue ) ) {
					sqlLiteralValue = valueConverter.toRelationalValue( literalValue );
				}
				else {
					if ( !valueConverter.getRelationalJavaDescriptor().getJavaTypeClass().isInstance( literalValue ) ) {
						throw new SqlTreeCreationException(
								String.format(
										Locale.ROOT,
										"QueryLiteral type [`%s`] did not match domain Java-type [`%s`] nor JDBC Java-type [`%s`]",
										literalValue.getClass(),
										valueConverter.getDomainJavaDescriptor().getJavaTypeClass().getName(),
										valueConverter.getRelationalJavaDescriptor().getJavaTypeClass().getName()
								)
						);
					}
					sqlLiteralValue = literalValue;
				}

				return new QueryLiteral<>( sqlLiteralValue, convertibleModelPart );
			}
		}
		// Special case for when we create an entity literal through the JPA CriteriaBuilder.literal API
		else if ( inferableExpressable instanceof EntityDiscriminatorMapping ) {
			final EntityDiscriminatorMapping discriminatorMapping = (EntityDiscriminatorMapping) inferableExpressable;
			final Object literalValue = literal.getLiteralValue();
			final EntityPersister mappingDescriptor;
			if ( literalValue instanceof Class<?> ) {
				mappingDescriptor = getCreationContext().getDomainModel()
						.getEntityDescriptor( (Class<?>) literalValue );
			}
			else {
				final JavaType<?> javaTypeDescriptor = discriminatorMapping.getJdbcMapping().getJavaTypeDescriptor();
				final Object discriminatorValue;
				if ( javaTypeDescriptor.getJavaTypeClass().isInstance( literalValue ) ) {
					discriminatorValue = literalValue;
				}
				else if ( literalValue instanceof CharSequence ) {
					discriminatorValue = javaTypeDescriptor.fromString( (CharSequence) literalValue );
				}
				else if ( creationContext.getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled() ) {
					discriminatorValue = literalValue;
				}
				else {
					discriminatorValue = javaTypeDescriptor.coerce( literalValue, null );
				}
				final String entityName = discriminatorMapping.getConcreteEntityNameForDiscriminatorValue(
						discriminatorValue
				);
				mappingDescriptor = getCreationContext().getDomainModel()
						.getEntityDescriptor( entityName );
			}
			return new EntityTypeLiteral( mappingDescriptor );
		}

		final MappingModelExpressable<?> expressable;
		final MappingModelExpressable<?> localExpressable = SqmMappingModelHelper.resolveMappingModelExpressable(
				literal,
				getCreationContext().getDomainModel(),
				getFromClauseAccess()::findTableGroup
		);
		if ( localExpressable == null ) {
			expressable = getElementExpressable( inferableExpressable );
		}
		else {
			final MappingModelExpressable<?> elementExpressable = getElementExpressable( localExpressable );
			if ( elementExpressable instanceof BasicType<?> ) {
				expressable = InferredBasicValueResolver.resolveSqlTypeIndicators(
						this,
						(BasicType<?>) elementExpressable,
						literal.getJavaTypeDescriptor()
				);
			}
			else {
				expressable = elementExpressable;
			}
		}

		if ( expressable instanceof BasicValuedMapping ) {
			return new QueryLiteral<>(
					literal.getLiteralValue(),
					(BasicValuedMapping) expressable
			);
		}
		// Handling other values might seem unnecessary, but with JPA Criteria it is totally possible to have such literals
		else if ( expressable instanceof EmbeddableValuedModelPart ) {
			final EmbeddableValuedModelPart embeddableValuedModelPart = (EmbeddableValuedModelPart) expressable;
			final List<Expression> list = new ArrayList<>( embeddableValuedModelPart.getJdbcTypeCount() );
			embeddableValuedModelPart.forEachJdbcValue(
					literal.getLiteralValue(),
					null,
					(selectionIndex, value, jdbcMapping) -> {
						list.add( new QueryLiteral<>( value, (BasicValuedMapping) jdbcMapping ) );
					},
					null
			);
			return new SqlTuple( list, expressable );
		}
		else if ( expressable instanceof EntityValuedModelPart ) {
			final EntityValuedModelPart entityValuedModelPart = (EntityValuedModelPart) expressable;
			final Object associationKey;
			final ModelPart associationKeyPart;
			if ( entityValuedModelPart instanceof Association ) {
				final Association association = (Association) entityValuedModelPart;
				final ForeignKeyDescriptor foreignKeyDescriptor = association.getForeignKeyDescriptor();
				associationKey = foreignKeyDescriptor.getAssociationKeyFromSide(
						literal.getLiteralValue(),
						association.getSideNature(),
						null
				);
				associationKeyPart = foreignKeyDescriptor.getPart( association.getSideNature() );
			}
			else {
				final EntityIdentifierMapping identifierMapping = entityValuedModelPart.getEntityMappingType()
						.getIdentifierMapping();
				associationKeyPart = identifierMapping;
				associationKey = identifierMapping.getIdentifier(
						literal.getLiteralValue(),
						getCreationContext().getSessionFactory());
			}
			if ( associationKeyPart instanceof BasicValuedMapping ) {
				return new QueryLiteral<>(
						associationKey,
						(BasicValuedMapping) associationKeyPart
				);
			}
			else {
				final List<Expression> list = new ArrayList<>( associationKeyPart.getJdbcTypeCount() );
				associationKeyPart.forEachJdbcValue(
						associationKey,
						null,
						(selectionIndex, value, jdbcMapping) -> {
							list.add( new QueryLiteral<>( value, (BasicValuedMapping) jdbcMapping ) );
						},
						null
				);
				return new SqlTuple( list, associationKeyPart );
			}
		}
		else {
			throw new NotYetImplementedFor6Exception(
					expressable == null ? literal.getLiteralValue().getClass() : expressable.getClass()
			);
		}
	}

	private MappingModelExpressable<?> getKeyExpressable(JdbcMappingContainer mappingModelExpressable) {
		if ( mappingModelExpressable instanceof EntityAssociationMapping ) {
			return ( (EntityAssociationMapping) mappingModelExpressable ).getKeyTargetMatchPart();
		}
		else {
			return (MappingModelExpressable<?>) mappingModelExpressable;
		}
	}

	private MappingModelExpressable<?> getElementExpressable(MappingModelExpressable<?> mappingModelExpressable) {
		if ( mappingModelExpressable instanceof PluralAttributeMapping ) {
			return ( (PluralAttributeMapping) mappingModelExpressable ).getElementDescriptor();
		}
		else {
			return mappingModelExpressable;
		}
	}

	private final Map<SqmParameter, List<List<JdbcParameter>>> jdbcParamsBySqmParam = new IdentityHashMap<>();
	private final JdbcParameters jdbcParameters = new JdbcParametersImpl();

	@Override
	public Map<SqmParameter, List<List<JdbcParameter>>> getJdbcParamsBySqmParam() {
		return jdbcParamsBySqmParam;
	}

	@Override
	public Expression visitNamedParameterExpression(SqmNamedParameter<?> expression) {
		return consumeSqmParameter( expression );
	}

	protected Expression consumeSqmParameter(
			SqmParameter sqmParameter,
			BiConsumer<Integer,JdbcParameter> jdbcParameterConsumer) {
		final MappingModelExpressable valueMapping = determineValueMapping( sqmParameter );

		final List<JdbcParameter> jdbcParametersForSqm = new ArrayList<>();

		resolveSqmParameter(
				sqmParameter,
				valueMapping,
				(index,jdbcParameter) -> {
					jdbcParameterConsumer.accept( index, jdbcParameter );
					jdbcParametersForSqm.add( jdbcParameter );
				}
		);

		this.jdbcParameters.addParameters( jdbcParametersForSqm );
		this.jdbcParamsBySqmParam
				.computeIfAbsent( sqmParameter, k -> new ArrayList<>( 1 ) )
				.add( jdbcParametersForSqm );

		final QueryParameterImplementor<?> queryParameter = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding<?> binding = domainParameterBindings.getBinding( queryParameter );
		if ( binding.setType( valueMapping ) ) {
			replaceJdbcParametersType(
					sqmParameter,
					domainParameterXref.getSqmParameters( queryParameter ),
					valueMapping
			);
		}
		return new SqmParameterInterpretation(
				sqmParameter,
				queryParameter,
				jdbcParametersForSqm,
				valueMapping,
				qp -> binding
		);
	}

	private void replaceJdbcParametersType(
			SqmParameter sourceSqmParameter,
			List<SqmParameter> sqmParameters,
			MappingModelExpressable<?> valueMapping) {
		final JdbcMapping jdbcMapping = valueMapping.getJdbcMappings().get( 0 );
		for ( SqmParameter<?> sqmParameter : sqmParameters ) {
			if ( sqmParameter == sourceSqmParameter ) {
				continue;
			}
			sqmParameterMappingModelTypes.put( sqmParameter, valueMapping );
			final List<List<JdbcParameter>> jdbcParamsForSqmParameter = jdbcParamsBySqmParam.get( sqmParameter );
			if ( jdbcParamsForSqmParameter != null ) {
				for ( List<JdbcParameter> parameters : jdbcParamsForSqmParameter ) {
					assert parameters.size() == 1;
					final JdbcParameter jdbcParameter = parameters.get( 0 );
					if ( ( (SqlExpressable) jdbcParameter ).getJdbcMapping() != valueMapping ) {
						final JdbcParameter newJdbcParameter = new JdbcParameterImpl( jdbcMapping );
						parameters.set( 0, newJdbcParameter );
						jdbcParameters.getJdbcParameters().remove( jdbcParameter );
						jdbcParameters.getJdbcParameters().add( newJdbcParameter );
					}
				}
			}
		}
	}

	protected Expression consumeSqmParameter(SqmParameter sqmParameter) {
		if ( sqmParameter.allowMultiValuedBinding() ) {
			final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter( sqmParameter );
			final QueryParameterBinding domainParamBinding = domainParameterBindings.getBinding( domainParam );

			if ( !domainParamBinding.isMultiValued() ) {
				return consumeSingleSqmParameter( sqmParameter );
			}

			final Collection bindValues = domainParamBinding.getBindValues();
			final List<Expression> expressions = new ArrayList<>( bindValues.size());
			boolean first = true;
			for ( Object bindValue : bindValues ) {
				final SqmParameter sqmParamToConsume;
				// for each bind value create an "expansion"
				if ( first ) {
					sqmParamToConsume = sqmParameter;
					first = false;
				}
				else {
					sqmParamToConsume = sqmParameter.copy();
					domainParameterXref.addExpansion( domainParam, sqmParameter, sqmParamToConsume );
				}
				expressions.add( consumeSingleSqmParameter( sqmParamToConsume ) );
			}

			return new SqlTuple( expressions, null );
		}
		else {
			return consumeSingleSqmParameter( sqmParameter );
		}
	}

	protected Expression consumeSingleSqmParameter(SqmParameter sqmParameter) {
		final MappingModelExpressable valueMapping = determineValueMapping( sqmParameter );

		final List<JdbcParameter> jdbcParametersForSqm = new ArrayList<>();

		resolveSqmParameter(
				sqmParameter,
				valueMapping,
				jdbcParametersForSqm::add
		);

		this.jdbcParameters.addParameters( jdbcParametersForSqm );
		this.jdbcParamsBySqmParam
				.computeIfAbsent( sqmParameter, k -> new ArrayList<>( 1 ) )
				.add( jdbcParametersForSqm );

		final QueryParameterImplementor<?> queryParameter = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding<?> binding = domainParameterBindings.getBinding( queryParameter );
		if ( binding.setType( valueMapping ) ) {
			replaceJdbcParametersType(
					sqmParameter,
					domainParameterXref.getSqmParameters( queryParameter ),
					valueMapping
			);
		}
		return new SqmParameterInterpretation(
				sqmParameter,
				queryParameter,
				jdbcParametersForSqm,
				valueMapping,
				qp -> binding
		);
	}

	protected MappingModelExpressable<?> lenientlyResolveMappingExpressable(SqmExpressable<?> nodeType) {
		try {
			return resolveMappingExpressable( nodeType );
		}
		catch (UnsupportedOperationException e) {
			// todo (6.0) : log?
			return null;
		}
	}

	protected MappingModelExpressable<?> resolveMappingExpressable(SqmExpressable<?> nodeType) {
		final MappingModelExpressable valueMapping = getCreationContext().getDomainModel().resolveMappingExpressable(
				nodeType,
				this::findTableGroupByPath
		);

		if ( valueMapping == null ) {
			final Supplier<MappingModelExpressable> currentExpressableSupplier = inferrableTypeAccessStack.getCurrent();
			if ( currentExpressableSupplier != null ) {
				return currentExpressableSupplier.get();
			}
		}

		if ( valueMapping == null ) {
			throw new ConversionException( "Could not determine ValueMapping for SqmExpressable: " + nodeType );
		}

		return valueMapping;
	}

	protected MappingModelExpressable<?> determineValueMapping(SqmExpression<?> sqmExpression) {
		if ( sqmExpression instanceof SqmParameter ) {
			return determineValueMapping( (SqmParameter) sqmExpression );
		}

		final MappingMetamodel domainModel = getCreationContext().getDomainModel();
		if ( sqmExpression instanceof SqmPath ) {
			log.debugf( "Determining mapping-model type for SqmPath : %s ", sqmExpression );
			return SqmMappingModelHelper.resolveMappingModelExpressable(
					sqmExpression,
					domainModel,
					getFromClauseAccess()::findTableGroup
			);
		}

		// The model type of an enum literal is always inferred
		if ( sqmExpression instanceof SqmEnumLiteral<?> ) {
			final Supplier<MappingModelExpressable> currentExpressableSupplier = inferrableTypeAccessStack.getCurrent();
			if ( currentExpressableSupplier != null ) {
				return currentExpressableSupplier.get();
			}
		}

		if ( sqmExpression instanceof SqmSubQuery<?> ) {
			final SqmSubQuery<?> subQuery = (SqmSubQuery<?>) sqmExpression;
			final SqmSelectClause selectClause = subQuery.getQuerySpec().getSelectClause();
			if ( selectClause.getSelections().size() == 1 ) {
				final SqmSelection<?> subQuerySelection = selectClause.getSelections().get( 0 );
				final SqmExpressable<?> selectionNodeType = subQuerySelection.getNodeType();
				if ( selectionNodeType != null ) {
					final SqmExpressable<?> sqmExpressable;
					if ( selectionNodeType instanceof PluralPersistentAttribute ) {
						sqmExpressable = ( (PluralPersistentAttribute<?,?,?>) selectionNodeType ).getElementPathSource();
					}
					else if ( selectionNodeType instanceof SqmPathSource<?>) {
						final SqmPathSource<?> pathSource = (SqmPathSource<?>) selectionNodeType;
						final CollectionPart.Nature partNature = CollectionPart.Nature.fromName(
								pathSource.getPathName()
						);
						if ( partNature == null ) {
							sqmExpressable = selectionNodeType;
						}
						else {
							final SqmPath<?> sqmPath = (SqmPath<?>) subQuerySelection.getSelectableNode();
							final NavigablePath navigablePath = sqmPath.getNavigablePath().getParent();
							if ( navigablePath.getParent() != null ) {
								final TableGroup parentTableGroup = findTableGroup( navigablePath.getParent() );
								final PluralAttributeMapping pluralPart = (PluralAttributeMapping) parentTableGroup.getModelPart()
										.findSubPart( navigablePath.getUnaliasedLocalName(), null );
								return pluralPart.findSubPart( pathSource.getPathName(), null );
							}
							return findTableGroup( navigablePath ).getModelPart();
						}
					}
					else {
						sqmExpressable = selectionNodeType;
					}

					final MappingModelExpressable<?> expressable = domainModel.resolveMappingExpressable( sqmExpressable, this::findTableGroupByPath );

					if ( expressable != null ) {
						return expressable;
					}

					try {
						return inferrableTypeAccessStack.getCurrent().get();
					}
					catch (Exception ignore) {
						return null;
					}
				}
			}
		}

		log.debugf( "Determining mapping-model type for generalized SqmExpression : %s", sqmExpression );
		final SqmExpressable<?> nodeType = sqmExpression.getNodeType();
		final MappingModelExpressable valueMapping = domainModel.resolveMappingExpressable(
				nodeType,
				this::findTableGroupByPath
		);

		if ( valueMapping == null ) {
			final Supplier<MappingModelExpressable> currentExpressableSupplier = inferrableTypeAccessStack.getCurrent();
			if ( currentExpressableSupplier != null ) {
				return currentExpressableSupplier.get();
			}
		}

		if ( valueMapping == null ) {
			throw new ConversionException( "Could not determine ValueMapping for SqmExpression: " + sqmExpression );
		}

		return valueMapping;
	}

	protected MappingModelExpressable<?> getInferredValueMapping() {
		final Supplier<MappingModelExpressable> currentExpressableSupplier = inferrableTypeAccessStack.getCurrent();
		if ( currentExpressableSupplier != null ) {
			final MappingModelExpressable inferredMapping = currentExpressableSupplier.get();
			if ( inferredMapping != null ) {
				if ( inferredMapping instanceof PluralAttributeMapping ) {
					return ( (PluralAttributeMapping) inferredMapping ).getElementDescriptor();
				}
				else if ( !( inferredMapping instanceof JavaObjectType ) ) {
					// Never report back the "object type" as inferred type and instead rely on the value type
					return inferredMapping;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("WeakerAccess")
	protected MappingModelExpressable<?> determineValueMapping(SqmParameter<?> sqmParameter) {
		log.debugf( "Determining mapping-model type for SqmParameter : %s", sqmParameter );

		final QueryParameterImplementor<?> queryParameter = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding<?> binding = domainParameterBindings.getBinding( queryParameter );

		if ( sqmParameter.getAnticipatedType() == null ) {
			// this should indicate the condition that the user query did not define an
			// explicit type in regard to this parameter.  Here we should prefer the
			// inferrable type and fallback to the binding type
			final MappingModelExpressable<?> inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return inferredValueMapping;
			}
		}

		AllowableParameterType<?> parameterSqmType = binding.getBindType();
		if ( parameterSqmType == null ) {
			parameterSqmType = queryParameter.getHibernateType();
			if ( parameterSqmType == null ) {
				parameterSqmType = sqmParameter.getAnticipatedType();
			}
		}

		if ( parameterSqmType == null ) {
			// Default to the Object type
			return basicType( Object.class );
		}
		else if ( parameterSqmType instanceof MappingModelExpressable<?> && parameterSqmType.getJavaType() == Object.class ) {
			return (MappingModelExpressable<?>) parameterSqmType;
		}

		if ( parameterSqmType instanceof SqmPath ) {
			final SqmPath sqmPath = (SqmPath) parameterSqmType;
			final NavigablePath navigablePath = sqmPath.getNavigablePath();
			if ( navigablePath.getParent() != null ) {
				final TableGroup tableGroup = getFromClauseAccess().getTableGroup( navigablePath.getParent() );
				return tableGroup.getModelPart().findSubPart(
						navigablePath.getLocalName(),
						null
				);
			}

			return getFromClauseAccess().getTableGroup( navigablePath ).getModelPart();
		}

		if ( parameterSqmType instanceof BasicValuedMapping ) {
			return (BasicValuedMapping) parameterSqmType;
		}

		if ( parameterSqmType instanceof CompositeSqmPathSource ) {
			// Try to infer the value mapping since the other side apparently is a path source
			final MappingModelExpressable<?> inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return inferredValueMapping;
			}
			throw new NotYetImplementedFor6Exception( "Support for embedded-valued parameters not yet implemented" );
		}

		if ( parameterSqmType instanceof SqmPathSource<?> || parameterSqmType instanceof BasicDomainType<?> ) {
			// Try to infer the value mapping since the other side apparently is a path source
			final MappingModelExpressable<?> inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return inferredValueMapping;
			}
			return getTypeConfiguration().getBasicTypeForJavaType(
					( (SqmExpressable<?>) parameterSqmType ).getExpressableJavaTypeDescriptor()
							.getJavaTypeClass()
			);
		}

		throw new ConversionException( "Could not determine ValueMapping for SqmParameter: " + sqmParameter );
	}

	protected final Stack<Supplier<MappingModelExpressable>> inferrableTypeAccessStack = new StandardStack<>(
			() -> null
	);

	private void resolveSqmParameter(
			SqmParameter expression,
			MappingModelExpressable valueMapping,
			BiConsumer<Integer,JdbcParameter> jdbcParameterConsumer) {
		sqmParameterMappingModelTypes.put( expression, valueMapping );
		final Bindable bindable;
		if(valueMapping instanceof EntityCollectionPart){
			bindable = ((EntityCollectionPart)valueMapping).getElementForeignKeyDescriptor();
		}
		else if ( valueMapping instanceof Association ) {
			bindable = ( (Association) valueMapping ).getForeignKeyDescriptor();
		}
		else if ( valueMapping instanceof EntityMappingType ) {
			bindable = ( (EntityMappingType) valueMapping ).getIdentifierMapping();
		}
		else {
			bindable = valueMapping;
		}
		bindable.forEachJdbcType(
				(index, jdbcMapping) -> jdbcParameterConsumer.accept( index, new JdbcParameterImpl( jdbcMapping ) )
		);
	}

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter<?> expression) {
		return consumeSqmParameter( expression );
	}

	@Override
	public Object visitJpaCriteriaParameter(JpaCriteriaParameter<?> expression) {
		return consumeSqmParameter( getSqmParameter( expression ) );
	}

	private SqmParameter<?> getSqmParameter(SqmExpression<?> parameter) {
		if ( parameter instanceof JpaCriteriaParameter<?> ) {
			return getSqmParameter( (JpaCriteriaParameter<?>) parameter );
		}
		else if ( parameter instanceof SqmParameter<?> ) {
			return (SqmParameter<?>) parameter;
		}
		return null;
	}

	private SqmParameter<?> getSqmParameter(JpaCriteriaParameter<?> expression) {
		if ( jpaCriteriaParamResolutions == null ) {
			throw new IllegalStateException( "No JpaCriteriaParameter resolutions registered" );
		}

		final Supplier<SqmJpaCriteriaParameterWrapper<?>> supplier = jpaCriteriaParamResolutions.get( expression );
		if ( supplier == null ) {
			throw new IllegalStateException( "Criteria parameter [" + expression + "] not known to be a parameter of the processing tree" );
		}
		return supplier.get();
	}

	@Override
	public Object visitTuple(SqmTuple<?> sqmTuple) {
		final List<SqmExpression<?>> groupedExpressions = sqmTuple.getGroupedExpressions();
		final int size = groupedExpressions.size();
		final List<Expression> expressions = new ArrayList<>( size );
		final MappingModelExpressable<?> mappingModelExpressable = inferrableTypeAccessStack.getCurrent().get();
		final EmbeddableMappingType embeddableMappingType;
		if ( mappingModelExpressable instanceof ValueMapping ) {
			embeddableMappingType = (EmbeddableMappingType) ( (ValueMapping) mappingModelExpressable ).getMappedType();
		}
		else {
			embeddableMappingType = null;
		}
		if ( embeddableMappingType == null ) {
			try {
				inferrableTypeAccessStack.push( () -> null );
				for ( int i = 0; i < size; i++ ) {
					expressions.add( (Expression) groupedExpressions.get( i ).accept( this ) );
				}
			}
			finally {
				inferrableTypeAccessStack.pop();
			}
		}
		else {
			for ( int i = 0; i < size; i++ ) {
				final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMappings().get( i );
				inferrableTypeAccessStack.push( () -> attributeMapping );
				try {
					expressions.add( (Expression) groupedExpressions.get( i ).accept( this ) );
				}
				finally {
					inferrableTypeAccessStack.pop();
				}
			}
		}
		final MappingModelExpressable<?> valueMapping;
		if ( mappingModelExpressable != null ) {
			valueMapping = mappingModelExpressable;
		}
		else {
			final SqmExpressable<?> expressable = sqmTuple.getExpressable();
			if ( expressable instanceof MappingModelExpressable<?> ) {
				valueMapping = (MappingModelExpressable<?>) expressable;
			}
			else {
				valueMapping = null;
			}
		}
		return new SqlTuple( expressions, valueMapping );
	}

	@Override
	public Object visitCollate(SqmCollate<?> sqmCollate) {
		return new Collate(
				(Expression) sqmCollate.getExpression().accept( this ),
				sqmCollate.getCollation()
		);
	}

	@Override
	public Expression visitFunction(SqmFunction<?> sqmFunction) {
		inferrableTypeAccessStack.push( () -> null );
		try {
			return sqmFunction.convertToSqlAst( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}
	}

	@Override
	public Star visitStar(SqmStar sqmStar) {
		return new Star();
	}

	@Override
	public Object visitDistinct(SqmDistinct<?> sqmDistinct) {
		return new Distinct( (Expression) sqmDistinct.getExpression().accept( this ) );
	}

	@Override
	public Object visitTrimSpecification(SqmTrimSpecification specification) {
		return new TrimSpecification( specification.getSpecification() );
	}

	@Override
	public Object visitCastTarget(SqmCastTarget<?> target) {
		BasicValuedMapping targetType = (BasicValuedMapping) target.getType();
		if ( targetType instanceof BasicType<?> ) {
			targetType = InferredBasicValueResolver.resolveSqlTypeIndicators(
					this,
					(BasicType<?>) targetType,
					target.getNodeJavaTypeDescriptor()
			);
		}
		return new CastTarget(
				targetType.getJdbcMapping(),
				target.getLength(),
				target.getPrecision(),
				target.getScale()
		);
	}

	@Override
	public Object visitExtractUnit(SqmExtractUnit<?> unit) {
		return new ExtractUnit(
				unit.getUnit(),
				(BasicValuedMapping) unit.getType()
		);
	}

	@Override
	public Object visitDurationUnit(SqmDurationUnit<?> unit) {
		return new DurationUnit(
				unit.getUnit(),
				(BasicValuedMapping) unit.getType()
		);
	}

	@Override
	public Object visitFormat(SqmFormat sqmFormat) {
		return new Format(
				sqmFormat.getLiteralValue(),
				(BasicValuedMapping) sqmFormat.getNodeType()
		);
	}

	@Override
	public Object visitUnaryOperationExpression(SqmUnaryOperation<?> expression) {
		return new UnaryOperation(
				interpret( expression.getOperation() ),
				toSqlExpression( expression.getOperand().accept( this ) ),
				(BasicValuedMapping) determineValueMapping( expression.getOperand() )
		);
	}

	private UnaryArithmeticOperator interpret(UnaryArithmeticOperator operator) {
		return operator;
	}

	@Override
	public Object visitBinaryArithmeticExpression(SqmBinaryArithmetic<?> expression) {
		SqmExpression leftOperand = expression.getLeftHandOperand();
		SqmExpression rightOperand = expression.getRightHandOperand();

		boolean durationToRight = TypeConfiguration.isDuration( rightOperand.getNodeType() );
		TypeConfiguration typeConfiguration = getCreationContext().getDomainModel().getTypeConfiguration();
		TemporalType temporalTypeToLeft = typeConfiguration.getSqlTemporalType( leftOperand.getNodeType() );
		TemporalType temporalTypeToRight = typeConfiguration.getSqlTemporalType( rightOperand.getNodeType() );
		boolean temporalTypeSomewhereToLeft = adjustedTimestamp != null || temporalTypeToLeft != null;

		if ( temporalTypeToLeft != null && durationToRight ) {
			if ( adjustmentScale != null || negativeAdjustment ) {
				//we can't distribute a scale over a date/timestamp
				throw new SemanticException( "scalar multiplication of temporal value" );
			}
		}

		if ( durationToRight && temporalTypeSomewhereToLeft ) {
			return transformDurationArithmetic( expression );
		}
		else if ( temporalTypeToLeft != null && temporalTypeToRight != null ) {
			return transformDatetimeArithmetic( expression );
		}
		else if ( durationToRight && appliedByUnit != null ) {
			return new BinaryArithmeticExpression(
					toSqlExpression( leftOperand.accept( this ) ),
					expression.getOperator(),
					toSqlExpression( rightOperand.accept( this ) ),
					//after distributing the 'by unit' operator
					//we always get a Long value back
					(BasicValuedMapping) appliedByUnit.getNodeType()
			);
		}
		else {
			return new BinaryArithmeticExpression(
					toSqlExpression( leftOperand.accept( this ) ),
					expression.getOperator(),
					toSqlExpression( rightOperand.accept( this ) ),
					getExpressionType( expression )
			);
		}
	}

	private BasicValuedMapping getExpressionType(SqmBinaryArithmetic<?> expression) {
		final SqmExpressable<?> leftHandOperandType = expression.getLeftHandOperand().getNodeType();
		if ( leftHandOperandType instanceof BasicValuedMapping ) {
			return (BasicValuedMapping) leftHandOperandType;
		}
		else {
			final SqmExpressable<?> rightHandOperandType = expression.getRightHandOperand().getNodeType();
			if ( rightHandOperandType instanceof BasicValuedMapping ) {
				return (BasicValuedMapping) rightHandOperandType;
			}

			return getTypeConfiguration().getBasicTypeForJavaType(
					leftHandOperandType.getExpressableJavaTypeDescriptor().getJavaTypeClass()
			);
		}
	}

	private Expression toSqlExpression(Object value) {
		return (Expression) value;
	}

	private Object transformDurationArithmetic(SqmBinaryArithmetic<?> expression) {
		BinaryArithmeticOperator operator = expression.getOperator();

		// we have a date or timestamp somewhere to
		// the right of us, so we need to restructure
		// the expression tree
		switch ( operator ) {
			case ADD:
			case SUBTRACT:
				// the only legal binary operations involving
				// a duration with a date or timestamp are
				// addition and subtraction with the duration
				// on the right and the date or timestamp on
				// the left, producing a date or timestamp
				//
				// ts + d or ts - d
				//
				// the only legal binary operations involving
				// two durations are addition and subtraction,
				// producing a duration
				//
				// d1 + d2

				// re-express addition of non-leaf duration
				// expressions to a date or timestamp as
				// addition of leaf durations to a date or
				// timestamp
				// ts + x * (d1 + d2) => (ts + x * d1) + x * d2
				// ts - x * (d1 + d2) => (ts - x * d1) - x * d2
				// ts + x * (d1 - d2) => (ts + x * d1) - x * d2
				// ts - x * (d1 - d2) => (ts - x * d1) + x * d2

				Expression timestamp = adjustedTimestamp;
				SqmExpressable<?> timestampType = adjustedTimestampType;
				adjustedTimestamp = toSqlExpression( expression.getLeftHandOperand().accept( this ) );
				JdbcMappingContainer type = adjustedTimestamp.getExpressionType();
				if ( type instanceof SqmExpressable ) {
					adjustedTimestampType = (SqmExpressable) type;
				}
				else if (type instanceof AttributeMapping ) {
					adjustedTimestampType = (SqmExpressable) ( (AttributeMapping) type ).getMappedType();
				}
				else {
					// else we know it has not been transformed
					adjustedTimestampType = expression.getLeftHandOperand().getNodeType();
				}
				if ( operator == SUBTRACT ) {
					negativeAdjustment = !negativeAdjustment;
				}
				try {
					return expression.getRightHandOperand().accept( this );
				}
				finally {
					if ( operator == SUBTRACT ) {
						negativeAdjustment = !negativeAdjustment;
					}
					adjustedTimestamp = timestamp;
					adjustedTimestampType = timestampType;
				}
			case MULTIPLY:
				// finally, we can multiply a duration on the
				// right by a scalar value on the left
				// scalar multiplication produces a duration
				// x * d

				// distribute scalar multiplication over the
				// terms, not forgetting the propagated scale
				// x * (d1 + d2) => x * d1 + x * d2
				// x * (d1 - d2) => x * d1 - x * d2
				// -x * (d1 + d2) => - x * d1 - x * d2
				// -x * (d1 - d2) => - x * d1 + x * d2
				Expression duration = toSqlExpression( expression.getLeftHandOperand().accept( this ) );
				Expression scale = adjustmentScale;
				boolean negate = negativeAdjustment;
				adjustmentScale = applyScale( duration );
				negativeAdjustment = false; //was sucked into the scale
				try {
					return expression.getRightHandOperand().accept( this );
				}
				finally {
					adjustmentScale = scale;
					negativeAdjustment = negate;
				}
			default:
				throw new SemanticException( "illegal operator for a duration " + operator );
		}
	}

	private Object transformDatetimeArithmetic(SqmBinaryArithmetic expression) {
		BinaryArithmeticOperator operator = expression.getOperator();

		// the only kind of algebra we know how to
		// do on dates/timestamps is subtract them,
		// producing a duration - all other binary
		// operator expressions with two dates or
		// timestamps are ill-formed
		if ( operator != SUBTRACT ) {
			throw new SemanticException( "illegal operator for temporal type: " + operator );
		}

		// a difference between two dates or two
		// timestamps is a leaf duration, so we
		// must apply the scale, and the 'by unit'
		// ts1 - ts2

		Expression left = cleanly( () -> toSqlExpression( expression.getLeftHandOperand().accept( this ) ) );
		Expression right = cleanly( () -> toSqlExpression( expression.getRightHandOperand().accept( this ) ) );

		TypeConfiguration typeConfiguration = getCreationContext().getDomainModel().getTypeConfiguration();
		TemporalType leftTimestamp = typeConfiguration.getSqlTemporalType( expression.getLeftHandOperand().getNodeType() );
		TemporalType rightTimestamp = typeConfiguration.getSqlTemporalType( expression.getRightHandOperand().getNodeType() );

		// when we're dealing with Dates, we use
		// DAY as the smallest unit, otherwise we
		// use a platform-specific granularity

		TemporalUnit baseUnit = ( rightTimestamp == TemporalType.TIMESTAMP || leftTimestamp == TemporalType.TIMESTAMP ) ?
				NATIVE :
				DAY;

		if ( adjustedTimestamp != null ) {
			if ( appliedByUnit != null ) {
				throw new IllegalStateException();
			}
			// we're using the resulting duration to
			// adjust a date or timestamp on the left

			// baseUnit is the finest resolution for the
			// temporal type, so we must use it for both
			// the diff, and then the subsequent add

			DurationUnit unit = new DurationUnit( baseUnit, basicType( Integer.class ) );
			Expression magnitude = applyScale( timestampdiff().expression( null, unit, right, left ) );
			return timestampadd().expression(
					(AllowableFunctionReturnType<?>) adjustedTimestampType, //TODO should be adjustedTimestamp.getType()
					unit, magnitude, adjustedTimestamp
			);
		}
		else if ( appliedByUnit != null ) {
			// we're immediately converting the resulting
			// duration to a scalar in the given unit

			DurationUnit unit = (DurationUnit) appliedByUnit.getUnit().accept( this );
			return applyScale( timestampdiff().expression( null, unit, right, left ) );
		}
		else {
			// a plain "bare" Duration
			DurationUnit unit = new DurationUnit( baseUnit, basicType( Integer.class ) );
			BasicValuedMapping durationType = (BasicValuedMapping) expression.getNodeType();
			Expression scaledMagnitude = applyScale( timestampdiff().expression(
					(AllowableFunctionReturnType<?>) expression.getNodeType(),
					unit, right, left
			) );
			return new Duration( scaledMagnitude, baseUnit, durationType );
		}
	}

	private <J> BasicType<J> basicType(Class<J> javaType) {
		return creationContext.getDomainModel().getTypeConfiguration().getBasicTypeForJavaType( javaType );
	}

	private TimestampaddFunction timestampadd() {
		return (TimestampaddFunction)
				getCreationContext().getSessionFactory()
						.getQueryEngine().getSqmFunctionRegistry()
						.findFunctionDescriptor( "timestampadd" );
	}

	private TimestampdiffFunction timestampdiff() {
		return (TimestampdiffFunction)
				getCreationContext().getSessionFactory()
						.getQueryEngine().getSqmFunctionRegistry()
						.findFunctionDescriptor( "timestampdiff" );
	}

	private <T> T cleanly(Supplier<T> supplier) {
		SqmByUnit byUnit = appliedByUnit;
		Expression timestamp = adjustedTimestamp;
		SqmExpressable<?> timestampType = adjustedTimestampType;
		Expression scale = adjustmentScale;
		boolean negate = negativeAdjustment;
		adjustmentScale = null;
		negativeAdjustment = false;
		appliedByUnit = null;
		adjustedTimestamp = null;
		adjustedTimestampType = null;
		try {
			return supplier.get();
		}
		finally {
			appliedByUnit = byUnit;
			adjustedTimestamp = timestamp;
			adjustedTimestampType = timestampType;
			adjustmentScale = scale;
			negativeAdjustment = negate;
		}
	}

	Expression applyScale(Expression magnitude) {
		boolean negate = negativeAdjustment;

		if ( magnitude instanceof UnaryOperation ) {
			UnaryOperation unary = (UnaryOperation) magnitude;
			if ( unary.getOperator() == UNARY_MINUS ) {
				// if it's already negated, don't
				// wrap it in another unary minus,
				// just throw away the one we have
				// (OTOH, if it *is* negated, shift
				// the operator to left of scale)
				negate = !negate;
			}
			magnitude = unary.getOperand();
		}

		if ( adjustmentScale != null ) {
			if ( isOne( adjustmentScale ) ) {
				//no work to do
			}
			else {
				if ( isOne( magnitude ) ) {
					magnitude = adjustmentScale;
				}
				else {
					final BasicValuedMapping magnitudeType = (BasicValuedMapping) magnitude.getExpressionType();
					final BasicValuedMapping expressionType;
					if ( magnitudeType.getJdbcMapping().getJdbcTypeDescriptor().isInterval() ) {
						expressionType = magnitudeType;
					}
					else {
						expressionType = widestNumeric(
								(BasicValuedMapping) adjustmentScale.getExpressionType(),
								magnitudeType
						);
					}
					magnitude = new BinaryArithmeticExpression(
							adjustmentScale,
							MULTIPLY,
							magnitude,
							expressionType
					);
				}
			}
		}

		if ( negate ) {
			magnitude = new UnaryOperation(
					UNARY_MINUS,
					magnitude,
					(BasicValuedMapping) magnitude.getExpressionType()
			);
		}

		return magnitude;
	}

	@SuppressWarnings("unchecked")
	static boolean isOne(Expression scale) {
		return scale instanceof QueryLiteral
				&& ( (QueryLiteral<Number>) scale ).getLiteralValue().longValue() == 1L;
	}

	@Override
	public Object visitToDuration(SqmToDuration<?> toDuration) {
		//TODO: do we need to temporarily set appliedByUnit
		//      to null before we recurse down the tree?
		//      and what about scale?
		Expression magnitude = toSqlExpression( toDuration.getMagnitude().accept( this ) );
		DurationUnit unit = (DurationUnit) toDuration.getUnit().accept( this );

		// let's start by applying the propagated scale
		// so we don't forget to do it in what follows
		Expression scaledMagnitude = applyScale( magnitude );

		if ( adjustedTimestamp != null ) {
			// we're adding this literal duration to the
			// given date or timestamp, producing an
			// adjusted date or timestamp
			if ( appliedByUnit != null ) {
				throw new IllegalStateException();
			}
			return timestampadd().expression(
					(AllowableFunctionReturnType<?>) adjustedTimestampType, //TODO should be adjustedTimestamp.getType()
					unit, scaledMagnitude, adjustedTimestamp
			);
		}
		else {
			BasicValuedMapping durationType = (BasicValuedMapping) toDuration.getNodeType();
			Duration duration;
			if ( scaledMagnitude.getExpressionType()
					.getJdbcMappings()
					.get( 0 )
					.getJdbcTypeDescriptor()
					.isInterval() ) {
				duration = new Duration( extractEpoch( scaledMagnitude ), SECOND, durationType );
			}
			else {
				duration = new Duration( scaledMagnitude, unit.getUnit(), durationType );
			}

			if ( appliedByUnit != null ) {
				// we're applying the 'by unit' operator,
				// producing a literal scalar value in
				// the given unit
				TemporalUnit appliedUnit = appliedByUnit.getUnit().getUnit();
				BasicValuedMapping scalarType = (BasicValuedMapping) appliedByUnit.getNodeType();
				return new Conversion( duration, appliedUnit, scalarType );
			}
			else {
				// a "bare" Duration value (gets rendered as nanoseconds)
				return duration;
			}
		}
	}

	@Override
	public Object visitByUnit(SqmByUnit byUnit) {
		SqmByUnit outer = appliedByUnit;
		appliedByUnit = byUnit;
		try {
			return byUnit.getDuration().accept( this );
		}
		finally {
			appliedByUnit = outer;
		}
	}

	private BasicValuedMapping widestNumeric(BasicValuedMapping lhs, BasicValuedMapping rhs) {
		final CastType lhsCastType = lhs.getJdbcMapping().getJdbcTypeDescriptor().getCastType();
		final CastType rhsCastType = rhs.getJdbcMapping().getJdbcTypeDescriptor().getCastType();
		if ( lhsCastType == CastType.FIXED ) {
			return lhs;
		}
		if ( rhsCastType == CastType.FIXED ) {
			return rhs;
		}
		if ( lhsCastType == CastType.DOUBLE ) {
			return lhs;
		}
		if ( rhsCastType == CastType.DOUBLE ) {
			return rhs;
		}
		if ( lhsCastType == CastType.FLOAT ) {
			return lhs;
		}
		if ( rhsCastType == CastType.FLOAT ) {
			return rhs;
		}
		return lhs;
	}

	@Override
	public QueryPart visitSubQueryExpression(SqmSubQuery<?> sqmSubQuery) {
		// The only purpose for tracking the current join is to
		// Reset the current join for sub queries because in there, we won't add nested joins
		final SqmJoin<?, ?> oldJoin = currentlyProcessingJoin;
		currentlyProcessingJoin = null;
		final QueryPart queryPart = visitQueryPart( sqmSubQuery.getQueryPart() );
		currentlyProcessingJoin = oldJoin;
		return queryPart;
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(SqmCaseSimple<?, ?> expression) {
		final List<CaseSimpleExpression.WhenFragment> whenFragments = new ArrayList<>( expression.getWhenFragments().size() );
		final Supplier<MappingModelExpressable> inferenceSupplier = inferrableTypeAccessStack.getCurrent();
		inferrableTypeAccessStack.push(
				() -> {
					for ( SqmCaseSimple.WhenFragment<?, ?> whenFragment : expression.getWhenFragments() ) {
						final MappingModelExpressable<?> resolved = determineCurrentExpressable( whenFragment.getCheckValue() );
						if ( resolved != null ) {
							return resolved;
						}
					}
					return null;
				}
		);
		final Expression fixture = (Expression) expression.getFixture().accept( this );
		final MappingModelExpressable<?> fixtureType = (MappingModelExpressable<?>) fixture.getExpressionType();
		inferrableTypeAccessStack.pop();
		MappingModelExpressable<?> resolved = determineCurrentExpressable( expression );
		Expression otherwise = null;
		for ( SqmCaseSimple.WhenFragment<?, ?> whenFragment : expression.getWhenFragments() ) {
			inferrableTypeAccessStack.push( () -> fixtureType );
			final Expression checkValue = (Expression) whenFragment.getCheckValue().accept( this );
			inferrableTypeAccessStack.pop();
			final MappingModelExpressable<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			final Expression resultExpression = (Expression) whenFragment.getResult().accept( this );
			inferrableTypeAccessStack.pop();
			resolved = (MappingModelExpressable<?>) TypeHelper.highestPrecedence( resolved, resultExpression.getExpressionType() );

			whenFragments.add(
					new CaseSimpleExpression.WhenFragment(
							checkValue,
							resultExpression
					)
			);
		}

		if ( expression.getOtherwise() != null ) {
			final MappingModelExpressable<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			otherwise = (Expression) expression.getOtherwise().accept( this );
			inferrableTypeAccessStack.pop();
			resolved = (MappingModelExpressable<?>) TypeHelper.highestPrecedence( resolved, otherwise.getExpressionType() );
		}

		return new CaseSimpleExpression(
				resolved,
				fixture,
				whenFragments,
				otherwise
		);
	}

	@Override
	public CaseSearchedExpression visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		final List<CaseSearchedExpression.WhenFragment> whenFragments = new ArrayList<>( expression.getWhenFragments().size() );
		final Supplier<MappingModelExpressable> inferenceSupplier = inferrableTypeAccessStack.getCurrent();
		MappingModelExpressable<?> resolved = determineCurrentExpressable( expression );

		Expression otherwise = null;
		for ( SqmCaseSearched.WhenFragment<?> whenFragment : expression.getWhenFragments() ) {
			inferrableTypeAccessStack.push( () -> null );
			final Predicate whenPredicate = (Predicate) whenFragment.getPredicate().accept( this );
			inferrableTypeAccessStack.pop();
			final MappingModelExpressable<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			final Expression resultExpression = (Expression) whenFragment.getResult().accept( this );
			inferrableTypeAccessStack.pop();
			resolved = (MappingModelExpressable<?>) TypeHelper.highestPrecedence( resolved, resultExpression.getExpressionType() );

			whenFragments.add( new CaseSearchedExpression.WhenFragment( whenPredicate, resultExpression ) );
		}

		if ( expression.getOtherwise() != null ) {
			final MappingModelExpressable<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			otherwise = (Expression) expression.getOtherwise().accept( this );
			inferrableTypeAccessStack.pop();
			resolved = (MappingModelExpressable<?>) TypeHelper.highestPrecedence( resolved, otherwise.getExpressionType() );
		}

		return new CaseSearchedExpression( resolved, whenFragments, otherwise );
	}

	private MappingModelExpressable<?> determineCurrentExpressable(SqmTypedNode<?> expression) {
		try {
			return creationContext
					.getDomainModel()
					.resolveMappingExpressable( expression.getNodeType(), getFromClauseIndex()::findTableGroup );
		}
		catch (UnsupportedOperationException e) {
			return null;
		}
	}

	private <X> X visitWithInferredType(SqmExpression<?> expression, SqmExpression<?> inferred) {
		inferrableTypeAccessStack.push( () -> determineValueMapping( inferred ) );
		try {
			return (X) expression.accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}
	}

	private <X> X visitWithLenientInferredType(SqmExpression<?> expression, SqmExpression<?> inferred) {
		inferrableTypeAccessStack.push(
				() -> {
					try {
						final MappingModelExpressable<?> definedType = creationContext
								.getDomainModel()
								.resolveMappingExpressable( expression.getNodeType(), getFromClauseIndex()::findTableGroup );
						if ( definedType != null ) {
							return definedType;
						}
					}
					catch (UnsupportedOperationException ignore) {
						// todo (6.0) : log?
					}

					try {
						final MappingModelExpressable<?> definedType = creationContext
								.getDomainModel()
								.lenientlyResolveMappingExpressable( inferred.getNodeType(), getFromClauseIndex()::findTableGroup );
						if ( definedType != null ) {
							return definedType;
						}
					}
					catch (UnsupportedOperationException ignore) {
						// todo (6.0) : log?
					}

					return null;
				}
		);

		try {
			return (X) expression.accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}
	}

	@Override
	public Object visitAny(SqmAny<?> sqmAny) {
		return new Any(
				visitSubQueryExpression( sqmAny.getSubquery() ),
				null //resolveMappingExpressable( sqmAny.getNodeType() )
		);
	}

	@Override
	public Object visitEvery(SqmEvery<?> sqmEvery) {
		return new Every(
				visitSubQueryExpression( sqmEvery.getSubquery() ),
				null //resolveMappingExpressable( sqmEvery.getNodeType() )
		);
	}

	@Override
	public Object visitSummarization(SqmSummarization<?> sqmSummarization) {
		final List<SqmExpression<?>> groupingExpressions = sqmSummarization.getGroupings();
		final int size = groupingExpressions.size();
		final List<Expression> expressions = new ArrayList<>( size );
		for ( int i = 0; i < size; i++ ) {
			expressions.add( (Expression) groupingExpressions.get( i ).accept( this ) );
		}
		return new Summarization(
				getSummarizationKind( sqmSummarization.getKind() ),
				expressions
		);
	}

	private Summarization.Kind getSummarizationKind(SqmSummarization.Kind kind) {
		switch ( kind ) {
			case CUBE:
				return Summarization.Kind.CUBE;
			case ROLLUP:
				return Summarization.Kind.ROLLUP;
		}
		throw new UnsupportedOperationException( "Unsupported summarization: " + kind );
	}

	@Override
	public Expression visitEntityTypeLiteralExpression(SqmLiteralEntityType<?> sqmExpression) {
		final EntityDomainType<?> nodeType = sqmExpression.getNodeType();
		final EntityPersister mappingDescriptor = getCreationContext().getDomainModel()
				.getEntityDescriptor( nodeType.getHibernateEntityName() );

		return new EntityTypeLiteral( mappingDescriptor );
	}

	@Override
	public Expression visitParameterizedEntityTypeExpression(SqmParameterizedEntityType<?> sqmExpression) {
		assert inferrableTypeAccessStack.getCurrent().get() instanceof EntityDiscriminatorMapping;
		return (Expression) sqmExpression.getDiscriminatorSource().accept( this );
	}

	@Override
	public Object visitEnumLiteral(SqmEnumLiteral<?> sqmEnumLiteral) {
		final BasicValuedMapping inferrableType = (BasicValuedMapping) inferrableTypeAccessStack.getCurrent().get();
		if ( inferrableType instanceof ConvertibleModelPart ) {
			final ConvertibleModelPart inferredPart = (ConvertibleModelPart) inferrableType;
			final BasicValueConverter valueConverter = inferredPart.getValueConverter();
			final Object jdbcValue = valueConverter.toRelationalValue( sqmEnumLiteral.getEnumValue() );
			return new QueryLiteral<>( jdbcValue, inferredPart );
		}

		return new QueryLiteral<>(
				sqmEnumLiteral.getEnumValue(),
				(BasicValuedMapping) determineValueMapping( sqmEnumLiteral )
		);
	}

	@Override
	public Object visitFieldLiteral(SqmFieldLiteral<?> sqmFieldLiteral) {
		return new QueryLiteral<>(
				sqmFieldLiteral.getValue(),
				(BasicValuedMapping) determineValueMapping( sqmFieldLiteral )
		);
	}

//	@Override
//	public Object visitPluralAttributeElementBinding(PluralAttributeElementBinding binding) {
//		final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( binding.getFromElement() );
//
//		return getCurrentDomainReferenceExpressionBuilder().buildPluralAttributeElementReferenceExpression(
//				binding,
//				resolvedTableGroup,
//				PersisterHelper.convert( binding.getNavigablePath() )
//		);
//	}
//
//	@Override
//	public ColumnReference visitExplicitColumnReference(SqmColumnReference sqmColumnReference) {
//		final TableGroup tableGroup = fromClauseIndex.findTableGroup(
//				sqmColumnReference.getSqmFromBase().getNavigablePath()
//		);
//
//		final ColumnReference columnReference = tableGroup.locateColumnReferenceByName( sqmColumnReference.getColumnName() );
//
//		if ( columnReference == null ) {
//			throw new HibernateException( "Could not resolve ColumnReference" );
//		}
//
//		return columnReference;
//	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates


	@Override
	public GroupedPredicate visitGroupedPredicate(SqmGroupedPredicate predicate) {
		return new GroupedPredicate( (Predicate) predicate.getSubPredicate().accept( this ) );
	}

	@Override
	public Junction visitAndPredicate(SqmAndPredicate predicate) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION, getBooleanType() );
		conjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		conjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return conjunction;
	}

	@Override
	public Junction visitOrPredicate(SqmOrPredicate predicate) {
		final Junction disjunction = new Junction( Junction.Nature.DISJUNCTION, getBooleanType() );
		disjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		disjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return disjunction;
	}

	@Override
	public Predicate visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
		final SqmPath<?> pluralPath = predicate.getPluralPath();
		prepareReusablePath( pluralPath, () -> null );

		final PluralAttributeMapping mappingModelExpressable = (PluralAttributeMapping) determineValueMapping(
				pluralPath );

		if ( mappingModelExpressable.getElementDescriptor() instanceof EntityCollectionPart ) {
			inferrableTypeAccessStack.push(
					() -> ( (EntityCollectionPart) mappingModelExpressable.getElementDescriptor() ).getKeyTargetMatchPart() );
		}
		else if ( mappingModelExpressable.getElementDescriptor() instanceof EmbeddedCollectionPart ) {
			inferrableTypeAccessStack.push(
					() -> mappingModelExpressable.getElementDescriptor() );
		}
		else {
			inferrableTypeAccessStack.push( () -> mappingModelExpressable );
		}

		final Expression lhs;
		try {
			lhs = (Expression) predicate.getLeftHandExpression().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		final FromClauseAccess parentFromClauseAccess = getFromClauseAccess();
		final QuerySpec subQuerySpec = new QuerySpec( false );
		pushProcessingState(
				new SqlAstQueryPartProcessingStateImpl(
						subQuerySpec,
						getCurrentProcessingState(),
						this,
						currentClauseStack::getCurrent
				)
		);
		try {
			final TableGroup tableGroup = mappingModelExpressable.createRootTableGroup(
					true,
					pluralPath.getNavigablePath(),
					null,
					() -> subQuerySpec::applyPredicate,
					this,
					creationContext
			);

			getFromClauseAccess().registerTableGroup( pluralPath.getNavigablePath(), tableGroup );
			subQuerySpec.getFromClause().addRoot( tableGroup );

			final CollectionPart elementDescriptor = mappingModelExpressable.getElementDescriptor();
			if ( elementDescriptor instanceof EntityCollectionPart ) {
				( (EntityCollectionPart) elementDescriptor ).getKeyTargetMatchPart()
						.createDomainResult(
								pluralPath.getNavigablePath(),
								tableGroup,
								null,
								this
						);
			}
			else {
				elementDescriptor.createDomainResult(
						pluralPath.getNavigablePath(),
						tableGroup,
						null,
						this
				);
			}

			subQuerySpec.applyPredicate(
					mappingModelExpressable.getKeyDescriptor().generateJoinPredicate(
							parentFromClauseAccess.findTableGroup( pluralPath.getNavigablePath().getParent() ),
							tableGroup,
							SqlAstJoinType.INNER,
							getSqlExpressionResolver(),
							creationContext
					)
			);
		}
		finally {
			popProcessingStateStack();
		}

		return new InSubQueryPredicate(
				lhs,
				subQuerySpec,
				predicate.isNegated(),
				getBooleanType()
		);
	}

	@Override
	public NegatedPredicate visitNegatedPredicate(SqmNegatedPredicate predicate) {
		return new NegatedPredicate(
				(Predicate) predicate.getWrappedPredicate().accept( this )
		);
	}

	@Override
	public ComparisonPredicate visitComparisonPredicate(SqmComparisonPredicate predicate) {
		inferrableTypeAccessStack.push( () -> determineValueMapping( predicate.getRightHandExpression() ) );

		final Expression lhs;
		try {
			lhs = (Expression) predicate.getLeftHandExpression().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		inferrableTypeAccessStack.push( () -> determineValueMapping( predicate.getLeftHandExpression() ) );

		final Expression rhs;
		try {
			rhs = (Expression) predicate.getRightHandExpression().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		return new ComparisonPredicate( lhs, predicate.getSqmOperator(), rhs, getBooleanType() );
	}

	@Override
	public Object visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
		prepareReusablePath( predicate.getPluralPath(), () -> null );

		final QuerySpec subQuerySpec = new QuerySpec( false, 1 );

		final FromClauseAccess parentFromClauseAccess = getFromClauseAccess();
		final SqlAstProcessingStateImpl subQueryState = new SqlAstProcessingStateImpl(
				getCurrentProcessingState(),
				this,
				currentClauseStack::getCurrent
		);

		pushProcessingState( subQueryState );
		try {
			final SqmPluralValuedSimplePath<?> sqmPluralPath = predicate.getPluralPath();

			final NavigablePath pluralPathNavPath = sqmPluralPath.getNavigablePath();
			final NavigablePath parentNavPath = pluralPathNavPath.getParent();
			assert parentNavPath != null;

			final TableGroup parentTableGroup = parentFromClauseAccess.getTableGroup( parentNavPath );
			final SqlAliasBase sqlAliasBase = sqlAliasBaseManager.createSqlAliasBase( parentTableGroup.getGroupAlias() );
			final TableGroup tableGroup = new CorrelatedTableGroup(
					parentTableGroup,
					sqlAliasBase,
					subQuerySpec,
					subQuerySpec::applyPredicate,
					creationContext.getSessionFactory()
			);
			subQueryState.getSqlAstCreationState().getFromClauseAccess().registerTableGroup(
					parentNavPath,
					tableGroup
			);

			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) visitPluralValuedPath(
					sqmPluralPath
			).getExpressionType();
			// The creation of the table group join against the correlated table group
			// has the side effect that the from and where clause of the sub-query are set
			tableGroup.addTableGroupJoin(
					pluralAttributeMapping.createTableGroupJoin(
							pluralPathNavPath,
							tableGroup,
							sqmPluralPath.getExplicitAlias(),
							SqlAstJoinType.INNER,
							false,
							sqlAliasBaseManager,
							subQueryState,
							creationContext
					)
			);

			final ForeignKeyDescriptor collectionKeyDescriptor = pluralAttributeMapping.getKeyDescriptor();
			final int jdbcTypeCount = collectionKeyDescriptor.getJdbcTypeCount();
			assert jdbcTypeCount > 0;

			final JdbcLiteral<Integer> jdbcLiteral = new JdbcLiteral<>( 1, basicType( Integer.class ) );
			subQuerySpec.getSelectClause().addSqlSelection(
					new SqlSelectionImpl( 1, 0, jdbcLiteral )
			);

			final ExistsPredicate existsPredicate = new ExistsPredicate( subQuerySpec, getBooleanType() );
			if ( predicate.isNegated() ) {
				return existsPredicate;
			}
			return new NegatedPredicate( existsPredicate );
		}
		finally {
			popProcessingStateStack();
		}
	}

	@Override
	public BetweenPredicate visitBetweenPredicate(SqmBetweenPredicate predicate) {
		final Expression expression;
		final Expression lowerBound;
		final Expression upperBound;

		inferrableTypeAccessStack.push(
				() -> coalesceSuppliedValues(
						() -> determineValueMapping( predicate.getLowerBound() ),
						() -> determineValueMapping( predicate.getUpperBound() )
				)
		);

		try {
			expression = (Expression) predicate.getExpression().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		inferrableTypeAccessStack.push(
				() -> coalesceSuppliedValues(
						() -> determineValueMapping( predicate.getExpression() ),
						() -> determineValueMapping( predicate.getUpperBound() )
				)
		);
		try {
			lowerBound = (Expression) predicate.getLowerBound().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		inferrableTypeAccessStack.push(
				() -> coalesceSuppliedValues(
						() -> determineValueMapping( predicate.getExpression() ),
						() -> determineValueMapping( predicate.getLowerBound() )
				)
		);
		try {
			upperBound = (Expression) predicate.getUpperBound().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		return new BetweenPredicate(
				expression,
				lowerBound,
				upperBound,
				predicate.isNegated(),
				getBooleanType()
		);
	}

	@Override
	public LikePredicate visitLikePredicate(SqmLikePredicate predicate) {
		final Expression escapeExpression = predicate.getEscapeCharacter() == null
				? null
				: (Expression) predicate.getEscapeCharacter().accept( this );

		return new LikePredicate(
				(Expression) predicate.getMatchExpression().accept( this ),
				(Expression) predicate.getPattern().accept( this ),
				escapeExpression,
				predicate.isNegated(),
				predicate.isCaseSensitive(),
				getBooleanType()
		);
	}

	@Override
	public NullnessPredicate visitIsNullPredicate(SqmNullnessPredicate predicate) {
		return new NullnessPredicate(
				(Expression) predicate.getExpression().accept( this ),
				predicate.isNegated(),
				getBooleanType()
		);
	}

	@Override
	public InListPredicate visitInListPredicate(SqmInListPredicate<?> predicate) {
		// special case: if there is just a single "value" element and it is a parameter
		//		and the binding for that parameter is multi-valued we need special
		//		handling for "expansion"
		if ( predicate.getListExpressions().size() == 1 ) {
			final SqmExpression sqmExpression = predicate.getListExpressions().get( 0 );
			if ( sqmExpression instanceof SqmParameter ) {
				final SqmParameter sqmParameter = (SqmParameter) sqmExpression;

				if ( sqmParameter.allowMultiValuedBinding() ) {
					final InListPredicate specialCase = processInListWithSingleParameter( predicate, sqmParameter );
					if ( specialCase != null ) {
						return specialCase;
					}
				}
			}
		}

		// otherwise - no special case...

		final InListPredicate inPredicate = new InListPredicate(
				(Expression) predicate.getTestExpression().accept( this ),
				predicate.isNegated(),
				getBooleanType()
		);

		inferrableTypeAccessStack.push( () -> determineValueMapping( predicate.getTestExpression() ) );

		try {
			for ( SqmExpression expression : predicate.getListExpressions() ) {
				inPredicate.addExpression( (Expression) expression.accept( this ) );
			}
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		return inPredicate;
	}

	private InListPredicate processInListWithSingleParameter(
			SqmInListPredicate<?> sqmPredicate,
			SqmParameter sqmParameter) {
		assert sqmParameter.allowMultiValuedBinding();

		if ( sqmParameter instanceof JpaCriteriaParameter ) {
			return processInSingleCriteriaParameter( sqmPredicate, (JpaCriteriaParameter) sqmParameter );
		}

		return processInSingleHqlParameter( sqmPredicate, sqmParameter );
	}

	private InListPredicate processInSingleHqlParameter(SqmInListPredicate<?> sqmPredicate, SqmParameter sqmParameter) {
		final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding domainParamBinding = domainParameterBindings.getBinding( domainParam );

		if ( !domainParamBinding.isMultiValued() ) {
			// triggers normal processing
			return null;
		}

		final InListPredicate inListPredicate = new InListPredicate(
				(Expression) sqmPredicate.getTestExpression().accept( this ),
				sqmPredicate.isNegated(),
				getBooleanType()
		);

		inferrableTypeAccessStack.push(
				() -> determineValueMapping( sqmPredicate.getTestExpression() )
		);

		try {
			boolean first = true;
			for ( Object bindValue : domainParamBinding.getBindValues() ) {
				final SqmParameter sqmParamToConsume;
				// for each bind value create an "expansion"
				if ( first ) {
					sqmParamToConsume = sqmParameter;
					first = false;
				}
				else {
					sqmParamToConsume = sqmParameter.copy();
					domainParameterXref.addExpansion( domainParam, sqmParameter, sqmParamToConsume );
				}

				inListPredicate.addExpression( consumeSingleSqmParameter( sqmParamToConsume ) );
			}
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		return inListPredicate;
	}

	private InListPredicate processInSingleCriteriaParameter(
			SqmInListPredicate<?> sqmPredicate,
			JpaCriteriaParameter jpaCriteriaParameter) {
		assert jpaCriteriaParameter.allowsMultiValuedBinding();

		final QueryParameterBinding domainParamBinding = domainParameterBindings.getBinding( jpaCriteriaParameter );
		if ( !domainParamBinding.isMultiValued() ) {
			return null;
		}

		final InListPredicate inListPredicate = new InListPredicate(
				(Expression) sqmPredicate.getTestExpression().accept( this ),
				sqmPredicate.isNegated(),
				getBooleanType()
		);

		inferrableTypeAccessStack.push(
				() -> determineValueMapping( sqmPredicate.getTestExpression() )
		);

		final SqmJpaCriteriaParameterWrapper<?> sqmWrapper = jpaCriteriaParamResolutions.get( jpaCriteriaParameter )
				.get();

		try {
			boolean first = true;
			for ( Object bindValue : domainParamBinding.getBindValues() ) {
				final SqmParameter sqmParamToConsume;
				// for each bind value create an "expansion"
				if ( first ) {
					sqmParamToConsume = sqmWrapper;
					first = false;
				}
				else {
					sqmParamToConsume = sqmWrapper.copy();
					domainParameterXref.addExpansion( jpaCriteriaParameter, sqmWrapper, sqmParamToConsume );
				}

				inListPredicate.addExpression( consumeSingleSqmParameter( sqmParamToConsume ) );
			}
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		return inListPredicate;
	}

	@Override
	public InSubQueryPredicate visitInSubQueryPredicate(SqmInSubQueryPredicate predicate) {
		return new InSubQueryPredicate(
				visitWithInferredType( predicate.getTestExpression(), predicate.getSubQueryExpression() ),
				visitWithInferredType( predicate.getSubQueryExpression(), predicate.getTestExpression() ),
				predicate.isNegated(),
				getBooleanType()
		);
	}

	private JdbcMappingContainer getBooleanType() {
		return getTypeConfiguration().getBasicTypeForJavaType( Boolean.class );
	}

	@Override
	public Object visitBooleanExpressionPredicate(SqmBooleanExpressionPredicate predicate) {
		final Expression booleanExpression = (Expression) predicate.getBooleanExpression().accept( this );
		if ( booleanExpression instanceof SelfRenderingExpression ) {
			final Predicate sqlPredicate = new SelfRenderingPredicate( (SelfRenderingExpression) booleanExpression );
			if ( predicate.isNegated() ) {
				return new NegatedPredicate( sqlPredicate );
			}
			return sqlPredicate;
		}
		else {
			return new BooleanExpressionPredicate(
					booleanExpression,
					predicate.isNegated(),
					getBooleanType()
			);
		}
	}

	@Override
	public Object visitExistsPredicate(SqmExistsPredicate predicate) {
		return new ExistsPredicate( (QueryPart) predicate.getExpression().accept( this ), getBooleanType() );
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}

	@Override
	public Object visitFullyQualifiedClass(Class<?> namedClass) {
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
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		final List<Fetch> fetches = CollectionHelper.arrayList( fetchParent.getReferencedMappingType().getNumberOfFetchables() );
		final List<String> bagRoles = new ArrayList<>();

		final BiConsumer<Fetchable, Boolean> fetchableBiConsumer = (fetchable, isKeyFetchable) -> {
			final NavigablePath resolvedNavigablePath = fetchParent.resolveNavigablePath( fetchable );

			final String alias;
			FetchTiming fetchTiming = fetchable.getMappedFetchOptions().getTiming();
			boolean joined = false;

			EntityGraphTraversalState.TraversalResult traversalResult = null;
			final FromClauseIndex fromClauseIndex = getFromClauseIndex();
			final SqmAttributeJoin<?, ?> fetchedJoin = fromClauseIndex.findFetchedJoinByPath( resolvedNavigablePath );
			boolean explicitFetch = false;

			final NavigablePath fetchablePath;
			if ( fetchedJoin != null ) {
				fetchablePath = fetchedJoin.getNavigablePath();
				// there was an explicit fetch in the SQM
				//		there should be a TableGroupJoin registered for this `fetchablePath` already
				assert fromClauseIndex.getTableGroup( fetchedJoin.getNavigablePath() ) != null;

				if ( fetchedJoin.isFetched() ) {
					fetchTiming = FetchTiming.IMMEDIATE;
				}
				joined = true;
				alias = fetchedJoin.getExplicitAlias();
				explicitFetch = true;
			}
			else {
				fetchablePath = resolvedNavigablePath;
				// there was not an explicit fetch in the SQM
				alias = null;

				if ( !( fetchable instanceof CollectionPart ) ) {
					if ( entityGraphTraversalState != null ) {
						traversalResult = entityGraphTraversalState.traverse( fetchParent, fetchable, isKeyFetchable );
						fetchTiming = traversalResult.getFetchTiming();
						joined = traversalResult.isJoined();
						explicitFetch = true;
					}
					else if ( getLoadQueryInfluencers().hasEnabledFetchProfiles() ) {
						// There is no point in checking the fetch profile if it can't affect this fetchable
						if ( fetchTiming != FetchTiming.IMMEDIATE || fetchable.incrementFetchDepth() ) {
							final String fetchableRole = fetchable.getNavigableRole().getFullPath();

							for ( String enabledFetchProfileName : getLoadQueryInfluencers().getEnabledFetchProfileNames() ) {
								final FetchProfile enabledFetchProfile = getCreationContext().getSessionFactory()
										.getFetchProfile( enabledFetchProfileName );
								final org.hibernate.engine.profile.Fetch profileFetch = enabledFetchProfile.getFetchByRole(
										fetchableRole );

								if ( profileFetch != null ) {
									fetchTiming = FetchTiming.IMMEDIATE;
									joined = joined || profileFetch.getStyle() == org.hibernate.engine.profile.Fetch.Style.JOIN;
									explicitFetch = true;
								}
							}
						}
					}
				}

				final TableGroup existingJoinedGroup = fromClauseIndex.findTableGroup( fetchablePath );
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
					if ( fetchDepth >= maxDepth ) {
						joined = false;
					}
				}

				if ( joined && fetchable instanceof TableGroupJoinProducer ) {
					TableGroupJoinProducer tableGroupJoinProducer = (TableGroupJoinProducer) fetchable;
					fromClauseIndex.resolveTableGroup(
							fetchablePath,
							np -> {
								// generate the join
								final TableGroup lhs = fromClauseIndex.getTableGroup( fetchParent.getNavigablePath() );
								final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) fetchable ).createTableGroupJoin(
										fetchablePath,
										lhs,
										alias,
										tableGroupJoinProducer.getDefaultSqlAstJoinType( lhs ),
										true,
										this
								);
								lhs.addTableGroupJoin( tableGroupJoin );
								return tableGroupJoin.getJoinedGroup();
							}
					);
				}
			}

			final boolean incrementFetchDepth = fetchable.incrementFetchDepth();
			try {
				if ( incrementFetchDepth ) {
					fetchDepth++;
				}
				// There is no need to check for circular fetches if this is an explicit fetch
				if ( !explicitFetch && !isResolvingCircularFetch() ) {
					final Fetch biDirectionalFetch = fetchable.resolveCircularFetch(
							fetchablePath,
							fetchParent,
							fetchTiming,
							this
					);

					if ( biDirectionalFetch != null ) {
						fetches.add( biDirectionalFetch );
						return;
					}
				}
				final Fetch fetch = buildFetch( fetchablePath, fetchParent, fetchable, fetchTiming, joined, alias );

				if ( fetch != null ) {
					if ( fetch.getTiming() == FetchTiming.IMMEDIATE && fetchable instanceof PluralAttributeMapping ) {
						final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;
						final CollectionClassification collectionClassification = pluralAttributeMapping.getMappedType()
								.getCollectionSemantics()
								.getCollectionClassification();
						if ( collectionClassification == CollectionClassification.BAG ) {
							bagRoles.add( fetchable.getNavigableRole().getNavigableName() );
						}
					}

					fetches.add( fetch );
				}
			}
			finally {
				if ( incrementFetchDepth ) {
					fetchDepth--;
				}
				if ( entityGraphTraversalState != null && traversalResult != null ) {
					entityGraphTraversalState.backtrack( traversalResult.getPreviousContext() );
				}
			}
		};

// todo (6.0) : determine how to best handle TREAT
//		fetchParent.getReferencedMappingContainer().visitKeyFetchables( fetchableBiConsumer, treatTargetType );
//		fetchParent.getReferencedMappingContainer().visitFetchables( fetchableBiConsumer, treatTargetType );
		fetchParent.getReferencedMappingContainer().visitKeyFetchables( fetchable -> fetchableBiConsumer.accept( fetchable, true ), null );
		fetchParent.getReferencedMappingContainer().visitFetchables( fetchable -> fetchableBiConsumer.accept( fetchable, false ), null );
		if ( bagRoles.size() > 1 ) {
			throw new MultipleBagFetchException( bagRoles );
		}
		return fetches;
	}

	private Fetch buildFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			Fetchable fetchable,
			FetchTiming fetchTiming,
			boolean joined,
			String alias) {
		// fetch has access to its parent in addition to the parent having its fetches.
		//
		// we could sever the parent -> fetch link ... it would not be "seen" while walking
		// but it would still have access to its parent info - and be able to access its
		// "initializing" state as part of AfterLoadAction

		try {
			final Fetch fetch = fetchParent.generateFetchableFetch(
					fetchable,
					fetchablePath,
					fetchTiming,
					joined,
					alias,
					this
			);

			if ( fetchable instanceof PluralAttributeMapping && fetch.getTiming() == FetchTiming.IMMEDIATE && joined ) {
				final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;

				final Joinable joinable = pluralAttributeMapping
						.getCollectionDescriptor()
						.getCollectionType()
						.getAssociatedJoinable( getCreationContext().getSessionFactory() );
				final TableGroup tableGroup = getFromClauseIndex().getTableGroup( fetchablePath );
				final FilterPredicate collectionFieldFilterPredicate = FilterHelper.createFilterPredicate(
						getLoadQueryInfluencers(),
						joinable,
						tableGroup
				);
				if ( collectionFieldFilterPredicate != null ) {
					if ( collectionFilterPredicates == null ) {
						collectionFilterPredicates = new HashMap<>();
					}
					collectionFilterPredicates.put( tableGroup.getGroupAlias(), collectionFieldFilterPredicate );
				}
				if ( pluralAttributeMapping.getCollectionDescriptor().isManyToMany() ) {
					assert joinable instanceof CollectionPersister;
					final Predicate manyToManyFilterPredicate = FilterHelper.createManyToManyFilterPredicate(
							getLoadQueryInfluencers(),
							( CollectionPersister) joinable,
							tableGroup
					);
					if ( manyToManyFilterPredicate != null ) {
						assert tableGroup.getTableReferenceJoins() != null &&
								tableGroup.getTableReferenceJoins().size() == 1;
						tableGroup.getTableReferenceJoins().get( 0 ).applyPredicate( manyToManyFilterPredicate );
					}
				}

				if ( orderByFragmentConsumer != null ) {
					assert tableGroup.getModelPart() == pluralAttributeMapping;

					if ( pluralAttributeMapping.getOrderByFragment() != null ) {
						orderByFragmentConsumer.accept( pluralAttributeMapping.getOrderByFragment(), tableGroup );
					}

					if ( pluralAttributeMapping.getManyToManyOrderByFragment() != null ) {
						orderByFragmentConsumer.accept( pluralAttributeMapping.getManyToManyOrderByFragment(), tableGroup );
					}
				}
			}

			return fetch;
		}
		catch (RuntimeException e) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Could not generate fetch : %s -> %s",
							fetchParent.getNavigablePath(),
							fetchable.getFetchableName()
					),
					e
			);
		}
	}

	@Override
	public boolean isResolvingCircularFetch() {
		return resolvingCircularFetch;
	}

	@Override
	public void setResolvingCircularFetch(boolean resolvingCircularFetch) {
		this.resolvingCircularFetch = resolvingCircularFetch;
	}

	@Override
	public ForeignKeyDescriptor.Nature getCurrentlyResolvingForeignKeyPart() {
		return currentlyResolvingForeignKeySide;
	}

	@Override
	public void setCurrentlyResolvingForeignKeyPart(ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide) {
		this.currentlyResolvingForeignKeySide = currentlyResolvingForeignKeySide;
	}

	@Internal
	public interface SqmAliasedNodeCollector {
		void next();
		List<SqlSelection> getSelections(int position);
	}

	protected static class DelegatingSqmAliasedNodeCollector implements SqlExpressionResolver, SqmAliasedNodeCollector {

		private final SqlExpressionResolver delegate;
		private SqmAliasedNodeCollector sqmAliasedNodeCollector;

		public DelegatingSqmAliasedNodeCollector(SqlExpressionResolver delegate) {
			this.delegate = delegate;
		}

		@Override
		public void next() {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<SqlSelection> getSelections(int position) {
			return sqmAliasedNodeCollector.getSelections( position );
		}

		@Override
		public Expression resolveSqlExpression(String key, Function<SqlAstProcessingState, Expression> creator) {
			return delegate.resolveSqlExpression( key, creator );
		}

		@Override
		public SqlSelection resolveSqlSelection(
				Expression expression,
				JavaType javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			return delegate.resolveSqlSelection( expression, javaTypeDescriptor, typeConfiguration );
		}

		public SqmAliasedNodeCollector getSqmAliasedNodeCollector() {
			return sqmAliasedNodeCollector;
		}

		public void setSqmAliasedNodeCollector(SqmAliasedNodeCollector sqmAliasedNodeCollector) {
			this.sqmAliasedNodeCollector = sqmAliasedNodeCollector;
		}
	}

	protected static class SqmAliasedNodePositionTracker implements SqlExpressionResolver, SqmAliasedNodeCollector {
		private final SqlExpressionResolver delegate;
		private final List<SqlSelection>[] sqlSelectionsForSqmSelection;
		private int index = -1;

		public SqmAliasedNodePositionTracker(SqlExpressionResolver delegate, List<? extends SqmAliasedNode<?>> selections) {
			this.delegate = delegate;
			this.sqlSelectionsForSqmSelection = new List[countIndividualSelections( selections )];
		}

		private static int countIndividualSelections(List<? extends SqmAliasedNode<?>> selections) {
			int offset = 0;
			for ( int i = 0; i < selections.size(); i++ ) {
				final SqmSelectableNode<?> selectableNode = selections.get( i ).getSelectableNode();
				if ( selectableNode instanceof SqmDynamicInstantiation<?> ) {
					offset = countIndividualSelections( ( (SqmDynamicInstantiation<?>) selectableNode ).getArguments() ) - 1;
				}
				else if ( selectableNode instanceof SqmJpaCompoundSelection<?> ) {
					offset += ( (SqmJpaCompoundSelection<?>) selectableNode ).getSelectionItems().size() - 1;
				}
			}
			return offset + selections.size();
		}

		@Override
		public void next() {
			index++;
		}

		@Override
		public List<SqlSelection> getSelections(int position) {
			return sqlSelectionsForSqmSelection[ position ];
		}

		@Override
		public Expression resolveSqlExpression(String key, Function<SqlAstProcessingState, Expression> creator) {
			return delegate.resolveSqlExpression( key, creator );
		}

		@Override
		public SqlSelection resolveSqlSelection(
				Expression expression,
				JavaType javaTypeDescriptor,
				TypeConfiguration typeConfiguration) {
			SqlSelection selection = delegate.resolveSqlSelection( expression, javaTypeDescriptor, typeConfiguration );
			List<SqlSelection> sqlSelectionList = sqlSelectionsForSqmSelection[index];
			if ( sqlSelectionList == null ) {
				sqlSelectionsForSqmSelection[index] = sqlSelectionList = new ArrayList<>();
			}
			sqlSelectionList.add( selection );
			return selection;
		}
	}

	@Override
	public List<Expression> expandSelfRenderingFunctionMultiValueParameter(SqmParameter sqmParameter) {
		assert sqmParameter.allowMultiValuedBinding();
		final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter(
				sqmParameter );
		final QueryParameterBinding domainParamBinding = domainParameterBindings.getBinding(
				domainParam );

		final Collection bindValues = domainParamBinding.getBindValues();
		final int bindValuesSize = bindValues.size();
		final List<Expression> result = new ArrayList<>( bindValuesSize );

		boolean first = true;
		for ( int i = 0; i < bindValuesSize; i++ ) {
			final SqmParameter sqmParamToConsume;
			// for each bind value create an "expansion"
			if ( first ) {
				sqmParamToConsume = sqmParameter;
				first = false;
			}
			else {
				sqmParamToConsume = sqmParameter.copy();
				domainParameterXref.addExpansion( domainParam, sqmParameter, sqmParamToConsume );
			}
			final Expression expression = consumeSingleSqmParameter( sqmParamToConsume );
			result.add( expression );
		}
		return result;
	}
}
