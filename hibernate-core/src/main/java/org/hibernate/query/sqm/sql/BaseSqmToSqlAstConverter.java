/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql;

import jakarta.persistence.criteria.Predicate.BooleanOperator;
import jakarta.persistence.metamodel.SingularAttribute;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.TimestampaddFunction;
import org.hibernate.dialect.function.TimestampdiffFunction;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.id.BulkInsertionCapableIdentifierGenerator;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
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
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.ValueMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.BasicValuedCollectionPart;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ManyToManyCollectionPart;
import org.hibernate.metamodel.mapping.internal.OneToManyCollectionPart;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.CompositeSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;
import org.hibernate.persister.entity.DiscriminatorHelper;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaSearchOrder;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.DiscriminatorSqmPath;
import org.hibernate.query.sqm.InterpretationException;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmPathVisitor;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.sql.internal.AnyDiscriminatorPathInterpretation;
import org.hibernate.query.sqm.sql.internal.AsWrappedExpression;
import org.hibernate.query.sqm.sql.internal.BasicValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.CollectionFetchPaginationQueryTransformer;
import org.hibernate.query.sqm.sql.internal.DiscriminatedAssociationPathInterpretation;
import org.hibernate.query.sqm.sql.internal.DiscriminatorPathInterpretation;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.sql.internal.EmbeddableValuedExpression;
import org.hibernate.query.sqm.sql.internal.EmbeddableValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.EntityValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.NonAggregatedCompositeValuedPathInterpretation;
import org.hibernate.query.sqm.sql.internal.PluralValuedSimplePathInterpretation;
import org.hibernate.query.sqm.sql.internal.SqlAstProcessingStateImpl;
import org.hibernate.query.sqm.sql.internal.SqlAstQueryNodeProcessingStateImpl;
import org.hibernate.query.sqm.sql.internal.SqlAstQueryPartProcessingStateImpl;
import org.hibernate.query.sqm.sql.internal.SqmMapEntryResult;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.query.sqm.sql.internal.SqmPathInterpretation;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.cte.SqmCteContainer;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.cte.SqmCteTable;
import org.hibernate.query.sqm.tree.cte.SqmCteTableColumn;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.AbstractSqmPluralJoin;
import org.hibernate.query.sqm.tree.domain.AbstractSqmSpecificPluralPartPath;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmCorrelatedRootJoin;
import org.hibernate.query.sqm.tree.domain.SqmCorrelation;
import org.hibernate.query.sqm.tree.domain.SqmCteRoot;
import org.hibernate.query.sqm.tree.domain.SqmDerivedRoot;
import org.hibernate.query.sqm.tree.domain.SqmElementAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmFkExpression;
import org.hibernate.query.sqm.tree.domain.SqmFunctionPath;
import org.hibernate.query.sqm.tree.domain.SqmFunctionRoot;
import org.hibernate.query.sqm.tree.domain.SqmIndexAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralPartJoin;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedFrom;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.AsWrapperSqmExpression;
import org.hibernate.query.sqm.tree.expression.Conversion;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmAny;
import org.hibernate.query.sqm.tree.expression.SqmAnyDiscriminatorValue;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmByUnit;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.SqmCoalesce;
import org.hibernate.query.sqm.tree.expression.SqmCollation;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmDurationUnit;
import org.hibernate.query.sqm.tree.expression.SqmEnumLiteral;
import org.hibernate.query.sqm.tree.expression.SqmEvery;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpressionHelper;
import org.hibernate.query.sqm.tree.expression.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.query.sqm.tree.expression.SqmFormat;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmHqlNumericLiteral;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEmbeddableType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.SqmLiteralNull;
import org.hibernate.query.sqm.tree.expression.SqmModifiedSubQueryExpression;
import org.hibernate.query.sqm.tree.expression.SqmNamedExpression;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmOver;
import org.hibernate.query.sqm.tree.expression.SqmOverflow;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameterizedEntityType;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.expression.SqmStar;
import org.hibernate.query.sqm.tree.expression.SqmSummarization;
import org.hibernate.query.sqm.tree.expression.SqmToDuration;
import org.hibernate.query.sqm.tree.expression.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFunctionJoin;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.from.SqmTreatedAttributeJoin;
import org.hibernate.query.sqm.tree.insert.SqmConflictClause;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.query.sqm.tree.insert.SqmInsertValuesStatement;
import org.hibernate.query.sqm.tree.insert.SqmValues;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBooleanExpressionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmEmptinessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmExistsPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmJunctionPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmMemberOfPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmTruthnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmAliasedNode;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
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
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleEntityValuedModelPart;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQueryNodeProcessingState;
import org.hibernate.sql.ast.spi.SqlAstQueryPartProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteObject;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.cte.CteTableGroup;
import org.hibernate.sql.ast.tree.cte.SearchClauseSpecification;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.AliasedExpression;
import org.hibernate.sql.ast.tree.expression.Any;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Collation;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Duration;
import org.hibernate.sql.ast.tree.expression.DurationUnit;
import org.hibernate.sql.ast.tree.expression.EmbeddableTypeLiteral;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.Every;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.sql.ast.tree.expression.JdbcLiteral;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.ModifiedSubQueryExpression;
import org.hibernate.sql.ast.tree.expression.Over;
import org.hibernate.sql.ast.tree.expression.Overflow;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.QueryTransformer;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.expression.UnparsedNumericLiteral;
import org.hibernate.sql.ast.tree.from.CorrelatedPluralTableGroup;
import org.hibernate.sql.ast.tree.from.CorrelatedTableGroup;
import org.hibernate.sql.ast.tree.from.EmbeddableFunctionTableGroup;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.FunctionTableGroup;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.QueryPartTableGroup;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.insert.ConflictClause;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.BooleanExpressionPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.ExistsPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.predicate.ThruthnessPredicate;
import org.hibernate.sql.ast.tree.select.QueryGroup;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignable;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.internal.AbstractJdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.internal.LimitJdbcParameter;
import org.hibernate.sql.exec.internal.OffsetJdbcParameter;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.internal.VersionTypeSeedParameterSpecification;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParameters;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.EntityGraphTraversalState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiation;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.StandardEntityGraphTraversalStateImpl;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.BindableType;
import org.hibernate.type.BottomType;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JavaTypeHelper;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserVersionType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

import static jakarta.persistence.metamodel.Type.PersistenceType.ENTITY;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hibernate.boot.model.internal.AuditHelper.isFetchableAuditExcluded;
import static org.hibernate.boot.model.process.internal.InferredBasicValueResolver.resolveSqlTypeIndicators;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.metamodel.mapping.EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME;
import static org.hibernate.query.QueryLogging.QUERY_MESSAGE_LOGGER;
import static org.hibernate.query.common.TemporalUnit.EPOCH;
import static org.hibernate.query.common.TemporalUnit.NANOSECOND;
import static org.hibernate.query.common.TemporalUnit.NATIVE;
import static org.hibernate.query.common.TemporalUnit.SECOND;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.ADD;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.MULTIPLY;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.SUBTRACT;
import static org.hibernate.query.sqm.UnaryArithmeticOperator.UNARY_MINUS;
import static org.hibernate.query.sqm.internal.SqmMappingModelHelper.resolveExplicitTreatTarget;
import static org.hibernate.query.sqm.internal.SqmMappingModelHelper.resolveMappingModelExpressible;
import static org.hibernate.query.sqm.internal.SqmUtil.isFkOptimizationAllowed;
import static org.hibernate.query.sqm.mutation.internal.SqmInsertStrategyHelper.createRowNumberingExpression;
import static org.hibernate.query.sqm.sql.AggregateColumnAssignmentHandler.forEntityDescriptor;
import static org.hibernate.sql.ast.internal.TableGroupJoinHelper.determineJoinForPredicateApply;
import static org.hibernate.sql.ast.spi.SqlAstTreeHelper.combinePredicates;
import static org.hibernate.type.spi.TypeConfiguration.getSqlTemporalType;
import static org.hibernate.type.spi.TypeConfiguration.isDuration;
import static org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmToSqlAstConverter<T extends Statement> extends BaseSemanticQueryWalker
		implements SqmTranslator<T>, DomainResultCreationState, JdbcTypeIndicators {

//	private static final Logger LOG = Logger.getLogger( BaseSqmToSqlAstConverter.class );

	private final SqlAstCreationContext creationContext;
	private final boolean jpaQueryComplianceEnabled;
	private final SqmStatement<?> statement;

	private final QueryOptions queryOptions;
	private final LoadQueryInfluencers loadQueryInfluencers;

	private final Map<SqmParameter<?>, List<List<JdbcParameter>>> jdbcParamsBySqmParam = new IdentityHashMap<>();
	private final JdbcParameters jdbcParameters = new JdbcParametersImpl();
	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindings domainParameterBindings;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> sqmParameterMappingModelTypes = new LinkedHashMap<>();
	private final Map<JpaCriteriaParameter<?>, SqmJpaCriteriaParameterWrapper<?>> jpaCriteriaParamResolutions;
	private final List<DomainResult<?>> domainResults;
	private final EntityGraphTraversalState entityGraphTraversalState;

	private int fetchDepth;
	private String currentBagRole;
	private boolean resolvingCircularFetch;
	private boolean deduplicateSelectionItems;
	private ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide;
	private SqmStatement<?> currentSqmStatement;
	private final Stack<SqmQueryPart<?>> sqmQueryPartStack = new StandardStack<>();
	private CteContainer cteContainer;
	/**
	 * A map from {@link SqmCteTable#getCteName()} to the final SQL name.
	 * We use this global map as most databases don't support shadowing of names.
	 */
	private Map<String, String> cteNameMapping;
	private boolean containsCollectionFetches;
	private boolean trackSelectionsForGroup;

	// Captures the list of SqlSelection for a navigable path.
	// The map will only contain entries for order by elements of a QueryGroup, that refer to an attribute name
	// i.e. `(select e from Entity e union all ...) order by name` where `name` is an attribute of the type `Entity`
	private Map<NavigablePath, Map.Entry<Integer, List<SqlSelection>>> trackedFetchSelectionsForGroup = emptyMap();

	private List<Map.Entry<OrderByFragment, TableGroup>> orderByFragments;

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();
	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>();
	private final Stack<FromClauseIndex> fromClauseIndexStack = new StandardStack<>();

	// Captures all entity name uses under which a table group is being used within the current conjunct.
	// Outside a top level conjunct, it represents the "global uses" i.e. select, from, group and order by clauses.
	// Top level conjunct contexts like visitWhereClause, visitHavingClause, visitOrPredicate and visitNestedTopLevelPredicate
	// stash away the parent entity name uses, consumes the entity name uses of the conjunct by rendering a type restriction,
	// and then restore the parent entity name uses again.
	private final Map<TableGroup, Map<String, EntityNameUse>> tableGroupEntityNameUses = new IdentityHashMap<>();

	private SqlAstProcessingState lastPoppedProcessingState;
	private FromClauseIndex lastPoppedFromClauseIndex;
	private SqmJoin<?, ?> currentlyProcessingJoin;
	protected Predicate additionalRestrictions;

	private final Stack<Clause> currentClauseStack = new StandardStack<>();
	private final Stack<Supplier<MappingModelExpressible<?>>> inferrableTypeAccessStack = new StandardStack<>();
	private final Stack<List<QueryTransformer>> queryTransformers = new StandardStack<>();
	private boolean inTypeInference;
	private boolean inImpliedResultTypeInference;
	private boolean inNestedContext;
	private Supplier<MappingModelExpressible<?>> functionImpliedResultTypeAccess;

	private SqmByUnit appliedByUnit;
	private Expression adjustedTimestamp;
	private SqmExpressible<?> adjustedTimestampType; //TODO: remove this once we can get a Type directly from adjustedTimestamp
	private Expression adjustmentScale;
	private boolean negativeAdjustment;

	private final Set<AssociationKey> visitedAssociationKeys = new HashSet<>();
	private final HashMap<MetadataKey<?, ?>, Object> metadata = new HashMap<>();
	private final MappingMetamodel domainModel;

	public BaseSqmToSqlAstConverter(
			SqlAstCreationContext creationContext,
			SqmStatement<?> statement,
			QueryOptions queryOptions,
			LoadQueryInfluencers loadQueryInfluencers,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			boolean deduplicateSelectionItems) {
		this.inferrableTypeAccessStack.push( () -> null );
		this.creationContext = creationContext;
		this.jpaQueryComplianceEnabled = creationContext.isJpaQueryComplianceEnabled();

		this.statement = statement;
		this.currentSqmStatement = statement;
		this.deduplicateSelectionItems = deduplicateSelectionItems;

		if ( statement instanceof SqmSelectStatement<?> selectStatement ) {
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
					selectStatement.getQueryPart().getFirstQuerySpec()
							.getSelectClause()
							.getSelections()
							.size()
			);

			// We can't deduplicate select items with query groups,
			// otherwise a query might fail with inconsistent select items
			//
			// select e1.id, e1.id from Entity1 e1
			// union all
			// select e2.id, e2.parentId from Entity2 e2
			if ( selectStatement.getQueryPart() instanceof SqmQueryGroup<?> ) {
				this.deduplicateSelectionItems = false;
			}
			this.entityGraphTraversalState = entityGraphTraversalState( creationContext, queryOptions );
		}
		else {
			this.domainResults = null;
			this.entityGraphTraversalState = null;
		}

		this.queryOptions = queryOptions;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.domainParameterXref = domainParameterXref;
		this.domainParameterBindings = domainParameterBindings;
		this.jpaCriteriaParamResolutions =
				domainParameterXref.getParameterResolutions()
						.getJpaCriteriaParamResolutions();
		this.domainModel = creationContext.getMappingMetamodel();
	}

	private static EntityGraphTraversalState entityGraphTraversalState
			(SqlAstCreationContext creationContext, QueryOptions queryOptions) {
		final var appliedGraph = queryOptions.getAppliedGraph();
		if ( appliedGraph != null
				&& appliedGraph.getSemantic() != null
				&& appliedGraph.getGraph() != null ) {
			return new StandardEntityGraphTraversalStateImpl(
					appliedGraph.getSemantic(), appliedGraph.getGraph(),
					creationContext.getJpaMetamodel()
			);
		}
		else {
			return null;
		}
	}

	private static Boolean stackMatchHelper(SqlAstProcessingState processingState, SqlAstProcessingState state) {
		if ( !( processingState instanceof SqlAstQueryPartProcessingState sqlAstQueryPartProcessingState ) ) {
			return false;
		}
		else if ( processingState == state ) {
			return null;
		}
		else {
			final var part = sqlAstQueryPartProcessingState.getInflightQueryPart();
			return part instanceof QueryGroup group
				&& group.getQueryParts().isEmpty()
					? null
					: false;
		}
	}

	private static Boolean matchSqlAstWithQueryPart(SqlAstProcessingState state, QueryPart cteQueryPartLocal) {
		if ( state instanceof SqlAstQueryPartProcessingState sqlAstQueryPartProcessingState ) {
			if ( sqlAstQueryPartProcessingState.getInflightQueryPart() == cteQueryPartLocal ) {
				return true;
			}
		}
		return null;
	}

	public Map<SqmParameter<?>, MappingModelExpressible<?>> getSqmParameterMappingModelExpressibleResolutions() {
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

	private SqlAstQueryNodeProcessingState currentQueryNodeProcessingState() {
		return (SqlAstQueryNodeProcessingState) getProcessingStateStack().getCurrent();
	}

	private QuerySpec currentQuerySpec() {
		return currentQueryPart().getLastQuerySpec();
	}

	private QueryPart currentQueryPart() {
		final var processingState =
				(SqlAstQueryPartProcessingState)
						getProcessingStateStack().getCurrent();
		return processingState.getInflightQueryPart();
	}

	protected SqmAliasedNodeCollector currentSqlSelectionCollector() {
		return (SqmAliasedNodeCollector) getCurrentProcessingState().getSqlExpressionResolver();
	}

	protected SqmStatement<?> getStatement() {
		return statement;
	}

	@Override
	public Dialect getDialect() {
		return creationContext.getDialect();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlTypeDescriptorIndicators

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return creationContext.getTypeConfiguration();
	}

	private SessionFactoryOptions getSessionFactoryOptions() {
		// TODO : FIX ME
		return creationContext.getSessionFactory().getSessionFactoryOptions();
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return getSessionFactoryOptions().getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public int getPreferredSqlTypeCodeForDuration() {
		return getSessionFactoryOptions().getPreferredSqlTypeCodeForDuration();
	}

	@Override
	public int getPreferredSqlTypeCodeForUuid() {
		return getSessionFactoryOptions().getPreferredSqlTypeCodeForUuid();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FromClauseAccess

	@Override
	public TableGroup findTableGroup(NavigablePath navigablePath) {
		return getFromClauseAccess().findTableGroup( navigablePath );
	}

	@Override
	public TableGroup findTableGroupOnCurrentFromClause(NavigablePath navigablePath) {
		return getFromClauseAccess().findTableGroupOnCurrentFromClause( navigablePath );
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

	@Override
	public @Nullable TableGroup findTableGroupByIdentificationVariable(String identificationVariable) {
		return getFromClauseAccess().findTableGroupByIdentificationVariable( identificationVariable );
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
		throw new UnsupportedOperationException( "Registering lock modes should only be done for result set mappings" );
	}

	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	@Override
	public boolean applyOnlyLoadByKeyFilters() {
		return false;
	}

	public FromClauseIndex getFromClauseIndex() {
		return (FromClauseIndex) getFromClauseAccess();
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		final var fromClauseIndex = fromClauseIndexStack.getCurrent();
		return fromClauseIndex == null ? lastPoppedFromClauseIndex : fromClauseIndex;
	}

	@Override
	public Stack<Clause> getCurrentClauseStack() {
		return currentClauseStack;
	}

	@Override
	public Stack<SqmQueryPart<?>> getSqmQueryPartStack() {
		return sqmQueryPartStack;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Statements

	@Override
	public SqmTranslation<T> translate() {
		final var sqmStatement = getStatement();
		//noinspection unchecked
		final T statement = (T) sqmStatement.accept( this );
		pruneTableGroupJoins();
		return new StandardSqmTranslation<>(
				statement,
				getJdbcParamsBySqmParam(),
				sqmParameterMappingModelTypes,
				lastPoppedProcessingState.getSqlExpressionResolver(),
				getFromClauseAccess()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Update statement

	@Override
	public UpdateStatement visitUpdateStatement(SqmUpdateStatement<?> sqmStatement) {
		final var oldCteContainer = this.cteContainer;
		final var cteContainer = visitCteContainer( sqmStatement );
		final var oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = sqmStatement;
		final var sqmTarget = sqmStatement.getTarget();
		final String entityName = sqmTarget.getEntityName();

		final var entityDescriptor =
				getCreationContext().getMappingMetamodel()
						.getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		final var fromClause = new FromClause( 1 );
		final var queryNodeProcessingState = new SqlAstQueryNodeProcessingStateImpl(
				fromClause,
				getCurrentProcessingState(),
				this,
				getCurrentClauseStack()::getCurrent
		);
		pushProcessingState( queryNodeProcessingState );

		try {
			consumeFromClauseRoot( sqmTarget );
			final var rootTableGroup = getFromClauseAccess().getTableGroup( sqmTarget.getNavigablePath() );

			final var assignments = visitSetClause( sqmStatement.getSetClause() );
			addVersionedAssignment( assignments::add, sqmStatement );

			Predicate suppliedPredicate = null;
			final var whereClause = sqmStatement.getWhereClause();
			if ( whereClause != null ) {
				suppliedPredicate = visitWhereClause( whereClause.getPredicate() );
			}

			return new UpdateStatement(
					cteContainer,
					(NamedTableReference) rootTableGroup.getPrimaryTableReference(),
					entityDescriptor,
					fromClause,
					assignments,
					combinePredicates( suppliedPredicate,
							combinePredicates( queryNodeProcessingState.getPredicate(),
									additionalRestrictions ) ),
					emptyList()
			);
		}
		finally {
			popProcessingStateStack();
			this.currentSqmStatement = oldSqmStatement;
			this.cteContainer = oldCteContainer;
		}
	}

	public void addVersionedAssignment(Consumer<Assignment> assignmentConsumer, SqmUpdateStatement<?> sqmStatement) {
		if ( sqmStatement.isVersioned() ) {
			final var persister =
					creationContext.getMappingMetamodel()
							.findEntityDescriptor( sqmStatement.getTarget().getEntityName() );
			if ( !persister.isVersioned() ) {
				throw new SemanticException( "Increment option specified for update of non-versioned entity" );
			}

			final var versionType = persister.getVersionType();
			if ( versionType instanceof UserVersionType ) {
				throw new SemanticException( "User-defined version types not supported for increment option" );
			}

			currentClauseStack.push( Clause.SET );
			final var versionMapping = persister.getVersionMapping();
			final var targetColumnReferences = BasicValuedPathInterpretation.from(
					(SqmBasicValuedSimplePath<?>)
							SqmExpressionHelper.get( sqmStatement.getRoot(),
									versionMapping.getPartName() ),
					this,
					jpaQueryComplianceEnabled
			).getColumnReferences();
			currentClauseStack.pop();
			assert targetColumnReferences.size() == 1;

			final var versionColumn = targetColumnReferences.get( 0 );
			final var value =
					versionMapping.getJdbcMapping().getJdbcType().isTemporal()
							? new VersionTypeSeedParameterSpecification( versionMapping )
							: versionIncrementExpression( persister, versionColumn, versionMapping, versionType );
			assignmentConsumer.accept( new Assignment( versionColumn, value ) );
		}
	}

	private static Expression versionIncrementExpression(
			EntityPersister persister,
			ColumnReference versionColumn,
			EntityVersionMapping versionMapping,
			BasicType<?> versionType) {
		final var versionJavaType = persister.getVersionJavaType();
		final Integer temporalPrecision = versionMapping.getTemporalPrecision();
		final Integer precision =
				temporalPrecision != null
						? temporalPrecision
						: versionMapping.getPrecision();
		final Integer scale = versionMapping.getScale();
		final Long length = versionMapping.getLength();
		return new BinaryArithmeticExpression(
				versionColumn,
				ADD,
				new QueryLiteral<>(
						versionJavaType.next(
								versionJavaType.seed(
										length,
										precision,
										scale,
										null
								),
								length,
								precision,
								scale,
								null
						),
						versionType
				),
				versionType
		);
	}

	@Override
	public List<Assignment> visitSetClause(SqmSetClause setClause) {
		final ArrayList<Assignment> assignments = new ArrayList<>( setClause.getAssignments().size() );

		final var target = ( (SqmDmlStatement<?>) currentSqmStatement ).getTarget();
		final String entityName = target.getEntityName();
		final var entityDescriptor =
				getCreationContext().getMappingMetamodel()
						.getEntityDescriptor( entityName );

		// Returns a new instance for collecting assignments if needed
		final var aggregateColumnAssignmentHandler =
				forEntityDescriptor( entityDescriptor, setClause.getAssignments().size() );

		for ( var sqmAssignment : setClause.getAssignments() ) {
			final SqmPathInterpretation<?> assignedPathInterpretation;
			try {
				currentClauseStack.push( Clause.SET );
				assignedPathInterpretation =
						(SqmPathInterpretation<?>)
								sqmAssignment.getTargetPath().accept( this );
			}
			finally {
				currentClauseStack.pop();
			}

			try {
				inferrableTypeAccessStack.push( assignedPathInterpretation::getExpressionType );
				currentClauseStack.push( Clause.SET_EXPRESSION );

				final var assignmentValue = sqmAssignment.getValue();
				final var assignmentValueParameter = getSqmParameter( assignmentValue );
				final var pathSqlExpression = assignedPathInterpretation.getSqlExpression();
				final var targetColumnReferences =
						pathSqlExpression instanceof SqlTupleContainer sqlTupleContainer
								? sqlTupleContainer.getSqlTuple().getColumnReferences()
								: pathSqlExpression.getColumnReference().getColumnReferences();
				if ( assignmentValueParameter != null ) {
					final ArrayList<Expression> expressions = new ArrayList<>( targetColumnReferences.size() );
					consumeSqmParameter(
							assignmentValueParameter,
							assignedPathInterpretation.getExpressionType(),
							(index, jdbcParameter) -> expressions.add( jdbcParameter )
					);
					if ( pathSqlExpression instanceof SqlTupleContainer ) {
						addAssignment(
								assignments,
								aggregateColumnAssignmentHandler,
								(Assignable) assignedPathInterpretation,
								targetColumnReferences,
								new SqlTuple( expressions, assignedPathInterpretation.getExpressionType() )
						);
					}
					else {
						assert expressions.size() == 1;
						addAssignment(
								assignments,
								aggregateColumnAssignmentHandler,
								(Assignable) assignedPathInterpretation,
								targetColumnReferences,
								expressions.get( 0 )
						);
					}
				}
				else if ( pathSqlExpression instanceof SqlTupleContainer
						&& assignmentValue instanceof SqmLiteralNull<?> ) {
					final ArrayList<Expression> expressions = new ArrayList<>( targetColumnReferences.size() );
					for ( var targetColumnReference : targetColumnReferences ) {
						expressions.add( new QueryLiteral<>( null,
								(SqlExpressible) targetColumnReference.getExpressionType() ) );
					}
					addAssignment(
							assignments,
							aggregateColumnAssignmentHandler,
							(Assignable) assignedPathInterpretation,
							targetColumnReferences,
							new SqlTuple( expressions, assignedPathInterpretation.getExpressionType() )
					);
				}
				else {
					addAssignments(
							(Expression) assignmentValue.accept( this ),
							assignedPathInterpretation,
							targetColumnReferences,
							assignments,
							aggregateColumnAssignmentHandler
					);
				}
			}
			finally {
				currentClauseStack.pop();
				inferrableTypeAccessStack.pop();
			}
		}

		if ( aggregateColumnAssignmentHandler != null ) {
			aggregateColumnAssignmentHandler.aggregateAssignments( assignments );
		}

		return assignments;
	}

	private void addAssignments(
			Expression valueExpression,
			SqmPathInterpretation<?> assignedPathInterpretation,
			List<ColumnReference> targetColumnReferences,
			ArrayList<Assignment> assignments,
			AggregateColumnAssignmentHandler aggregateColumnAssignmentHandler) {
		checkAssignment( valueExpression, assignedPathInterpretation.getExpressionType() );
		addAssignment(
				assignments,
				aggregateColumnAssignmentHandler,
				(Assignable) assignedPathInterpretation,
				targetColumnReferences,
				valueExpression
		);
	}

	private void checkAssignment(Expression valueExpression, ModelPart assignedPathType) {
		final int valueExprJdbcCount = getKeyExpressible( valueExpression.getExpressionType() ).getJdbcTypeCount();
		final int assignedPathJdbcCount = getKeyExpressible( assignedPathType ).getJdbcTypeCount();
		// TODO: why is this not an AssertionFailure instead of a DEBUG level log?!
		if ( valueExprJdbcCount != assignedPathJdbcCount ) {
			SqlTreeCreationLogger.LOGGER.debug(
					"JDBC type count does not match in UPDATE assignment between the assigned-path and the assigned-value; " +
							"this will likely lead to problems executing the query"
			);
		}
		assert assignedPathJdbcCount == valueExprJdbcCount;
	}

	private void addAssignment(
			List<Assignment> assignments,
			AggregateColumnAssignmentHandler aggregateColumnAssignmentHandler,
			Assignable assignable,
			List<ColumnReference> targetColumnReferences,
			Expression valueExpression) {
		if ( aggregateColumnAssignmentHandler != null ) {
			for ( var targetColumnReference : targetColumnReferences ) {
				aggregateColumnAssignmentHandler.addAssignment( assignments.size(), targetColumnReference );
			}
		}
		assignments.add( new Assignment( assignable, valueExpression ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Delete statement

	@Override
	public DeleteStatement visitDeleteStatement(SqmDeleteStatement<?> statement) {
		final var oldCteContainer = cteContainer;
		final var cteContainer = this.visitCteContainer( statement );
		final var oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = statement;
		final var sqmTarget = statement.getTarget();
		final String entityName = sqmTarget.getEntityName();
		final var entityDescriptor =
				creationContext.getMappingMetamodel()
						.getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		final var fromClause = new FromClause( 1 );
		final var queryNodeProcessingState = new SqlAstQueryNodeProcessingStateImpl(
				fromClause,
				getCurrentProcessingState(),
				this,
				getCurrentClauseStack()::getCurrent
		);
		pushProcessingState( queryNodeProcessingState );

		try {
			consumeFromClauseRoot( sqmTarget );
			final var rootTableGroup = getFromClauseAccess().getTableGroup( sqmTarget.getNavigablePath() );

			Predicate suppliedPredicate = null;
			final var whereClause = statement.getWhereClause();
			if ( whereClause != null ) {
				suppliedPredicate = visitWhereClause( whereClause.getPredicate() );
			}

			return new DeleteStatement(
					cteContainer,
					(NamedTableReference) rootTableGroup.getPrimaryTableReference(),
					entityDescriptor,
					fromClause,
					combinePredicates(
							suppliedPredicate,
							combinePredicates(
									queryNodeProcessingState.getPredicate(),
									additionalRestrictions
							)
					),
					emptyList()
			);
		}
		finally {
			popProcessingStateStack();
			this.currentSqmStatement = oldSqmStatement;
			this.cteContainer = oldCteContainer;
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Insert-select statement

	@Override
	public InsertStatement visitInsertSelectStatement(SqmInsertSelectStatement<?> sqmStatement) {
		final var oldCteContainer = cteContainer;
		final var cteContainer = this.visitCteContainer( sqmStatement );
		final var oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = sqmStatement;
		final String entityName = sqmStatement.getTarget().getEntityName();
		final var entityDescriptor =
				creationContext.getMappingMetamodel()
						.getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		final var selectQueryPart = sqmStatement.getSelectQueryPart();
		pushProcessingState(
				new SqlAstProcessingStateImpl(
						null,
						this,
						r -> new SqmAliasedNodePositionTracker(
								r,
								selectQueryPart.getFirstQuerySpec()
						),
						getCurrentClauseStack()::getCurrent
				)
		);
		currentClauseStack.push( Clause.INSERT );
		final InsertSelectStatement insertStatement;
		final AdditionalInsertValues additionalInsertValues;
		try {
			final var rootPath = sqmStatement.getTarget().getNavigablePath();
			final var rootTableGroup = entityDescriptor.createRootTableGroup(
					true,
					rootPath,
					sqmStatement.getTarget().getExplicitAlias(),
					null,
					() -> predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
					this
			);

			getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );

			insertStatement = new InsertSelectStatement(
					cteContainer,
					(NamedTableReference) rootTableGroup.getPrimaryTableReference(),
					entityDescriptor,
					emptyList()
			);
			additionalInsertValues = visitInsertionTargetPaths(
					(assigable, references) -> insertStatement.addTargetColumnReferences( references ),
					sqmStatement,
					entityDescriptor,
					rootTableGroup
			);

			if ( hasJoins( rootTableGroup ) ) {
				throw new SemanticException( "Not expecting multiple table references for an SQM INSERT-SELECT" );
			}
		}
		finally {
			popProcessingStateStack();
			currentClauseStack.pop();
		}

		insertStatement.setSourceSelectStatement(
				visitQueryPart( selectQueryPart )
		);

		insertStatement.getSourceSelectStatement().visitQuerySpecs(
				querySpec -> {
					final boolean appliedRowNumber =
							additionalInsertValues.applySelections( querySpec, creationContext.getSessionFactory() );
					// Just make sure that if we get here, a row number will never be applied
					// If this requires the special row number handling, it should use the mutation strategy
					assert !appliedRowNumber;
				}
		);

		insertStatement.setConflictClause( visitConflictClause( sqmStatement.getConflictClause() ) );

		this.currentSqmStatement = oldSqmStatement;
		this.cteContainer = oldCteContainer;

		return insertStatement;
	}

	private static boolean hasJoins(TableGroup rootTableGroup) {
		return !rootTableGroup.getTableReferenceJoins().isEmpty()
			|| hasJoins( rootTableGroup.getTableGroupJoins() )
			|| hasJoins( rootTableGroup.getNestedTableGroupJoins() );
	}

	private static boolean hasJoins(List<TableGroupJoin> tableGroupJoins) {
		for ( var tableGroupJoin : tableGroupJoins ) {
			final var joinedGroup = tableGroupJoin.getJoinedGroup();
			if ( joinedGroup.isInitialized() ) {
				if ( joinedGroup.isVirtual() ) {
					return hasJoins( joinedGroup );
				}
				else {
					return true;
				}
			}
		}
		return false;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Insert-values statement

	@Override
	public InsertStatement visitInsertValuesStatement(SqmInsertValuesStatement<?> sqmStatement) {
		final var oldCteContainer = this.cteContainer;
		final var cteContainer = this.visitCteContainer( sqmStatement );
		final var oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = sqmStatement;
		final String entityName = sqmStatement.getTarget().getEntityName();
		final var entityDescriptor =
				creationContext.getMappingMetamodel()
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
			final var rootPath = sqmStatement.getTarget().getNavigablePath();
			final var rootTableGroup = entityDescriptor.createRootTableGroup(
					true,
					rootPath,
					sqmStatement.getTarget().getExplicitAlias(),
					null,
					() -> predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
					this
			);

			getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );

			final var insertStatement = new InsertSelectStatement(
					cteContainer,
					(NamedTableReference) rootTableGroup.getPrimaryTableReference(),
					entityDescriptor,
					emptyList()
			);

			final var additionalInsertValues = visitInsertionTargetPaths(
					(assigable, references) -> insertStatement.addTargetColumnReferences( references ),
					sqmStatement,
					entityDescriptor,
					rootTableGroup
			);

			if ( hasJoins( rootTableGroup ) ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM INSERT-SELECT" );
			}

			for ( var sqmValues : sqmStatement.getValuesList() ) {
				final var values = visitValues( sqmValues );
				additionalInsertValues.applyValues( values );
				insertStatement.getValuesList().add( values );
			}

			insertStatement.setConflictClause( visitConflictClause( sqmStatement.getConflictClause() ) );

			return insertStatement;
		}
		finally {
			popProcessingStateStack();
			this.currentSqmStatement = oldSqmStatement;
			this.cteContainer = oldCteContainer;
		}
	}

	@Override
	public ConflictClause visitConflictClause(SqmConflictClause<?> sqmConflictClause) {
		if ( sqmConflictClause == null ) {
			return null;
		}
		final var constraintAttributes = sqmConflictClause.getConstraintPaths();
		final List<String> constraintColumnNames = new ArrayList<>( constraintAttributes.size() );
		for ( var constraintAttribute : constraintAttributes ) {
			final var assignable = ( (Assignable) constraintAttribute.accept( this ) );
			for ( var columnReference : assignable.getColumnReferences() ) {
				constraintColumnNames.add( columnReference.getSelectableName() );
			}
		}
		final var updateAction = sqmConflictClause.getConflictAction();
		final List<Assignment> assignments;
		final Predicate predicate;
		if ( updateAction == null ) {
			assignments = emptyList();
			predicate = null;
		}
		else {
			final var excludedRoot = sqmConflictClause.getExcludedRoot();
			final var entityDescriptor = resolveEntityPersister( excludedRoot.getModel() );
			final var tableGroup = entityDescriptor.createRootTableGroup(
					true,
					excludedRoot.getNavigablePath(),
					excludedRoot.getExplicitAlias(),
					new SqlAliasBaseConstant( "excluded" ),
					() -> null,
					this
			);
			registerSqmFromTableGroup( excludedRoot, tableGroup );
			assignments = visitSetClause( updateAction.getSetClause() );
			final var whereClause = updateAction.getWhereClause();
			predicate = whereClause == null ? null : visitWhereClause( whereClause.getPredicate() );
		}

		return new ConflictClause( sqmConflictClause.getConstraintName(), constraintColumnNames, assignments, predicate );
	}

	public AdditionalInsertValues visitInsertionTargetPaths(
			BiConsumer<Assignable, List<ColumnReference>> targetColumnReferenceConsumer,
			SqmInsertStatement<?> sqmStatement,
			EntityPersister entityDescriptor, TableGroup rootTableGroup) {
		final var targetPaths = sqmStatement.getInsertionTargetPaths();
		final var discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		Expression versionExpression = null;
		Expression discriminatorExpression = null;
		BasicEntityIdentifierMapping identifierMapping = null;
		// We use the id property name to null the identifier generator variable if the target paths contain the id
		Generator identifierGenerator = entityDescriptor.getGenerator();
		final String identifierPropertyName =
				identifierGenerator != null
						? entityDescriptor.getIdentifierPropertyName()
						: null;
		final String versionAttributeName;
		boolean needsVersionInsert;
		if ( entityDescriptor.isVersioned() ) {
			versionAttributeName = entityDescriptor.getVersionMapping().getVersionAttribute().getAttributeName();
			needsVersionInsert = true;
		}
		else {
			versionAttributeName = null;
			needsVersionInsert = false;
		}
		// Go through all target paths and remember if the target paths contain the version or id attributes
		for ( int i = 0; i < targetPaths.size(); i++ ) {
			final var path = targetPaths.get( i );
			final String localName = path.getNavigablePath().getLocalName();
			if ( localName.equals( identifierPropertyName ) ) {
				identifierGenerator = null;
			}
			else if ( localName.equals( versionAttributeName ) ) {
				needsVersionInsert = false;
			}
			final var assignable = (Assignable) path.accept( this );
			targetColumnReferenceConsumer.accept( assignable, assignable.getColumnReferences() );
		}
		if ( needsVersionInsert ) {
			final var versionPath = BasicValuedPathInterpretation.from(
					(SqmBasicValuedSimplePath<?>)
							SqmExpressionHelper.get( sqmStatement.getTarget(), versionAttributeName ),
					this,
					jpaQueryComplianceEnabled
			);
			final var targetColumnReferences = versionPath.getColumnReferences();
			assert targetColumnReferences.size() == 1;

			targetColumnReferenceConsumer.accept( versionPath, targetColumnReferences );
			versionExpression = new VersionTypeSeedParameterSpecification( entityDescriptor.getVersionMapping() );
		}
		if ( discriminatorMapping != null && discriminatorMapping.hasPhysicalColumn() ) {
			final var discriminatorPath = new BasicValuedPathInterpretation<>(
					new ColumnReference(
							rootTableGroup.resolveTableReference( discriminatorMapping.getContainingTableExpression() ),
							discriminatorMapping
					),
					rootTableGroup.getNavigablePath().append( discriminatorMapping.getPartName() ),
					discriminatorMapping,
					rootTableGroup
			);
			targetColumnReferenceConsumer.accept( discriminatorPath, discriminatorPath.getColumnReferences() );
			discriminatorExpression = new QueryLiteral<>(
					DiscriminatorHelper.toRelationalValue( entityDescriptor.getDiscriminatorValue() ),
					discriminatorMapping
			);

		}
		// This uses identity generation, so we don't need to list the column
		if ( identifierGenerator != null && identifierGenerator.generatedOnExecution()
				|| identifierGenerator instanceof CompositeNestedGeneratedValueGenerator ) {
			identifierGenerator = null;
		}
		else if ( identifierGenerator != null ) {
			// When we have an identifier generator, we somehow must list the identifier column in the insert statement.
			final boolean addIdColumn;
			if ( sqmStatement instanceof SqmInsertValuesStatement<?> ) {
				// For an InsertValuesStatement, we can just list the column, as we can inject a parameter in the VALUES clause.
				addIdColumn = true;
			}
			else if ( !( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator bulkInsertionCapableGenerator ) ) {
				// For non-identity generators that don't implement BulkInsertionCapableIdentifierGenerator, there is nothing we can do
				addIdColumn = false;
			}
			else {
				// Same condition as in AdditionalInsertValues#applySelections
				final Optimizer optimizer;
				if ( identifierGenerator instanceof OptimizableGenerator optimizableGenerator
						&& ( optimizer = optimizableGenerator.getOptimizer() ) != null
						&& optimizer.getIncrementSize() > 1
							|| !bulkInsertionCapableGenerator.supportsBulkInsertionIdentifierGeneration() ) {
					// If the dialect does not support window functions, we don't need the id column in the temporary table insert
					// because we will make use of the special "rn_" column that is auto-incremented and serves as temporary identifier for a row,
					// which is needed to control the generation of proper identifier values with the generator afterward
					addIdColumn = creationContext.getDialect().supportsWindowFunctions();
				}
				else {
					// If the generator supports bulk insertion and the optimizer uses an increment size of 1,
					// we can list the column, because we can emit a SQL expression.
					addIdColumn = true;
				}
			}
			identifierMapping = (BasicEntityIdentifierMapping) entityDescriptor.getIdentifierMapping();
			if ( addIdColumn ) {
				final var identifierPath = new BasicValuedPathInterpretation<>(
						new ColumnReference(
								rootTableGroup.resolveTableReference( identifierMapping.getContainingTableExpression() ),
								identifierMapping
						),
						rootTableGroup.getNavigablePath().append( identifierMapping.getPartName() ),
						identifierMapping,
						rootTableGroup
				);
				targetColumnReferenceConsumer.accept( identifierPath, identifierPath.getColumnReferences() );
			}
		}

		return new AdditionalInsertValues(
				versionExpression,
				discriminatorExpression,
				identifierGenerator,
				identifierMapping
		);
	}

	public static class AdditionalInsertValues {
		private final Expression versionExpression;
		private final Expression discriminatorExpression;
		private final Generator identifierGenerator;
		private final BasicEntityIdentifierMapping identifierMapping;
		private Expression identifierGeneratorParameter;
		private SqlSelection versionSelection;
		private SqlSelection discriminatorSelection;
		private SqlSelection identifierSelection;

		public AdditionalInsertValues(
				Expression versionExpression,
				Expression discriminatorExpression,
				Generator identifierGenerator,
				BasicEntityIdentifierMapping identifierMapping) {
			this.versionExpression = versionExpression;
			this.discriminatorExpression = discriminatorExpression;
			this.identifierGenerator = identifierGenerator;
			this.identifierMapping = identifierMapping;
		}

		public void applyValues(Values values) {
			final List<Expression> expressions = values.getExpressions();
			if ( versionExpression != null ) {
				expressions.add( versionExpression );
			}
			if ( discriminatorExpression != null ) {
				expressions.add( discriminatorExpression );
			}
			if ( identifierGenerator != null && !identifierGenerator.generatedOnExecution() ) {
				if ( identifierGeneratorParameter == null ) {
					identifierGeneratorParameter =
							new IdGeneratorParameter( identifierMapping,
									(BeforeExecutionGenerator) identifierGenerator );
				}
				expressions.add( identifierGeneratorParameter );
			}
		}

		/**
		 * Returns true if the identifier can't be applied directly and needs to be generated separately.
		 * As a replacement for the identifier, the special row_number column should be filled.
		 */
		public boolean applySelections(QuerySpec querySpec, SessionFactoryImplementor sessionFactory) {
			final var selectClause = querySpec.getSelectClause();
			if ( versionExpression != null ) {
				if ( versionSelection == null ) {
					// The position is irrelevant as this is only needed for insert
					versionSelection = new SqlSelectionImpl( versionExpression );
				}
				selectClause.addSqlSelection( versionSelection );
			}
			if ( discriminatorExpression != null ) {
				if ( discriminatorSelection == null ) {
					// The position is irrelevant as this is only needed for insert
					discriminatorSelection = new SqlSelectionImpl( discriminatorExpression );
				}
				selectClause.addSqlSelection( discriminatorSelection );
			}
			if ( identifierGenerator != null ) {
				if ( identifierSelection == null ) {
					if ( !( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator bulkInsertionCapableGenerator ) ) {
						throw new SemanticException(
								"SQM INSERT-SELECT without bulk insertion capable identifier generator: " + identifierGenerator );
					}
					if ( identifierGenerator instanceof OptimizableGenerator optimizableGenerator ) {
						final var optimizer = optimizableGenerator.getOptimizer();
						if ( optimizer != null && optimizer.getIncrementSize() > 1
								|| !bulkInsertionCapableGenerator.supportsBulkInsertionIdentifierGeneration() ) {
							// This is a special case where we have a sequence with an optimizer
							// or a table based identifier generator
							if ( !sessionFactory.getJdbcServices().getDialect().supportsWindowFunctions() ) {
								return false;
							}
							else {
								identifierSelection =
										new SqlSelectionImpl( createRowNumberingExpression( querySpec, sessionFactory ) );
								selectClause.addSqlSelection( identifierSelection );
								return true;
							}
						}
					}
					final String fragment =
							bulkInsertionCapableGenerator.determineBulkInsertionIdentifierGenerationSelectFragment(
									sessionFactory.getSqlStringGenerationContext()
							);
					// The position is irrelevant as this is only needed for insert
					identifierSelection = new SqlSelectionImpl( new SelfRenderingSqlFragmentExpression( fragment ) );
				}
				selectClause.addSqlSelection( identifierSelection );
			}
			return requiresRowNumberIntermediate();
		}

		public boolean requiresRowNumberIntermediate() {
			return identifierSelection != null
				&& !( identifierSelection.getExpression() instanceof SelfRenderingSqlFragmentExpression );
		}
	}

	private static class IdGeneratorParameter extends AbstractJdbcParameter {

		private final BeforeExecutionGenerator generator;

		public IdGeneratorParameter(BasicEntityIdentifierMapping identifierMapping, BeforeExecutionGenerator generator) {
			super( identifierMapping.getJdbcMapping() );
			this.generator = generator;
		}

		@Override
		public void bindParameterValue(
				PreparedStatement statement,
				int startPosition,
				JdbcParameterBindings jdbcParamBindings,
				ExecutionContext executionContext) throws SQLException {
			getJdbcMapping().getJdbcValueBinder().bind(
					statement,
					generator.generate( executionContext.getSession(), null, null, INSERT ),
					startPosition,
					executionContext.getSession()
			);
		}
	}

	@Override
	public Values visitValues(SqmValues sqmValues) {
		final var insertionTargetPaths =
				currentSqmStatement instanceof SqmInsertStatement<?> insertStatement
						? insertStatement.getInsertionTargetPaths()
						: null;
		final var expressions = sqmValues.getExpressions();
		final ArrayList<Expression> valuesExpressions = new ArrayList<>( expressions.size() );
		for ( int i = 0; i < expressions.size(); i++ ) {
			// todo: add WriteExpression handling
			final var sqmExpression = expressions.get( i );
			valuesExpressions.add(
					insertionTargetPaths == null
							? (Expression) sqmExpression.accept( this )
							: visitWithInferredType( sqmExpression, insertionTargetPaths.get( i ) )
			);
		}
		return new Values( valuesExpressions );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Select statement

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement<?> statement) {
		final var oldCteContainer = this.cteContainer;
		final var cteContainer = visitCteContainer( statement );
		final var oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = statement;
		final var queryPart = visitQueryPart( statement.getQueryPart() );
		try {
			return new SelectStatement( cteContainer, queryPart,
					queryPart.isRoot() ? domainResults : emptyList() );
		}
		finally {
			this.currentSqmStatement = oldSqmStatement;
			this.cteContainer = oldCteContainer;
			rootPathsForLockingCollector = null;
		}
	}

	@Override
	public DynamicInstantiation<?> visitDynamicInstantiation(SqmDynamicInstantiation<?> sqmDynamicInstantiation) {
		final var instantiationTarget = sqmDynamicInstantiation.getInstantiationTarget();
		final var dynamicInstantiation =
				new DynamicInstantiation<>( instantiationTarget.getNature(),
						interpretInstantiationTarget( instantiationTarget ) );
		for ( var sqmArgument : sqmDynamicInstantiation.getArguments() ) {
			if ( sqmArgument.getSelectableNode() instanceof SqmPath<?> sqmPath ) {
				prepareForSelection( sqmPath );
			}
			dynamicInstantiation.addArgument( sqmArgument.getAlias(),
					(DomainResultProducer<?>) sqmArgument.accept( this ) );
		}
		dynamicInstantiation.complete();
		return dynamicInstantiation;
	}

	private JavaType<?> interpretInstantiationTarget(SqmDynamicInstantiationTarget<?> instantiationTarget) {
		return getCreationContext().getTypeConfiguration().getJavaTypeRegistry()
				.resolveDescriptor( switch ( instantiationTarget.getNature() ) {
					case LIST -> List.class;
					case MAP -> Map.class;
					default -> instantiationTarget.getJavaType();
				} );
	}

	@Override
	public CteStatement visitCteStatement(SqmCteStatement<?> sqmCteStatement) {
		final var sqmCteTable = sqmCteStatement.getCteTable();
		final String cteName = getCteName( sqmCteTable );
		final var selectStatement = sqmCteStatement.getCteDefinition();
		final var queryPart = selectStatement.getQueryPart();
		final var cycleLiteral = getLiteral( sqmCteStatement.getCycleLiteral() );
		final var noCycleLiteral = getLiteral( sqmCteStatement.getNoCycleLiteral() );
		final var cycleMarkType = cycleLiteral == null ? null : cycleLiteral.getJdbcMapping();
		final var stringType =
				creationContext.getTypeConfiguration()
						.getBasicTypeForJavaType( String.class );
		if ( queryPart instanceof SqmQueryGroup<?> queryGroup
				&& queryPart.getSortSpecifications().isEmpty()
				&& queryPart.getFetchExpression() == null
				&& queryPart.getOffsetExpression() == null ) {
			switch ( queryGroup.getSetOperator() ) {
				case UNION:
				case UNION_ALL:
					if ( queryGroup.getQueryParts().size() == 2 ) {
						// This could potentially be a recursive CTE,
						// for which we need to visit the non-recursive part first
						// and register a CteStatement before visiting the recursive part.
						// This is important, because the recursive part will refer to the CteStatement,
						// hence we require that it is registered already
						final var oldCteContainer = cteContainer;
						final var subCteContainer = visitCteContainer( selectStatement );
						// Note that the following is a trimmed down version of what visitQueryGroup does
						try {
							final var firstPart = queryGroup.getQueryParts().get( 0 );
							final var secondPart = queryGroup.getQueryParts().get( 1 );
							final List<QueryPart> newQueryParts = new ArrayList<>( 2 );
							final var group = new QueryGroup(
									getProcessingStateStack().isEmpty(),
									queryGroup.getSetOperator(),
									newQueryParts
							);

							final var processingState = new SqlAstQueryPartProcessingStateImpl(
									group,
									getCurrentProcessingState(),
									this,
									DelegatingSqmAliasedNodeCollector::new,
									currentClauseStack::getCurrent,
									deduplicateSelectionItems
							);
							final var collector =
									(DelegatingSqmAliasedNodeCollector)
											processingState.getSqlExpressionResolver();
							sqmQueryPartStack.push( queryGroup );
							pushProcessingState( processingState );

							try {
								newQueryParts.add( visitQueryPart( firstPart ) );

								collector.setSqmAliasedNodeCollector(
										(SqmAliasedNodeCollector)
												lastPoppedProcessingState.getSqlExpressionResolver()
								);

								// Before visiting the second query part, setup the CteStatement and register it
								final var cteTable = new CteTable(
										cteName,
										sqmCteTable.resolveTableGroupProducer(
												cteName,
												newQueryParts.get( 0 ).getFirstQuerySpec()
														.getSelectClause().getSqlSelections(),
												lastPoppedFromClauseIndex
										)
								);

								final var cteStatement = new CteStatement(
										cteTable,
										new SelectStatement( subCteContainer, group, emptyList() ),
										sqmCteStatement.getMaterialization(),
										sqmCteStatement.getSearchClauseKind(),
										visitSearchBySpecifications( cteTable, sqmCteStatement.getSearchBySpecifications() ),
										createCteColumn( sqmCteStatement.getSearchAttributeName(), stringType ),
										visitCycleColumns( cteTable, sqmCteStatement.getCycleAttributes() ),
										createCteColumn( sqmCteStatement.getCycleMarkAttributeName(), cycleMarkType ),
										createCteColumn( sqmCteStatement.getCyclePathAttributeName(), stringType ),
										cycleLiteral,
										noCycleLiteral
								);
								oldCteContainer.addCteStatement( cteStatement );

								// Finally, visit the second part, which is potentially the recursive part
								newQueryParts.add( visitQueryPart( secondPart ) );
								return cteStatement;
							}
							finally {
								popProcessingStateStack();
								sqmQueryPartStack.pop();
							}
						}
						finally {
							this.cteContainer = oldCteContainer;
						}
					}
					break;
			}
		}
		final SelectStatement statement;
		if ( selectStatement instanceof SqmSubQuery<?> subquery ) {
			statement = visitSubQueryExpression( subquery );
		}
		else if ( selectStatement instanceof SqmSelectStatement<?> select ) {
			statement = visitSelectStatement( select );
		}
		else {
			throw new AssertionFailure( "Unrecognized select statemengt type" );
		}

		final var cteTable = new CteTable(
				cteName,
				sqmCteTable.resolveTableGroupProducer(
						cteName,
						statement.getQuerySpec().getSelectClause().getSqlSelections(),
						lastPoppedFromClauseIndex
				)
		);

		final var cteStatement = new CteStatement(
				cteTable,
				statement,
				sqmCteStatement.getMaterialization(),
				sqmCteStatement.getSearchClauseKind(),
				visitSearchBySpecifications( cteTable, sqmCteStatement.getSearchBySpecifications() ),
				createCteColumn( sqmCteStatement.getSearchAttributeName(), stringType ),
				visitCycleColumns( cteTable, sqmCteStatement.getCycleAttributes() ),
				createCteColumn( sqmCteStatement.getCycleMarkAttributeName(), cycleMarkType ),
				createCteColumn( sqmCteStatement.getCyclePathAttributeName(), stringType ),
				cycleLiteral,
				noCycleLiteral
		);
		cteContainer.addCteStatement( cteStatement );
		return cteStatement;
	}

	private String getCteName(SqmCteTable<?> sqmCteTable) {
		final String name = sqmCteTable.getName();
		if ( cteNameMapping == null ) {
			cteNameMapping = new HashMap<>();
		}
		final String key = sqmCteTable.getCteName();
		final String generatedCteName = cteNameMapping.get( key );
		if ( generatedCteName != null ) {
			return generatedCteName;
		}
		else {
			final String cteName = name != null
					? generateCteName( name )
					: generateCteName( "cte" + cteNameMapping.size() );
			cteNameMapping.put( key, cteName );
			return cteName;
		}
	}

	private String generateCteName(String baseName) {
		String name = baseName;
		int maxTries = 5;
		for ( int i = 0; i < maxTries; i++ ) {
			if ( !cteNameMapping.containsKey( name ) ) {
				return name;
			}
			name = baseName + "_" + i;
		}
		throw new InterpretationException(
				String.format(
						"Couldn't generate CTE name for base name [%s] after %d tries",
						baseName,
						maxTries
				)
		);
	}

	private Literal getLiteral(SqmLiteral<?> value) {
		return value == null ? null : (Literal) visitLiteral( value );
	}

	protected List<SearchClauseSpecification> visitSearchBySpecifications(
			CteTable cteTable,
			List<JpaSearchOrder> searchBySpecifications) {
		if ( searchBySpecifications == null || searchBySpecifications.isEmpty() ) {
			return null;
		}
		else {
			final int size = searchBySpecifications.size();
			final List<SearchClauseSpecification> searchClauseSpecifications = new ArrayList<>( size );
			for ( int i = 0; i < size; i++ ) {
				final var specification = searchBySpecifications.get( i );
				forEachCteColumn(
						cteTable,
						(SqmCteTableColumn) specification.getAttribute(),
						cteColumn -> searchClauseSpecifications.add(
								new SearchClauseSpecification(
										cteColumn,
										specification.getSortOrder(),
										specification.getNullPrecedence()
								)
						)
				);
			}
			return searchClauseSpecifications;
		}
	}

	protected CteColumn createCteColumn(String cteColumn, JdbcMapping jdbcMapping) {
		return cteColumn == null ? null : new CteColumn( cteColumn, jdbcMapping );
	}

	protected void forEachCteColumn(CteTable cteTable, SqmCteTableColumn cteColumn, Consumer<CteColumn> consumer) {
		final var cteColumns = cteTable.getCteColumns();
		final int size = cteColumns.size();
		for ( int i = 0; i < size; i++ ) {
			final var column = cteColumns.get( i );
			final String columnName = column.getColumnExpression();
			final String sqmName = cteColumn.getName();
			final int length = sqmName.length();
			if ( columnName.regionMatches( 0, sqmName, 0, length )
					&& ( columnName.length() == length
						|| columnName.charAt( length ) == '_' ) ) {
				consumer.accept( column );
			}
		}
	}

	protected List<CteColumn> visitCycleColumns(CteTable cteTable, List<JpaCteCriteriaAttribute> cycleColumns) {
		if ( cycleColumns == null || cycleColumns.isEmpty() ) {
			return null;
		}
		else {
			final int size = cycleColumns.size();
			final List<CteColumn> columns = new ArrayList<>( size );
			for ( int i = 0; i < size; i++ ) {
				forEachCteColumn(
						cteTable,
						(SqmCteTableColumn) cycleColumns.get( i ),
						columns::add
				);
			}
			return columns;
		}
	}

	@Override
	public CteContainer visitCteContainer(SqmCteContainer consumer) {
		final var sqmCteStatements = consumer.getCteStatements();
		cteContainer = new CteContainerImpl( cteContainer );
		if ( !sqmCteStatements.isEmpty() ) {
			final boolean originalDeduplicateSelectionItems = deduplicateSelectionItems;
			deduplicateSelectionItems = false;
			currentClauseStack.push( Clause.WITH );
			for ( var sqmCteStatement : sqmCteStatements ) {
				visitCteStatement( sqmCteStatement );
			}
			currentClauseStack.pop();
			deduplicateSelectionItems = originalDeduplicateSelectionItems;
			// Avoid leaking the processing state from CTEs to upper levels
			lastPoppedFromClauseIndex = null;
			lastPoppedProcessingState = null;
		}
		return cteContainer;
	}

	@Override
	public QueryPart visitQueryPart(SqmQueryPart<?> queryPart) {
		return (QueryPart) super.visitQueryPart( queryPart );
	}

	@Override
	public QueryGroup visitQueryGroup(SqmQueryGroup<?> queryGroup) {
		final var queryParts = queryGroup.getQueryParts();
		final int size = queryParts.size();
		final List<QueryPart> newQueryParts = new ArrayList<>( size );
		final var group = new QueryGroup(
				getProcessingStateStack().isEmpty(),
				queryGroup.getSetOperator(),
				newQueryParts
		);

		final var originalTrackedFetchSelectionsForGroup = trackedFetchSelectionsForGroup;
		final var orderByClause = queryGroup.getOrderByClause();
		if ( orderByClause != null && orderByClause.hasPositionalSortItem() ) {
			trackSelectionsForGroup = true;
			// Find the order by elements which refer to attributes of the selections
			// and register the navigable paths so that a list of SqlSelection is tracked for the fetch
			Map<NavigablePath, Map.Entry<Integer, List<SqlSelection>>> trackedFetchSelectionsForGroup = null;
			for ( var sortSpecification : orderByClause.getSortSpecifications() ) {
				if ( sortSpecification.getExpression() instanceof SqmAliasedNodeRef nodeRef ) {
					final var navigablePath = nodeRef.getNavigablePath();
					if ( navigablePath != null ) {
						if ( trackedFetchSelectionsForGroup == null ) {
							trackedFetchSelectionsForGroup = new HashMap<>();
						}
						trackedFetchSelectionsForGroup.put( navigablePath,
								new AbstractMap.SimpleEntry<>( nodeRef.getPosition() - 1, new ArrayList<>() ) );
					}
				}
			}

			this.trackedFetchSelectionsForGroup =
					trackedFetchSelectionsForGroup == null
							? emptyMap()
							: trackedFetchSelectionsForGroup;
		}

		final var processingState = new SqlAstQueryPartProcessingStateImpl(
				group,
				getCurrentProcessingState(),
				this,
				DelegatingSqmAliasedNodeCollector::new,
				currentClauseStack::getCurrent,
				deduplicateSelectionItems
		);
		final var collector =
				(DelegatingSqmAliasedNodeCollector)
						processingState.getSqlExpressionResolver();
		sqmQueryPartStack.push( queryGroup );
		pushProcessingState( processingState );

		FromClauseIndex firstQueryPartIndex = null;
		SqlAstProcessingState firstPoppedProcessingState = null;
		try {
			newQueryParts.add( visitQueryPart( queryParts.get( 0 ) ) );

			firstQueryPartIndex = lastPoppedFromClauseIndex;
			firstPoppedProcessingState = lastPoppedProcessingState;
			collector.setSqmAliasedNodeCollector(
					(SqmAliasedNodeCollector)
							lastPoppedProcessingState.getSqlExpressionResolver()
			);

			visitOrderByOffsetAndFetch( queryGroup, group );

			trackSelectionsForGroup = false;
			trackedFetchSelectionsForGroup = originalTrackedFetchSelectionsForGroup;
			for ( int i = 1; i < size; i++ ) {
				newQueryParts.add( visitQueryPart( queryParts.get( i ) ) );
			}

			return group;
		}
		finally {
			popProcessingStateStack();
			sqmQueryPartStack.pop();
			lastPoppedFromClauseIndex = firstQueryPartIndex;
			lastPoppedProcessingState = firstPoppedProcessingState;
		}
	}

	@Override
	public QuerySpec visitQuerySpec(SqmQuerySpec<?> sqmQuerySpec) {
		final boolean topLevel = getProcessingStateStack().isEmpty();
		final var sqlQuerySpec = new QuerySpec( topLevel, sqmQuerySpec.getFromClause().getNumberOfRoots() );

		final var originalAdditionalRestrictions = additionalRestrictions;
		additionalRestrictions = null;
		final boolean oldInNestedContext = inNestedContext;
		inNestedContext = false;

		final SqlAstQueryPartProcessingStateImpl processingState;
		if ( trackAliasedNodePositions( sqmQuerySpec ) ) {
			processingState = new SqlAstQueryPartProcessingStateImpl(
					sqlQuerySpec,
					getCurrentProcessingState(),
					this,
					resolver -> new SqmAliasedNodePositionTracker( resolver, sqmQuerySpec ),
					currentClauseStack::getCurrent,
					deduplicateSelectionItems
			);
		}
		else {
			processingState = new SqlAstQueryPartProcessingStateImpl(
					sqlQuerySpec,
					getCurrentProcessingState(),
					this,
					currentClauseStack::getCurrent,
					deduplicateSelectionItems
			);
		}

		final boolean originalDeduplicateSelectionItems = deduplicateSelectionItems;
		sqmQueryPartStack.push( sqmQuerySpec );
		// In sub-queries, we can never deduplicate the selection items as that might change semantics
		deduplicateSelectionItems = false;
		pushProcessingState( processingState );
		queryTransformers.push( new ArrayList<>() );

		try {
			return querySpec( sqmQuerySpec, sqlQuerySpec, topLevel, processingState );
		}
		finally {
			if ( additionalRestrictions != null ) {
				sqlQuerySpec.applyPredicate( additionalRestrictions );
			}
			additionalRestrictions = originalAdditionalRestrictions;
			inNestedContext = oldInNestedContext;
			popProcessingStateStack();
			queryTransformers.pop();
			sqmQueryPartStack.pop();
			deduplicateSelectionItems = originalDeduplicateSelectionItems;
		}
	}

	private QuerySpec querySpec(
			SqmQuerySpec<?> sqmQuerySpec,
			QuerySpec sqlQuerySpec,
			boolean topLevel,
			SqlAstQueryPartProcessingStateImpl processingState) {
		// we want to visit the from-clause first
		visitFromClause( sqmQuerySpec.getFromClause() );

		visitSelectClause( sqmQuerySpec.getSelectClause() );

		final var whereClause = sqmQuerySpec.getWhereClause();
		if ( whereClause != null ) {
			sqlQuerySpec.applyPredicate( visitWhereClause( whereClause.getPredicate() ) );
		}

		sqlQuerySpec.setGroupByClauseExpressions( visitGroupByClause( sqmQuerySpec.getGroupByClauseExpressions() ) );

		final var havingClausePredicate = sqmQuerySpec.getHavingClausePredicate();
		if ( havingClausePredicate != null ) {
			sqlQuerySpec.setHavingClauseRestrictions( visitHavingClause( havingClausePredicate ) );
		}

		visitOrderByOffsetAndFetch( sqmQuerySpec, sqlQuerySpec );

		if ( topLevel && statement instanceof SqmSelectStatement<?> ) {
			if ( orderByFragments != null ) {
				orderByFragments.forEach( entry -> entry.getKey().apply( sqlQuerySpec, entry.getValue(), this ) );
				orderByFragments = null;
			}
		}

		downgradeTreatUses( processingState );

		return applyTransformers( sqlQuerySpec );
	}

	/**
	 * Look for treated {@code SqmFrom} registrations that have uses of the untreated {@code SqmFrom}.
	 * These {@code SqmFrom} nodes are then not treat-joined but rather treated only in expressions.
	 * Consider the following two queries. The latter also uses the untreated {@code SqmFrom}, and
	 * hence has different semantics i.e. the treat is not filtering, but just applies where it's used.
	 *
	 * <pre>select a.id from Root r join treat(r.attribute as Subtype) a where a.id = 1</pre>
	 *
	 * <pre>select a.id from Root r join r.attribute a where treat(a as Subtype).id = 1</pre>
	 */
	private void downgradeTreatUses(SqlAstQueryPartProcessingStateImpl processingState) {
		processingState.getFromRegistrations().forEach( (key, value) -> {
			if ( value != null && value ) {
				downgradeTreatUses( getFromClauseIndex().getTableGroup( key.getNavigablePath() ) );
			}
		} );
	}

	private QuerySpec applyTransformers(QuerySpec sqlQuerySpec) {
		QuerySpec finalQuerySpec = sqlQuerySpec;
		final var transformers = queryTransformers.getCurrent();
		for ( var transformer : transformers ) {
			finalQuerySpec = transformer.transform( cteContainer, finalQuerySpec, this );
		}
		return finalQuerySpec;
	}

	private boolean trackAliasedNodePositions(SqmQuerySpec<?> sqmQuerySpec) {
		return trackSelectionsForGroup
			|| sqmQuerySpec.getOrderByClause() != null && sqmQuerySpec.getOrderByClause().hasPositionalSortItem()
			|| sqmQuerySpec.hasPositionalGroupItem()
			// Since JPA Criteria queries can use the same expression object in order or group by items,
			// we need to track the positions to be able to replace the expression in the items with alias references
			// Also see #resolveGroupOrOrderByExpression for more details
			|| statement.getQuerySource() == SqmQuerySource.CRITERIA
				&& ( sqmQuerySpec.getOrderByClause() != null || !sqmQuerySpec.getGroupByClauseExpressions().isEmpty() );
	}

	private void downgradeTreatUses(TableGroup tableGroup) {
		final var entityNameUses = tableGroupEntityNameUses.get( tableGroup );
		if ( entityNameUses != null ) {
			for ( var entry : entityNameUses.entrySet() ) {
				if ( entry.getValue().getKind() == EntityNameUse.UseKind.TREAT ) {
					entry.setValue( EntityNameUse.EXPRESSION );
				}
			}
		}
	}

	protected void visitOrderByOffsetAndFetch(SqmQueryPart<?> sqmQueryPart, QueryPart sqlQueryPart) {
		if ( sqmQueryPart.getOrderByClause() != null ) {
			currentClauseStack.push( Clause.ORDER );
			inferrableTypeAccessStack.push( () -> null );
			try {
				for ( var sortSpecification : sqmQueryPart.getOrderByClause().getSortSpecifications() ) {
					final var specification = visitSortSpecification( sortSpecification );
					if ( specification != null ) {
						sqlQueryPart.addSortSpecification( specification );
					}
				}
			}
			finally {
				inferrableTypeAccessStack.pop();
				currentClauseStack.pop();
			}
		}

		if ( sqlQueryPart.isRoot() && queryOptions.isLimitInMemoryEnabled() == TRUE ) {
			// Suppress SQL-level pagination for the top-level query so the runtime
			// can apply the requested slice in memory instead.
		}
		else if ( !containsCollectionFetches || !currentClauseStack.isEmpty() ) {
			applyOffsetAndFetch( sqmQueryPart, sqlQueryPart );
		}
		else if ( sqlQueryPart instanceof QuerySpec sqlQuerySpec
				&& canPushPaginationDown( sqmQueryPart, sqlQuerySpec ) ) {
			// Push the offset/fetch into a derived table over the root entity, leaving
			// the plural fetch joins on an outer query. See CollectionFetchPaginationQueryTransformer.
			applyOffsetAndFetch( sqmQueryPart, sqlQueryPart );
			if ( sqlQueryPart.getOffsetClauseExpression() == null
					&& sqlQueryPart.getFetchClauseExpression() == null ) {
				applyQueryOptionsOffsetAndFetch( sqlQueryPart );
			}
			registerQueryTransformer(
					new CollectionFetchPaginationQueryTransformer( queryOptions.isScrollExecution() )
			);
		}
		// else: strip the root offset/fetch (the in-memory fallback in
		// AbstractSqmSelectionQuery slices the parent list after execution).
	}

	private void applyOffsetAndFetch(SqmQueryPart<?> sqmQueryPart, QueryPart sqlQueryPart) {
		inferrableTypeAccessStack.push( () -> getTypeConfiguration().getBasicTypeForJavaType( Integer.class ) );
		sqlQueryPart.setOffsetClauseExpression( visitOffsetExpression( sqmQueryPart.getOffsetExpression() ) );
		final var fetchClauseType = sqmQueryPart.getFetchClauseType();
		if ( fetchClauseType == FetchClauseType.PERCENT_ONLY
			|| fetchClauseType == FetchClauseType.PERCENT_WITH_TIES ) {
			inferrableTypeAccessStack.pop();
			inferrableTypeAccessStack.push( () -> getTypeConfiguration().getBasicTypeForJavaType( Double.class ) );
		}
		sqlQueryPart.setFetchClauseExpression(
				visitFetchExpression( sqmQueryPart.getFetchExpression() ),
				fetchClauseType
		);
		inferrableTypeAccessStack.pop();
	}

	private boolean canPushPaginationDown(SqmQueryPart<?> sqmQueryPart, QuerySpec sqlQuerySpec) {
		if ( queryOptions.isLimitInMemoryEnabled() == TRUE ) {
			return false;
		}
		else if ( !getDialect().supportsOffsetInSubquery() ) {
			return false;
		}
		else {
			// Every root must be an entity (so the rewrite knows how to project its
			// primary-table columns). At least one root must contribute a plural
			// fetched join, otherwise the limit semantics are unaffected and the
			// rewrite is pointless. Secondary tables and joined-inheritance subtype
			// tables come along inside the inner derived table; their columns get
			// absorbed into the column-list and outer references rewritten.
			if ( !hasPluralFetchOnSomeRoot( sqlQuerySpec.getFromClause() ) ) {
				return false;
			}
			else if ( isPercentFetch( sqmQueryPart ) && !canRenderPercentFetchInSubquery() ) {
				// "fetch first N percent rows" pushed into the inner derived table
				// requires either native PERCENT support in a subquery (e.g. H2,
				// Oracle, SQL Server) or window-function-based emulation
				// (PostgreSQL/MySQL/etc. wrap the inner with row_number+count).
				// Dialects with neither (HSQLDB) bail back to the in-memory
				// fallback rather than emit unsupported SQL.
				return false;
			}
			else if ( ordersByPluralFetchBeforeOwner( (SqmQuerySpec<?>) sqmQueryPart ) ) {
				// When a query orders by a collection element before the owner,
				// pushing the owner table into a paginated subquery is not safe,
				// because the pagination ordering depends on the collection element.
				// This is arguably silly, but let's play safe here and disable the transformation in this case.
				return false;
			}
			else if ( sqmQueryPart.getFetchExpression() != null
					|| sqmQueryPart.getOffsetExpression() != null ) {
				return true;
			}
			else {
				final var limit = queryOptions.peekOriginalLimit();
				return limit != null && !limit.isEmpty();
			}
		}
	}

	private static boolean isPercentFetch(SqmQueryPart<?> sqmQueryPart) {
		final var fetchClauseType = sqmQueryPart.getFetchClauseType();
		return fetchClauseType == FetchClauseType.PERCENT_ONLY
			|| fetchClauseType == FetchClauseType.PERCENT_WITH_TIES;
	}

	private boolean canRenderPercentFetchInSubquery() {
		final var dialect = getDialect();
		return dialect.supportsFetchClause( FetchClauseType.PERCENT_ONLY )
			|| dialect.supportsWindowFunctions();
	}

	private static boolean hasPluralFetchOnSomeRoot(FromClause fromClause) {
		for ( var root : fromClause.getRoots() ) {
			if ( !( root.getModelPart() instanceof EntityMappingType ) ) {
				return false;
			}
			else if ( hasFetchedPluralReachable( root ) ) {
				return true;
			}
		}
		return false;
	}

	private static Set<SqmPath<?>> collectNonPluralPaths(SqmFromClause fromClause) {
		final var nonPluralPaths = new HashSet<SqmPath<?>>();
		for ( SqmRoot<?> root : fromClause.getRoots() ) {
			collectPaths( root, nonPluralPaths, true );
		}
		return nonPluralPaths;
	}

	private static void collectPaths(SqmFrom<?, ?> from, HashSet<SqmPath<?>> paths, boolean nonPluralFetchOnly) {
		paths.add( from );
		for ( SqmJoin<?, ?> sqmJoin : from.getSqmJoins() ) {
			if ( nonPluralFetchOnly
					&& sqmJoin instanceof SqmAttributeJoin<?,?> attributeJoin
					&& attributeJoin.isFetched() ) {
				if ( !(sqmJoin instanceof AbstractSqmPluralJoin<?, ?, ?>) ) {
					collectPaths( sqmJoin, paths, true );
				}
			}
			else {
				collectPaths( sqmJoin, paths, false );
			}
		}
		for ( SqmTreatedFrom<?, ?, ?> sqmTreat : from.getSqmTreats() ) {
			collectPaths( sqmTreat, paths, nonPluralFetchOnly );
		}
	}

	public static boolean ordersByPluralFetchBeforeOwner(SqmQuerySpec<?> sqmQuerySpec) {
		final var allowedPaths = collectNonPluralPaths( sqmQuerySpec.getFromClause() );
		final var referencedPaths = new ArrayList<SqmPath<?>>();
		final SqmPathVisitor pathVisitor = new SqmPathVisitor( referencedPaths::add );
		boolean referencesPluralFetch = false;
		for ( var sortSpecification : sqmQuerySpec.getSortSpecifications() ) {
			referencedPaths.clear();
			final SqmExpression<?> sortExpression = sortSpecification.getSortExpression();
			if ( sortExpression instanceof SqmAliasedNodeRef aliasedNodeRef ) {
				final var sqmSelection =
						sqmQuerySpec.getSelectClause().getSelections().get( aliasedNodeRef.getPosition() - 1 );
				sqmSelection.accept( pathVisitor );
			}
			else {
				sortExpression.accept( pathVisitor );
			}
			for ( SqmPath<?> referencedPath : referencedPaths ) {
				if ( isPluralFetchPath( referencedPath, allowedPaths ) ) {
					referencesPluralFetch = true;
					break;
				}
				else {
					if ( referencesPluralFetch ) {
						return true;
					}
				}
			}
		}
		return referencesPluralFetch;
	}

	private static boolean isPluralFetchPath(SqmPath<?> referencedPath, Set<SqmPath<?>> allowedPaths) {
		if ( referencedPath instanceof SqmFrom<?,?> ) {
			return !allowedPaths.contains( referencedPath );
		}
		else {
			assert referencedPath.getLhs() != null;
			return !allowedPaths.contains( referencedPath.getLhs() );
		}
	}

	/**
	 * True when a fetched plural join is reachable from {@code group} either
	 * directly or through a chain of fetched singular joins. The transformer's
	 * move pass mirrors this walk.
	 */
	private static boolean hasFetchedPluralReachable(TableGroup group) {
		for ( var join : group.getTableGroupJoins() ) {
			final var joined = join.getJoinedGroup();
			if ( joined.isFetched() ) {
				if ( joined instanceof PluralTableGroup
						|| hasFetchedPluralReachable( joined ) ) {
					return true;
				}
			}
		}
		return false;
	}


	private void applyQueryOptionsOffsetAndFetch(QueryPart sqlQueryPart) {
		final var limit = queryOptions.peekOriginalLimit();
		if ( limit != null && !limit.isEmpty() ) {
			final var integerType = getTypeConfiguration().getBasicTypeForJavaType( Integer.class );
			// Always emit both parameters so the cached plan works for any combination of
			// setFirstResult/setMaxResults values; the parameter binders default firstRow
			// to 0 and maxRows to Integer.MAX_VALUE when the corresponding option is not set.
			sqlQueryPart.setOffsetClauseExpression(
					new OffsetJdbcParameter( integerType )
			);
			sqlQueryPart.setFetchClauseExpression(
					new LimitJdbcParameter( integerType ),
					FetchClauseType.ROWS_ONLY
			);
		}
	}

	private TableGroup findTableGroupByPath(NavigablePath navigablePath) {
		return getFromClauseAccess().getTableGroup( navigablePath );
	}

	private Consumer<NavigablePath> rootPathsForLockingCollector;

	@Override
	public SelectClause visitSelectClause(SqmSelectClause selectClause) {
		currentClauseStack.push( Clause.SELECT );
		try {
			final var querySpec = currentQuerySpec();
			final var sqlSelectClause = querySpec.getSelectClause();
			if ( sqmQueryPartStack.depth() == 1 && currentClauseStack.depth() == 1 ) {
				// these 2 conditions combined *should* indicate we have the
				// root query-spec of a top-level select statement
				rootPathsForLockingCollector = querySpec::applyRootPathForLocking;
			}
			if ( selectClause.getSelections().isEmpty() ) {
				final var implicitSelection =
						determineImplicitSelection( (SqmQuerySpec<?>) getCurrentSqmQueryPart() );
				visitSelection( 0, new SqmSelection<>( implicitSelection, implicitSelection.nodeBuilder() ) );
			}
			else {
				final var selections = selectClause.getSelections();
				for ( int i = 0; i < selections.size(); i++ ) {
					visitSelection( i, selections.get( i ) );
				}
				sqlSelectClause.makeDistinct( selectClause.isDistinct() );
			}
			return sqlSelectClause;
		}
		finally {
			rootPathsForLockingCollector = null;
			currentClauseStack.pop();
		}
	}

	protected SqmFrom<?, ?> determineImplicitSelection(SqmQuerySpec<?> querySpec) {
		// Note that this is different from org.hibernate.query.hql.internal.SemanticQueryBuilder.buildInferredSelectClause
		return querySpec.getFromClause().getRoots().get( 0 );
	}

	@Override
	public Void visitSelection(SqmSelection<?> sqmSelection) {
		visitSelection(
				getCurrentSqmQueryPart().getFirstQuerySpec().getSelectClause()
						.getSelections().indexOf( sqmSelection ),
				sqmSelection
		);
		return null;
	}

	private void visitSelection(int index, SqmSelection<?> sqmSelection) {
		collectRootPathsForLocking( sqmSelection );
		inferTargetPath( index );
		callResultProducers( resultProducers( sqmSelection ) );
		if ( statement instanceof SqmInsertSelectStatement<?>
				&& contributesToTopLevelSelectClause() ) {
			inferrableTypeAccessStack.pop();
		}
	}

	private void collectRootPathsForLocking(SqmSelection<?> sqmSelection) {
		if ( rootPathsForLockingCollector != null ) {
			collectRootPathsForLocking( sqmSelection.getSelectableNode() );
		}
	}

	private void collectRootPathsForLocking(SqmSelectableNode<?> selectableNode) {
		// roughly speaking we only care about 2 cases here:
		//		1) entity path - the entity will be locked
		//		2) scalar path - the entity from which the path originates will be locked
		//
		// note, however, that we need to account for both cases as the argument to a dynamic instantiation

		if ( selectableNode instanceof SqmPath<?> selectedPath ) {
			collectRootPathsForLocking( selectedPath );
		}
		else if ( selectableNode instanceof SqmDynamicInstantiation<?> dynamicInstantiation ) {
			collectRootPathsForLocking( dynamicInstantiation );
		}
	}

	private void collectRootPathsForLocking(SqmPath<?> selectedPath) {
		assert rootPathsForLockingCollector != null;
		// Typically this comes from paths rooted in a CTE.
		// Regardless, without a path we cannot evaluate.
		if ( selectedPath != null ) {
			if ( selectedPath.getNodeType() instanceof EntityTypeImpl ) {
				rootPathsForLockingCollector.accept( selectedPath.getNavigablePath() );
			}
			else {
				collectRootPathsForLocking( selectedPath.getLhs() );
			}
		}
	}

	private void collectRootPathsForLocking(SqmDynamicInstantiation<?> dynamicInstantiation) {
		dynamicInstantiation.getArguments().forEach( ( argument ) -> {
			collectRootPathsForLocking( argument.getSelectableNode() );
		} );
	}

	private void inferTargetPath(int index) {
		// Only infer the type on the "top level" select clauses
		// todo: add WriteExpression handling
		if ( statement instanceof SqmInsertSelectStatement<?> insertSelectStatement
				&& contributesToTopLevelSelectClause() ) {
			// we infer the target path
			final var path = insertSelectStatement.getInsertionTargetPaths().get( index );
			inferrableTypeAccessStack.push( () -> determineValueMapping( path ) );
		}
	}

	private boolean contributesToTopLevelSelectClause() {
		return currentClauseStack.depth() == 1
			&& currentClauseStack.getCurrent() == Clause.SELECT;
	}

	private void callResultProducers(List<Map.Entry<String, DomainResultProducer<?>>> resultProducers) {
		final boolean needsDomainResults = domainResults != null && contributesToTopLevelSelectClause();
		final boolean collectDomainResults = collectDomainResults( needsDomainResults );
		resultProducers.forEach(
				entry -> {
					// this currentSqlSelectionCollector().next() is meant solely for resolving
					// literal reference to a selection-item in the order-by or group-by clause.
					// in the case of `DynamicInstantiation`, that ordering should ignore that
					// level here. Visiting the dynamic-instantiation will manage this for its
					// arguments
					final var domainResultProducer = entry.getValue();
					if ( !( domainResultProducer instanceof DynamicInstantiation<?> ) ) {
						currentSqlSelectionCollector().next();
					}

					if ( collectDomainResults ) {
						domainResults.add( domainResultProducer.createDomainResult( entry.getKey(), this ) );
					}
					else if ( needsDomainResults ) {
						// We just create domain results for the purpose of creating selections
						// This is necessary for top-level query specs within query groups to avoid cycles
						domainResultProducer.createDomainResult( entry.getKey(), this );
					}
					else {
						domainResultProducer.applySqlSelections( this );
					}
				}
		);
	}

	private boolean collectDomainResults(boolean needsDomainResults) {
		final var processingStateStack = getProcessingStateStack();
		if ( processingStateStack.depth() == 1 ) {
			return needsDomainResults;
		}
		else {
			final var current = processingStateStack.getCurrent();
			// Since we only want to create domain results for the first/left-most query spec within query groups,
			// we have to check if the current query spec is the left-most.
			// This is the case when all upper level in-flight query groups are still empty
			return needsDomainResults
				&& processingStateStack.findCurrentFirstWithParameter( current, BaseSqmToSqlAstConverter::stackMatchHelper ) == null;
		}
	}

	private List<Map.Entry<String, DomainResultProducer<?>>> resultProducers(SqmSelection<?> sqmSelection) {
		final var selectionNode = sqmSelection.getSelectableNode();
		if ( selectionNode instanceof SqmJpaCompoundSelection<?> selectableNode ) {
			final List<Map.Entry<String, DomainResultProducer<?>>> resultProducers =
					new ArrayList<>( selectableNode.getSelectionItems().size() );
			for ( var selectionItem : selectableNode.getSelectionItems() ) {
				if ( selectionItem instanceof SqmPath<?> path ) {
					prepareForSelection( path );
				}
				resultProducers.add(
						new AbstractMap.SimpleEntry<>( selectionItem.getAlias(),
								(DomainResultProducer<?>) selectionItem.accept( this ) )
				);
			}
			return resultProducers;

		}
		else {
			if ( selectionNode instanceof SqmPath<?> path ) {
				prepareForSelection( path );
			}
			return singletonList(
					new AbstractMap.SimpleEntry<>( sqmSelection.getAlias(),
						(DomainResultProducer<?>) selectionNode.accept( this ) )
			);
		}
	}

	protected Expression resolveGroupOrOrderByExpression(SqmExpression<?> groupByClauseExpression) {
		final int sqmPosition;
		final NavigablePath path;
		if ( groupByClauseExpression instanceof SqmAliasedNodeRef aliasedNodeRef ) {
			sqmPosition = aliasedNodeRef.getPosition() - 1;
			path = aliasedNodeRef.getNavigablePath();
		}
		else if ( statement.getQuerySource() == SqmQuerySource.CRITERIA && currentClauseStack.getCurrent() != Clause.OVER ) {
			// In JPA Criteria we could be using the same expression object for the group/order by and select item
			// We try to find the select item position for this expression here which is not necessarily just an optimization.
			// This is vital to enable the support for parameters in these expressions.
			// Databases usually don't know if a parameter marker will have the same value as another parameter marker
			// and due to that, a database usually complains when seeing something like
			// `select ?, count(*) from dual group by ?` saying that there is a missing group by for the first `?`
			// To avoid this issue, we determine the position and let the SqlAstTranslator handle the rest.
			// Usually it will render `select ?, count(*) from dual group by 1` if supported
			// or force rendering the parameter as literal instead so that the database can see the grouping is fine
			sqmPosition = indexOfExpression( getCurrentSqmQueryPart().getFirstQuerySpec(), groupByClauseExpression );
			path = null;
		}
		else {
			sqmPosition = -1;
			path = null;
		}
		return sqmPosition != -1
				? selectionExpressions( path, sqmPosition )
				: (Expression) groupByClauseExpression.accept( this );
	}

	private Expression selectionExpressions(NavigablePath path, int sqmPosition) {
		final var selections =
				path == null
						? currentSqlSelectionCollector().getSelections( sqmPosition )
						: trackedFetchSelectionsForGroup.get( path ).getValue();
		assert selections != null
				: String.format( Locale.ROOT, "No SqlSelections for SQM position `%s`", sqmPosition );
		final List<Expression> expressions = new ArrayList<>( selections.size() );
		for ( int i = 0; i < selections.size(); i++ ) {
			final var selectionExpression = selectionExpression( selections, i );
			if ( selectionExpression != null ) {
				expressions.add( selectionExpression );
			}
		}
		return expressions.size() == 1
				? expressions.get( 0 )
				: new SqlTuple( expressions, null );
	}

	private SqlSelectionExpression selectionExpression(List<SqlSelection> selections, int i) {
		final var selection = selections.get( i );
		// We skip duplicate selections which can occur when grouping/ordering by an entity alias.
		// Duplication happens because the primary key of an entity usually acts as FK target of collections
		// which is, just like the identifier itself, also registered as selection
		for ( int j = 0; j < i; j++ ) {
			if ( selections.get( j ) == selection ) {
				return null;
			}
		}
		return getCurrentSqmQueryPart() instanceof SqmQueryGroup<?>
				// Reusing the SqlSelection for query groups would be wrong because the aliases do no exist
				// So we have to use a literal expression in a new SqlSelection instance to refer to the position
				? sqlSelectionExpression( selection )
				: new SqlSelectionExpression( selection );
	}

	private SqlSelectionExpression sqlSelectionExpression(SqlSelection selection) {
		return new SqlSelectionExpression(
				new SqlSelectionImpl(
						selection.getJdbcResultSetIndex(),
						selection.getValuesArrayPosition(),
						new QueryLiteral<>(
								selection.getValuesArrayPosition(),
								basicType( Integer.class )
						),
						false
				)
		);
	}

	private int indexOfExpression(SqmQuerySpec<?> querySpec, SqmExpression<?> node) {
		final var selections = querySpec.getSelectClause().getSelections();
		if ( selections.isEmpty() ) {
			final var sqmRoots = querySpec.getRootList();
			// Otherwise it would have failed query validation before already
			assert sqmRoots.size() == 1;
			return node == sqmRoots.get( 0 ) ? 0 : -1;
		}
		else {
			final int result = indexOfExpression( 0, selections, node );
			return result < 0 ? -1 : result;
		}
	}

	private int indexOfExpression(int offset, List<? extends SqmAliasedNode<?>> selections, SqmExpression<?> node) {
		// The idea of this method is that we return the negated index of the position at which we found the node
		// and if we didn't find the node, we return the offset + size to allow for recursive invocation
		// Encoding this into the integer allows us to avoid some kind of mutable state to handle size/offset
		for ( int i = 0; i < selections.size(); i++ ) {
			final var selectableNode = selections.get( i ).getSelectableNode();
			if ( selectableNode instanceof SqmDynamicInstantiation<?> dynamicInstantiation ) {
				final int subResult = indexOfExpression(
						offset + i,
						dynamicInstantiation.getArguments(),
						node
				);
				if ( subResult >= 0 ) {
					return subResult;
				}
				offset = -subResult - i;
			}
			else if ( selectableNode instanceof SqmJpaCompoundSelection<?> compoundSelection ) {
				final var selectionItems = compoundSelection.getSelectionItems();
				for ( int j = 0; j < selectionItems.size(); j++ ) {
					if ( selectionItems.get( j ) == node ) {
						return offset + i + j;
					}
				}
				offset += selectionItems.size();
			}
			else {
				if ( selectableNode == node ) {
					return offset + i;
				}
			}
		}
		return -( offset + selections.size() );
	}

	@Override
	public List<Expression> visitGroupByClause(List<SqmExpression<?>> groupByClauseExpressions) {
		if ( !groupByClauseExpressions.isEmpty() ) {
			currentClauseStack.push( Clause.GROUP );
			inferrableTypeAccessStack.push( () -> null );
			try {
				final List<Expression> expressions = new ArrayList<>( groupByClauseExpressions.size() );
				for ( var groupByClauseExpression : groupByClauseExpressions ) {
					expressions.add( resolveGroupOrOrderByExpression( groupByClauseExpression ) );
				}
				return expressions;
			}
			finally {
				inferrableTypeAccessStack.pop();
				currentClauseStack.pop();
			}
		}
		return emptyList();
	}

	@Override
	public Predicate visitWhereClause(@Nullable SqmWhereClause whereClause) {
		return whereClause == null ? null : visitWhereClause( whereClause.getPredicate() );
	}

	private Predicate visitWhereClause(SqmPredicate sqmPredicate) {
		currentClauseStack.push( Clause.WHERE );
		inferrableTypeAccessStack.push( () -> null );
		try {
			return combinePredicates(
					sqmPredicate != null ? (Predicate) sqmPredicate.accept( this ) : null,
					consumeConjunctTreatTypeRestrictions()
			);
		}
		finally {
			inferrableTypeAccessStack.pop();
			currentClauseStack.pop();
		}
	}

	@Override
	public Predicate visitHavingClause(SqmPredicate sqmPredicate) {
		currentClauseStack.push( Clause.HAVING );
		inferrableTypeAccessStack.push( () -> null );
		try {
			return combinePredicates(
					sqmPredicate != null ? (Predicate) sqmPredicate.accept( this ) : null,
					consumeConjunctTreatTypeRestrictions()
			);
		}
		finally {
			inferrableTypeAccessStack.pop();
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
		final var expression = resolveGroupOrOrderByExpression( sortSpecification.getSortExpression() );
		return expression == null
				? null
				: new SortSpecification(
						expression,
						sortSpecification.getSortDirection(),
						sortSpecification.getNullPrecedence(),
						sortSpecification.isIgnoreCase()
				);
	}

	@Override
	public Expression visitOffsetExpression(SqmExpression<?> expression) {
		if ( expression == null ) {
			return null;
		}
		else {
			currentClauseStack.push( Clause.OFFSET );
			try {
				return (Expression) expression.accept( this );
			}
			finally {
				currentClauseStack.pop();
			}
		}
	}

	@Override
	public Expression visitFetchExpression(SqmExpression<?> expression) {
		if ( expression == null ) {
			return null;
		}
		else {
			currentClauseStack.push( Clause.FETCH );
			try {
				return (Expression) expression.accept( this );
			}
			finally {
				currentClauseStack.pop();
			}
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// FROM clause

	@Override
	public Void visitFromClause(SqmFromClause sqmFromClause) {
		currentClauseStack.push( Clause.FROM );
		try {
			// First, consume correlated roots, because these table groups can be used in join predicates of other from nodes
			sqmFromClause.visitRoots( this::consumeFromClauseCorrelatedRoot );
			sqmFromClause.visitRoots( this::consumeFromClauseRoot );
		}
		finally {
			currentClauseStack.pop();
		}
		return null;
	}

	protected void consumeFromClauseCorrelatedRoot(SqmRoot<?> sqmRoot) {
//		LOG.tracef( "Resolving SqmRoot [%s] to TableGroup", sqmRoot );
		final var fromClauseIndex = getFromClauseIndex();
//		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
//			LOG.tracef( "Already resolved SqmRoot [%s] to TableGroup", sqmRoot );
//		}
		if ( !sqmRoot.isCorrelated() ) {
			return;
		}
		final var currentQuerySpec = currentQuerySpec();
		final var sessionFactory = creationContext.getSessionFactory();
		final TableGroup tableGroup;
		if ( sqmRoot.containsOnlyInnerJoins() ) {
			// If we have just inner joins against a correlated root, we can render the joins as references
			// If we correlate a join, we have to create a special SqmRoot shell called SqmCorrelatedRootJoin.
			// The only purpose of that is to serve as SqmRoot, which is needed for the FROM clause.
			// It will always contain just a single correlated join though, which is what is actually correlated
			final SqmFrom<?, ?> from;
			if ( sqmRoot instanceof SqmCorrelatedRootJoin<?> ) {
				final var sqmJoins = sqmRoot.getSqmJoins();
				assert sqmJoins.size() == 1
					&& sqmJoins.get( 0 ).isCorrelated();
				from = sqmJoins.get( 0 );
			}
			else {
				from = sqmRoot;
			}
			final var parentTableGroup = fromClauseIndex.findTableGroup(
					from.getCorrelationParent().getNavigablePath()
			);
			if ( parentTableGroup == null ) {
				throw new InterpretationException( "Access to from node '" + from.getCorrelationParent() + "' is not possible in from-clause subqueries, unless the 'lateral' keyword is used for the subquery!" );
			}
			final var sqlAliasBase = sqlAliasBaseManager.createSqlAliasBase( parentTableGroup.getGroupAlias() );
			if ( parentTableGroup instanceof PluralTableGroup pluralTableGroup ) {
				final var correlatedPluralTableGroup = new CorrelatedPluralTableGroup(
						parentTableGroup,
						sqlAliasBase,
						currentQuerySpec,
						predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
						sessionFactory
				);
				final var elementTableGroup = pluralTableGroup.getElementTableGroup();
				if ( elementTableGroup != null ) {
					final var correlatedElementTableGroup = new CorrelatedTableGroup(
							elementTableGroup,
							sqlAliasBase,
							currentQuerySpec,
							predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
							sessionFactory
					);
					final var tableGroupJoin = new TableGroupJoin(
							elementTableGroup.getNavigablePath(),
							SqlAstJoinType.INNER,
							correlatedElementTableGroup
					);
					correlatedPluralTableGroup.registerElementTableGroup( tableGroupJoin );
				}
				final var indexTableGroup = pluralTableGroup.getIndexTableGroup();
				if ( indexTableGroup != null ) {
					final var correlatedIndexTableGroup = new CorrelatedTableGroup(
							indexTableGroup,
							sqlAliasBase,
							currentQuerySpec,
							predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
							sessionFactory
					);
					final var tableGroupJoin = new TableGroupJoin(
							indexTableGroup.getNavigablePath(),
							SqlAstJoinType.INNER,
							correlatedIndexTableGroup
					);
					correlatedPluralTableGroup.registerIndexTableGroup( tableGroupJoin );
				}
				tableGroup = correlatedPluralTableGroup;
			}
			else {
				tableGroup = new CorrelatedTableGroup(
						parentTableGroup,
						sqlAliasBase,
						currentQuerySpec,
						predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
						sessionFactory
				);
			}

			fromClauseIndex.register( from, tableGroup );
			registerPluralTableGroupParts( tableGroup );
			// Note that we do not need to register the correlated table group to the from clause
			// because that is never "rendered" in the subquery anyway.
			// Any table group joins added to the correlated table group are added to the query spec
			// as roots anyway, so nothing to worry about

//			LOG.tracef( "Resolved SqmRoot [%s] to correlated TableGroup [%s]", sqmRoot, tableGroup );
			if ( from instanceof SqmRoot<?> correlatedRoot ) {
				consumeJoins( correlatedRoot, fromClauseIndex, tableGroup );
			}
			else {
				consumeExplicitJoins( from, tableGroup );
			}
			return;
		}
		else {
			final var entityDescriptor = resolveEntityPersister( sqmRoot.getModel() );
			final var parentTableGroup = fromClauseIndex.findTableGroup(
					sqmRoot.getCorrelationParent().getNavigablePath()
			);
			// If we have non-inner joins against a correlated root, we must render the root with a correlation predicate
			tableGroup = entityDescriptor.createRootTableGroup(
					true,
					sqmRoot.getNavigablePath(),
					sqmRoot.getExplicitAlias(),
					null,
					() -> predicate -> {},
					this
			);
			final var identifierMapping = entityDescriptor.getIdentifierMapping();
			final var navigablePath =
					sqmRoot.getNavigablePath()
							.append( identifierMapping.getNavigableRole().getNavigableName() );
			final int jdbcTypeCount = identifierMapping.getJdbcTypeCount();
			if ( jdbcTypeCount == 1 ) {
				identifierMapping.forEachSelectable(
						(index, selectable) -> {
							final String containingTable = selectable.getContainingTableExpression();
							additionalRestrictions = combinePredicates(
									additionalRestrictions,
									new ComparisonPredicate(
											new ColumnReference(
													parentTableGroup.resolveTableReference( navigablePath, containingTable ),
													selectable
											),
											ComparisonOperator.EQUAL,
											new ColumnReference(
													tableGroup.resolveTableReference( navigablePath, containingTable ),
													selectable
											)
									)
							);
						}
				);
			}
			else {
				final List<Expression> lhs = new ArrayList<>( jdbcTypeCount );
				final List<Expression> rhs = new ArrayList<>( jdbcTypeCount );
				identifierMapping.forEachSelectable(
						(index, selectable) -> {
							final String containingTable = selectable.getContainingTableExpression();
							lhs.add(
									new ColumnReference(
											parentTableGroup.resolveTableReference( navigablePath, containingTable ),
											selectable
									)
							);
							rhs.add(
									new ColumnReference(
											tableGroup.resolveTableReference( navigablePath, containingTable ),
											selectable
									)
							);
						}
				);
				additionalRestrictions = combinePredicates(
						additionalRestrictions,
						new ComparisonPredicate(
								new SqlTuple( lhs, identifierMapping ),
								ComparisonOperator.EQUAL,
								new SqlTuple( rhs, identifierMapping )
						)
				);
			}
		}

//		LOG.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, tableGroup );

		fromClauseIndex.register( sqmRoot, tableGroup );
		currentQuerySpec.getFromClause().addRoot( tableGroup );

		consumeJoins( sqmRoot, fromClauseIndex, tableGroup );
	}

	protected void consumeFromClauseRoot(SqmRoot<?> sqmRoot) {
//		LOG.tracef( "Resolving SqmRoot [%s] to TableGroup", sqmRoot );
		final var fromClauseIndex = getFromClauseIndex();
//		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
//			LOG.tracef( "Already resolved SqmRoot [%s] to TableGroup", sqmRoot );
//		}
		if ( sqmRoot.isCorrelated() ) {
			return;
		}
		final var currentQueryNodeProcessingState = currentQueryNodeProcessingState();
		final TableGroup tableGroup;
		if ( sqmRoot instanceof SqmDerivedRoot<?> derivedRoot ) {
			// Temporarily push an empty FromClauseIndex to disallow access to aliases from the top query
			// Only lateral subqueries are allowed to see the aliases
			fromClauseIndexStack.push( new FromClauseIndex( null ) );
			final var statement = (SelectStatement) derivedRoot.getQueryPart().accept( this );
			fromClauseIndexStack.pop();
			final var tupleType = (AnonymousTupleType<?>) sqmRoot.getNodeType();
			final var sqlSelections =
					statement.getQueryPart().getFirstQuerySpec().getSelectClause().getSqlSelections();
			final var tableGroupProducer = tupleType.resolveTableGroupProducer(
					derivedRoot.getExplicitAlias(),
					sqlSelections,
					lastPoppedFromClauseIndex
			);
			final var columnNames = tableGroupProducer.getColumnNames();
			final var sqlAliasBase = getSqlAliasBaseGenerator().createSqlAliasBase(
					derivedRoot.getExplicitAlias() == null ? "derived" : derivedRoot.getExplicitAlias()
			);
			final String identifierVariable = sqlAliasBase.generateNewAlias();
			tableGroup = new QueryPartTableGroup(
					derivedRoot.getNavigablePath(),
					tableGroupProducer,
					statement,
					identifierVariable,
					columnNames,
					tableGroupProducer.getCompatibleTableExpressions(),
					false,
					true,
					creationContext.getSessionFactory()
			);
		}
		else if ( sqmRoot instanceof SqmFunctionRoot<?> functionRoot ) {
			tableGroup = createFunctionTableGroup(
					functionRoot.getFunction(),
					functionRoot.getNavigablePath(),
					functionRoot.getExplicitAlias(),
					false,
					true,
					functionRoot.getReusablePath( CollectionPart.Nature.INDEX.getName() ) != null
			);
		}
		else if ( sqmRoot instanceof SqmCteRoot<?> cteRoot ) {
			tableGroup = createCteTableGroup(
					getCteName( cteRoot.getCte().getCteTable() ),
					cteRoot.getNavigablePath(),
					cteRoot.getExplicitAlias(),
					true
			);
		}
		else {
			final EntityPersister entityDescriptor = resolveEntityPersister( sqmRoot.getModel() );
			tableGroup = entityDescriptor.createRootTableGroup(
					true,
					sqmRoot.getNavigablePath(),
					sqmRoot.getExplicitAlias(),
					null,
					() -> predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
					this
			);

			entityDescriptor.applyBaseRestrictions(
					currentQueryNodeProcessingState::applyPredicate,
					tableGroup,
					true,
					getLoadQueryInfluencers().getEnabledFilters(),
					false,
					null,
					this
			);
		}

//		LOG.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, tableGroup );

		registerSqmFromTableGroup( sqmRoot, tableGroup );
		currentQueryNodeProcessingState.getFromClause().addRoot( tableGroup );

		consumeJoins( sqmRoot, fromClauseIndex, tableGroup );
	}

	private void registerSqmFromTableGroup(SqmFrom<?, ?> sqmFrom, TableGroup tableGroup) {
		getFromClauseIndex().register( sqmFrom, tableGroup );
		// We also need to register the table group for the treats
		for ( SqmFrom<?, ?> sqmTreat : sqmFrom.getSqmTreats() ) {
			getFromClauseAccess().registerTableGroup( sqmTreat.getNavigablePath(), tableGroup );
		}
	}

	private TableGroup createFunctionTableGroup(
			SqmSetReturningFunction<?> function,
			NavigablePath navigablePath,
			String explicitAlias,
			boolean lateral,
			boolean canUseInnerJoins,
			boolean withOrdinality) {
		if ( !lateral ) {
			// Temporarily push an empty FromClauseIndex to disallow access to aliases from the top query
			// Only lateral subqueries are allowed to see the aliases
			fromClauseIndexStack.push( new FromClauseIndex( null ) );
		}
		final boolean oldInNestedContext = inNestedContext;
		inNestedContext = true;
		final var oldFunctionImpliedResultTypeAccess = functionImpliedResultTypeAccess;
		functionImpliedResultTypeAccess = inferrableTypeAccessStack.getCurrent();
		inferrableTypeAccessStack.push( () -> null );
		try {
			final var sqlAliasBase = getSqlAliasBaseGenerator().createSqlAliasBase(
					explicitAlias == null ? "derived" : explicitAlias
			);
			final String identifierVariable = sqlAliasBase.generateNewAlias();
			return function.convertToSqlAst( navigablePath, identifierVariable, lateral, canUseInnerJoins, withOrdinality, this );
		}
		finally {
			inferrableTypeAccessStack.pop();
			functionImpliedResultTypeAccess = oldFunctionImpliedResultTypeAccess;
			inNestedContext = oldInNestedContext;
			if ( !lateral ) {
				fromClauseIndexStack.pop();
			}
		}
	}

	private TableGroup createCteTableGroup(
			String cteName,
			NavigablePath navigablePath,
			String explicitAlias,
			boolean canUseInnerJoins) {
		final var sqlAliasBase = getSqlAliasBaseGenerator().createSqlAliasBase(
				explicitAlias == null ? cteName : explicitAlias
		);
		final String identifierVariable = sqlAliasBase.generateNewAlias();
		final var cteStatement = cteContainer.getCteStatement( cteName );
		if ( cteStatement == null ) {
			throw new InterpretationException( "Could not find CTE for name '" + cteName + "'!" );
		}
		final var cteQueryPart = ( (SelectStatement) cteStatement.getCteDefinition() ).getQueryPart();
		// If the query part of the CTE is one which we are currently processing, then this is a recursive CTE
		if ( cteQueryPart instanceof QueryGroup && TRUE == processingStateStack.findCurrentFirstWithParameter( cteQueryPart, BaseSqmToSqlAstConverter::matchSqlAstWithQueryPart ) ) {
			cteStatement.setRecursive();
		}
		final var tableGroupProducer = cteStatement.getCteTable().getTableGroupProducer();
		return new CteTableGroup(
				canUseInnerJoins,
				navigablePath,
				sqlAliasBase,
				tableGroupProducer,
				new NamedTableReference( cteName, identifierVariable ),
				tableGroupProducer.getCompatibleTableExpressions()
		);
	}

	private void consumeJoins(SqmRoot<?> sqmRoot, FromClauseIndex fromClauseIndex, TableGroup tableGroup) {
		if ( sqmRoot.getOrderedJoins() == null ) {
			consumeExplicitJoins( sqmRoot, tableGroup );
		}
		else {
//			if ( LOG.isTraceEnabled() ) {
//				LOG.tracef( "Visiting explicit joins for '%s'", sqmRoot.getNavigablePath() );
//			}
			TableGroup lastTableGroup = tableGroup;
			for ( var join : sqmRoot.getOrderedJoins() ) {
				final var ownerTableGroup = getOwnerTableGroup( fromClauseIndex, tableGroup, join );
				assert ownerTableGroup != null;
				final var actualTableGroup = getActualTableGroup( ownerTableGroup, join );
				lastTableGroup = consumeExplicitJoin( join, lastTableGroup, actualTableGroup, false );
			}
		}
	}

	private static TableGroup getOwnerTableGroup(FromClauseIndex fromClauseIndex, TableGroup tableGroup, SqmJoin<?, ?> join) {
		if ( join.getLhs() == null ) {
			return tableGroup;
		}
		else if ( join.getLhs() instanceof SqmCorrelation<?, ?> correlation ) {
			return fromClauseIndex.findTableGroup(
					correlation.getCorrelatedRoot().getNavigablePath()
			);
		}
		else {
			return fromClauseIndex.findTableGroup( join.getLhs().getNavigablePath() );
		}
	}

	private EntityPersister resolveEntityPersister(EntityDomainType<?> entityDomainType) {
		return creationContext.getMappingMetamodel()
				.getEntityDescriptor( entityDomainType.getHibernateEntityName() );
	}

	private SqlAstQueryPartProcessingState getSqlAstQueryPartProcessingState() {
		return (SqlAstQueryPartProcessingState) getCurrentProcessingState();
	}

	/**
	 * Registers {@link EntityNameUse#PROJECTION} entity name uses for all entity valued path subtypes.
	 * If the path is a treat, registers {@link EntityNameUse#TREAT} for all treated subtypes instead.
	 */
	private void registerEntityNameProjectionUsage(SqmPath<?> projectedPath, TableGroup tableGroup) {
		if ( projectedPath instanceof SqmTreatedPath<?, ?> sqmTreatedPath ) {
			final var treatedType = sqmTreatedPath.getTreatTarget();
			registerEntityNameUsage( tableGroup, EntityNameUse.TREAT, treatedType.getTypeName(), true );
			if ( projectedPath instanceof SqmFrom<?, ?> ) {
				// Register that the TREAT uses for the SqmFrom node may not be downgraded
				getSqlAstQueryPartProcessingState().registerFromUsage(
						(SqmFrom<?, ?>) sqmTreatedPath.getWrappedPath(),
						false
				);
			}
		}
		else if ( projectedPath.getReferencedPathSource().getPathType() instanceof EntityDomainType<?> entityDomainType ) {
			registerEntityNameUsage( tableGroup, EntityNameUse.PROJECTION, entityDomainType.getTypeName(), true );
			if ( projectedPath instanceof SqmFrom<?, ?> sqmFrom ) {
				// Register that the TREAT uses for the SqmFrom node may not be downgraded
				getSqlAstQueryPartProcessingState().registerFromUsage( sqmFrom, true );
			}
		}
	}

	/**
	 * If the {@link SqmPath} has a {@link PersistentAttribute} as {@link SqmPathSource},
	 * this method determines the declaring entity type of the attribute and register a {@link EntityNameUse#EXPRESSION}
	 * for the given table group. If the parent path is a treat e.g. {@code treat(alias as Subtype).attribute},
	 * it will instead register a {@link EntityNameUse#TREAT} for the treated type.
	 */
	private void registerPathAttributeEntityNameUsage(SqmPath<?> sqmPath, TableGroup tableGroup) {
		final var parentPath = sqmPath.getLhs();
		if ( getCurrentProcessingState() instanceof SqlAstQueryPartProcessingState sqlAstQueryPartProcessingState ) {
			if ( parentPath instanceof SqmFrom<?, ?> sqmFrom ) {
				sqlAstQueryPartProcessingState.registerFromUsage( sqmFrom, true );
			}
			if ( sqmPath instanceof SqmFrom<?, ?> ) {
				sqlAstQueryPartProcessingState.registerFromUsage( (SqmFrom<?, ?>) sqmPath, true );
			}
		}
		if ( !( sqmPath instanceof SqmTreatedPath<?, ?> )
				&& tableGroup.getModelPart().getPartMappingType() instanceof EntityMappingType entityType
				&& sqmPath.getResolvedModel() instanceof PersistentAttribute<?, ?> ) {
			final String attributeName = sqmPath.getResolvedModel().getPathName();
			final EntityMappingType parentType;
			if ( parentPath instanceof SqmTreatedPath<?, ?> treatedPath ) {
				// A treated attribute usage i.e. `treat(alias as Subtype).attribute = 1`
				final var treatTarget = treatedPath.getTreatTarget();
				if ( treatTarget.getPersistenceType() == ENTITY ) {
					parentType = creationContext.getMappingMetamodel().getEntityDescriptor( treatTarget.getTypeName() );
					// The following is an optimization to avoid rendering treat conditions into predicates.
					// Imagine an HQL predicate like `treat(alias as Subtype).attribute is null or alias.name = '...'`.
					// If the `attribute` is basic, we will render a case wrapper around the column expression
					// and hence we can safely skip adding the `type(alias) = Subtype and ...` condition to the SQL.
					final var entityNameUse =
							parentType.findSubPart( attributeName ).asBasicValuedModelPart() != null
									// We only apply this optimization for basic valued model parts for now
									? EntityNameUse.OPTIONAL_TREAT
									: EntityNameUse.BASE_TREAT;
					registerEntityNameUsage( tableGroup, entityNameUse, treatTarget.getTypeName() );
				}
				else {
					parentType = entityType;
				}
			}
			else {
				// A simple attribute usage e.g. `alias.attribute = 1`
				parentType = entityType;
			}
			final var attributeMapping = parentType.findAttributeMapping( attributeName );
			if ( attributeMapping == null ) {
				if ( attributeName.equals( parentType.getIdentifierMapping().getAttributeName() ) ) {
					if ( parentType.getIdentifierMapping() instanceof EmbeddableValuedModelPart ) {
						// Until HHH-16571 is fixed, we must also register an entity name use for the root entity descriptor name
						registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
								parentType.getRootEntityDescriptor().getEntityName() );
					}
					registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
							entityNameFromDiscriminatorMapping( parentType ) );
				}
				else {
					// If the attribute mapping can't be found on the declaring type and it is not the identifier,
					// this signals that we are working with an arbitrarily chosen attribute from a subclass.
					// Register entity name usages for all subtypes that declare the attribute with the same name then
					for ( var subMappingType : parentType.getSubMappingTypes() ) {
						if ( subMappingType.findDeclaredAttributeMapping( attributeName ) != null ) {
							registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION, subMappingType.getEntityName() );
						}
					}
				}
			}
			else {
				registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
						attributeMapping.findContainingEntityMapping().getEntityName() );
			}
		}
	}

	private static String entityNameFromDiscriminatorMapping(EntityMappingType parentType) {
		final var discriminator = parentType.getDiscriminatorMapping();
		// This is needed to preserve optimization for joined + discriminator inheritance
		// see JoinedSubclassEntityPersister#getIdentifierMappingForJoin
		return discriminator != null
			&& discriminator.hasPhysicalColumn()
			&& !parentType.getSubMappingTypes().isEmpty()
				? parentType.getRootEntityDescriptor().getEntityName()
				: parentType.getEntityName();
	}

	@Override
	public boolean supportsEntityNameUsage() {
		return true;
	}

	@Override
	public void registerEntityNameUsage(
			TableGroup tableGroup,
			EntityNameUse entityNameUse,
			String treatTargetTypeName) {
		registerEntityNameUsage(
				tableGroup,
				entityNameUse,
				treatTargetTypeName,
				entityNameUse.getKind() == EntityNameUse.UseKind.PROJECTION
		);
	}

	private void registerEntityNameUsage(
			TableGroup tableGroup,
			EntityNameUse entityNameUse,
			String treatTargetTypeName,
			boolean projection) {
		final EntityPersister persister;
		if ( tableGroup.getModelPart() instanceof EmbeddableValuedModelPart ) {
			persister = null;
			final var embeddableDomainType =
					creationContext.getJpaMetamodel()
							.findEmbeddableType( treatTargetTypeName );
			if ( embeddableDomainType == null || !embeddableDomainType.isPolymorphic() ) {
				return;
			}
		}
		else {
			persister =
					creationContext.getMappingMetamodel()
							.findEntityDescriptor( treatTargetTypeName );
			if ( persister == null || !persister.isPolymorphic() ) {
				return;
			}
		}
		final TableGroup actualTableGroup;
		final EntityNameUse finalEntityNameUse;
		if ( tableGroup instanceof CorrelatedTableGroup correlatedTableGroup ) {
			actualTableGroup = correlatedTableGroup.getCorrelatedTableGroup();
			// For correlated table groups we can't apply filters,
			// as the context is in which the use happens may only affect the result of the subquery
			finalEntityNameUse =
					entityNameUse == EntityNameUse.EXPRESSION
							? entityNameUse
							: EntityNameUse.PROJECTION;
		}
		else {
			actualTableGroup =
					tableGroup instanceof PluralTableGroup pluralTableGroup
							? pluralTableGroup.getElementTableGroup()
							: tableGroup;
			finalEntityNameUse =
					entityNameUse == EntityNameUse.EXPRESSION
						|| entityNameUse == EntityNameUse.PROJECTION
						|| contextAllowsTreatOrFilterEntityNameUse()
							? entityNameUse
							: EntityNameUse.EXPRESSION;
		}
		final var entityNameUses =
				tableGroupEntityNameUses.computeIfAbsent( actualTableGroup,
						group -> new HashMap<>( 1 ) );
		entityNameUses.compute( treatTargetTypeName,
				(s, existingUse) -> finalEntityNameUse.stronger( existingUse ) );

		if ( persister != null ) {
			// Resolve the table reference for all types which we register an entity name use for.
			// Also, force table group initialization for treats when needed to ensure correct cardinality
			final var useKind = finalEntityNameUse.getKind();
			if ( actualTableGroup.isInitialized()
					|| useKind == EntityNameUse.UseKind.TREAT
						&& actualTableGroup.canUseInnerJoins()
						&& !((EntityMappingType) actualTableGroup.getModelPart().getPartMappingType())
								.isTypeOrSuperType( persister ) ) {
				actualTableGroup.resolveTableReference( null, persister.getTableName() );
			}

			if ( projection ) {
				EntityMappingType superMappingType = persister;
				while ( (superMappingType = superMappingType.getSuperMappingType()) != null ) {
					entityNameUses.putIfAbsent( superMappingType.getEntityName(), EntityNameUse.PROJECTION );
					actualTableGroup.resolveTableReference( null,
							superMappingType.getEntityPersister().getTableName() );
				}
			}

			// If we encounter a treat or projection use, we also want register the use for all subtypes.
			// We do this here to not have to expand entity name uses during pruning later on
			if ( useKind == EntityNameUse.UseKind.TREAT ) {
				for ( var subType : persister.getSubMappingTypes() ) {
					entityNameUses.compute( subType.getEntityName(),
							(s, existingUse) -> finalEntityNameUse.stronger( existingUse ) );
				}
			}
			else if ( useKind == EntityNameUse.UseKind.PROJECTION ) {
				for ( var subType : persister.getSubMappingTypes() ) {
					entityNameUses.compute( subType.getEntityName(),
							(s, existingUse) -> finalEntityNameUse.stronger( existingUse ) );
				}
			}
		}
		else {
			// No need to do anything else for embeddables
			return;
		}

	}

	private boolean contextAllowsTreatOrFilterEntityNameUse() {
		return switch ( getCurrentClauseStack().getCurrent() ) {
			// A TREAT or FILTER EntityNameUse is only allowed in these clauses,
			// but only if it's not in a nested context
			case SET, FROM, GROUP, HAVING, WHERE -> !inNestedContext;
			default -> false;
		};
	}

	protected void registerTypeUsage(DiscriminatorSqmPath<?> path) {
		registerTypeUsage( getFromClauseAccess().getTableGroup( path.getNavigablePath().getParent() ) );
	}

	protected void registerTypeUsage(TableGroup tableGroup) {
		// When we encounter a discriminator path i.e. a use of `type( alias )`
		// we have to resolve all subclass tables, otherwise we might get wrong results
		// It might be worth deferring this process to the pruning phase when we start to prune subclass joins in more cases
		// The biggest optimization that we currently don't do yet is capturing how this discriminator path is restricted
		// If we could infer a list of treated entity names from the restrictions,
		// we could intersect that with the tableGroupTreatUsages and thus eliminate subclass joins.
		// The hard part about this is inferring the list though, because we must respect the predicate transitivity
		// i.e. for `a = 1 or type(..) = ...` means nothing can be inferred,
		// but for `a = 1 and type(..) = A or type(..) = B` we can infer `A, B`
		// The OR junction allows to create a union of entity name lists of all sub-predicates
		// The AND junction allows to create an intersection of entity name lists of all sub-predicates
		if ( tableGroup.getModelPart().getPartMappingType() instanceof EntityMappingType mappingType ) {
			final var persister = mappingType.getEntityPersister();
			if ( persister.isPolymorphic() ) {
				final var discriminatorMapping = persister.getDiscriminatorMapping();
				if ( discriminatorMapping != null && !discriminatorMapping.hasPhysicalColumn() ) {
					final var currentClause = getCurrentClauseStack().getCurrent();
					if ( currentClause != Clause.WHERE && currentClause != Clause.HAVING ) {
						// Where and having clauses are handled specially with EntityNameUse.FILTER and pruning
						registerEntityNameUsage( tableGroup, EntityNameUse.PROJECTION, persister.getEntityName(), true );
					}
					else {
						final int subclassTableSpan = persister.getSubclassTableSpan();
						for ( int i = 0; i < subclassTableSpan; i++ ) {
							tableGroup.resolveTableReference( null, persister.getSubclassTableName( i ) );
						}
					}
				}
			}
		}
	}

	protected void pruneTableGroupJoins() {
		for ( var entry : tableGroupEntityNameUses.entrySet() ) {
			final var tableGroup = entry.getKey();
			if ( tableGroup.isInitialized() ) {
				final var entityNameUses = entry.getValue();
				final var modelPart = tableGroup.getModelPart();
				final var partMappingType =
						modelPart instanceof PluralAttributeMapping pluralAttributeMapping
								? pluralAttributeMapping.getElementDescriptor().getPartMappingType()
								: modelPart.getPartMappingType();
				if ( partMappingType instanceof EntityPersister entityPersister ) {
					entityPersister.pruneForSubclasses( tableGroup, entityNameUses );
				}
			}
		}
	}

	protected void consumeExplicitJoins(SqmFrom<?, ?> sqmFrom, TableGroup lhsTableGroup) {
//		if ( LOG.isTraceEnabled() ) {
//			LOG.tracef( "Visiting explicit joins for '%s'", sqmFrom.getNavigablePath() );
//		}
		sqmFrom.visitSqmJoins(
				sqmJoin -> {
					final TableGroup actualTableGroup = getActualTableGroup( lhsTableGroup, sqmJoin );
					registerPathAttributeEntityNameUsage( sqmJoin, actualTableGroup );
					consumeExplicitJoin( sqmJoin, actualTableGroup, actualTableGroup, true );
				}
		);
		final var sqmTreats = sqmFrom.getSqmTreats();
		if ( !sqmTreats.isEmpty() ) {
			final var queryPartProcessingState = getSqlAstQueryPartProcessingState();
			queryPartProcessingState.registerTreatedFrom( sqmFrom );
			// If a SqmFrom is used anywhere even though treats exists,
			// the treats are context dependent and hence we need to downgrade TREAT entity uses to EXPRESSION.
			// Treat expressions will be protected via predicates or case when expressions,
			// but we may not filter rows based on the TREAT entity uses.
			if ( lhsTableGroup.hasRealJoins() ) {//|| sqmFrom instanceof SqmRoot<?> ) {
				queryPartProcessingState.registerFromUsage( sqmFrom, true );
			}
			for ( var sqmTreat : sqmTreats ) {
				final var actualTableGroup = getActualTableGroup( lhsTableGroup, sqmTreat );
				// We don't know the context yet in which a treat is used, so we have to register base treats and track the usage
				registerEntityNameUsage( actualTableGroup, EntityNameUse.BASE_TREAT,
						( (SqmTreatedPath<?, ?>) sqmTreat ).getTreatTarget().getTypeName() );
				consumeExplicitJoins( sqmTreat, actualTableGroup );
			}
		}
	}

	protected TableGroup consumeExplicitJoin(
			SqmJoin<?, ?> sqmJoin,
			TableGroup lhsTableGroup,
			TableGroup ownerTableGroup,
			boolean transitive) {
		if ( sqmJoin instanceof SqmAttributeJoin<?, ?> attributeJoin ) {
			return consumeAttributeJoin( attributeJoin, lhsTableGroup, ownerTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmCrossJoin<?> crossJoin ) {
			return consumeCrossJoin( crossJoin, lhsTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmEntityJoin<?,?> entityJoin ) {
			return consumeEntityJoin( entityJoin, lhsTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmDerivedJoin<?> derivedJoin ) {
			return consumeDerivedJoin( derivedJoin, lhsTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmFunctionJoin<?> functionJoin ) {
			return consumeFunctionJoin( functionJoin, lhsTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmCteJoin<?> cteJoin ) {
			return consumeCteJoin( cteJoin, lhsTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmPluralPartJoin<?, ?> pluralPartJoin ) {
			return consumePluralPartJoin( pluralPartJoin, ownerTableGroup, transitive );
		}
		else {
			throw new InterpretationException( "Could not resolve SqmJoin [" + sqmJoin.getNavigablePath() + "] to TableGroupJoin" );
		}
	}

	private TableGroup getActualTableGroup(TableGroup lhsTableGroup, SqmPath<?> path) {
		// The actual table group in case of PluralTableGroups usually is the element table group,
		// but if the SqmPath is a SqmPluralPartJoin e.g. `join key(mapAlias) k`
		// or the SqmPath is a simple path for the key e.g. `select key(mapAlias)`,
		// then we want to return the PluralTableGroup instead
		if ( lhsTableGroup instanceof PluralTableGroup pluralTableGroup
				&& !( path instanceof SqmPluralPartJoin<?, ?> )
				&& CollectionPart.Nature.fromNameExact( path.getNavigablePath().getLocalName() ) == null ) {
			final var elementTableGroup = pluralTableGroup.getElementTableGroup();
			// The element table group could be null for basic collections
			if ( elementTableGroup != null ) {
				return elementTableGroup;
			}
		}
		return lhsTableGroup;
	}

	private TableGroup consumeAttributeJoin(
			SqmAttributeJoin<?, ?> sqmJoin,
			TableGroup lhsTableGroup,
			TableGroup ownerTableGroup,
			boolean transitive) {

		final var pathSource = sqmJoin.getReferencedPathSource();
		final var sqmJoinType = sqmJoin.getSqmJoinType();

		final var sqmJoinNavigablePath = sqmJoin.getNavigablePath();

		final var modelPart =
				ownerTableGroup.getModelPart()
						.findSubPart( pathSource.getPathName(),
								resolveExplicitTreatTarget( sqmJoin, this ) );

		final var sqmTreats = sqmJoin.getSqmTreats();
		final SqmPredicate joinPredicate;
		final SqmPredicate[] treatPredicates;
		final boolean discriminatedAssociationTreatJoin =
				isDiscriminatedAssociationTreatJoin( sqmJoin, sqmTreats, modelPart );
		final boolean hasPredicate;
		if ( !sqmTreats.isEmpty() ) {
			if ( sqmTreats.size() == 1 && !discriminatedAssociationTreatJoin ) {
				// If there is only a single treat, combine the predicates just as they are
				final var treatedAttributeJoin =
						(SqmTreatedAttributeJoin<?, ?, ?>)
								sqmTreats.get( 0 );
				joinPredicate = SqmCreationHelper.combinePredicates(
						sqmJoin.getJoinPredicate(),
						treatedAttributeJoin.getJoinPredicate()
				);
				treatPredicates = null;
				hasPredicate = joinPredicate != null;
			}
			else {
				// When there are multiple predicates, we have to apply type filters
				joinPredicate = sqmJoin.getJoinPredicate();
				treatPredicates = new SqmPredicate[sqmTreats.size()];
				boolean hasTreatPredicate = false;
				for ( int i = 0; i < sqmTreats.size(); i++ ) {
					final var treatedAttributeJoin =
							(SqmTreatedAttributeJoin<?, ?, ?>)
									sqmTreats.get( i );
					final var p = treatedAttributeJoin.getJoinPredicate();
					treatPredicates[i] = p;
					hasTreatPredicate = hasTreatPredicate || p != null;
				}
				hasPredicate = joinPredicate != null || hasTreatPredicate || discriminatedAssociationTreatJoin;
			}
		}
		else {
			joinPredicate = sqmJoin.getJoinPredicate();
			treatPredicates = null;
			hasPredicate = joinPredicate != null;
		}
		final var joinedTableGroupJoin =
				((TableGroupJoinProducer) modelPart)
						.createTableGroupJoin(
								sqmJoinNavigablePath,
								ownerTableGroup,
								sqmJoin.getExplicitAlias(),
								null,
								sqmJoinType.getCorrespondingSqlJoinType(),
								sqmJoin.isFetched(),
								hasPredicate,
								this
						);
		final var joinedTableGroup = joinedTableGroupJoin.getJoinedGroup();

		if ( pathSource instanceof PluralPersistentAttribute ) {
			assert modelPart instanceof PluralAttributeMapping;
			if ( sqmJoin.isFetched() ) {
				containsCollectionFetches = true;
			}
		}
		else {
			// Since this is an explicit join, we force the initialization of a possible lazy table group
			// to retain the cardinality, but only if this is a non-trivial attribute join.
			// Left or inner singular attribute joins without a predicate can be safely optimized away
			if ( hasPredicate || sqmJoinType != SqmJoinType.INNER && sqmJoinType != SqmJoinType.LEFT ) {
				joinedTableGroup.getPrimaryTableReference();
			}
		}

		lhsTableGroup.addTableGroupJoin( joinedTableGroupJoin );

		registerSqmFromTableGroup( sqmJoin, joinedTableGroup );
		registerPluralTableGroupParts( joinedTableGroup );
		if ( sqmJoin.isFetched() ) {
			// A fetch is like a projection usage, so register that properly
			registerEntityNameProjectionUsage( sqmJoin, getActualTableGroup( joinedTableGroup, sqmJoin ) );
		}
		registerPathAttributeEntityNameUsage( sqmJoin, ownerTableGroup );
		if ( !sqmJoin.hasTreats()
				&& sqmJoin.getReferencedPathSource().getPathType()
						instanceof EntityDomainType<?> entityDomainType ) {
			final var elementTableGroup =
					joinedTableGroup instanceof PluralTableGroup pluralTableGroup
							? pluralTableGroup.getElementTableGroup()
							: joinedTableGroup;
			final var entityModelPart = (EntityValuedModelPart) elementTableGroup.getModelPart();
			final var entityDescriptor = entityModelPart.getEntityMappingType().getEntityPersister();
			if ( entityDescriptor.getSuperMappingType() != null ) {
				// This is a non-treated join with an entity which is an inheritance subtype,
				// register a TREAT entity name use to filter only the entities of the correct type.
				registerEntityNameUsage( elementTableGroup, EntityNameUse.TREAT,
						entityDomainType.getHibernateEntityName() );
			}
		}

		// Implicit joins in the predicate might alter the nested table group joins,
		// so defer determination of the join for predicate until after the predicate was visited
		final TableGroupJoin joinForPredicate;
		// add any additional join restrictions
		if ( hasPredicate ) {
			joinForPredicate = getJoinForPredicate(
					sqmJoin,
					sqmJoinNavigablePath,
					joinPredicate,
					treatPredicates,
					sqmTreats,
					discriminatedAssociationTreatJoin,
					joinedTableGroup,
					modelPart,
					joinedTableGroupJoin,
					sqmJoinType
			);
		}
		else {
			joinForPredicate = determineJoinForPredicateApply( joinedTableGroupJoin );
		}
		// Since joins on treated paths will never cause table pruning, we need to add a join condition for the treat
		if ( sqmJoin.getLhs() instanceof SqmTreatedPath<?, ?> treatedPath ) {
			final var treatTarget = treatedPath.getTreatTarget();
			if ( treatTarget.getPersistenceType() == ENTITY ) {
				joinForPredicate.applyPredicate(
						createTreatTypeRestriction(
								treatedPath.getWrappedPath(),
								(EntityDomainType<?>) treatTarget
						)
				);
			}
		}

		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, joinedTableGroup );
		}
		return joinedTableGroup;
	}

	private TableGroupJoin getJoinForPredicate(
			SqmAttributeJoin<?, ?> sqmJoin,
			NavigablePath sqmJoinNavigablePath,
			SqmPredicate joinPredicate,
			SqmPredicate[] treatPredicates,
			List<SqmTreatedFrom<?, ?, ?>> sqmTreats,
			boolean discriminatedAssociationTreatJoin,
			TableGroup joinedTableGroup,
			ModelPart modelPart,
			TableGroupJoin joinedTableGroupJoin,
			SqmJoinType sqmJoinType) {
		if ( sqmJoin.isFetched() ) {
			QUERY_MESSAGE_LOGGER.debugf( "Join fetch [%s] is restricted", sqmJoinNavigablePath );
		}

		final var oldJoin = currentlyProcessingJoin;
		currentlyProcessingJoin = sqmJoin;
		Predicate predicate =
				joinPredicate == null
						? null
						: visitNestedTopLevelPredicate( joinPredicate );
		if ( treatPredicates != null ) {
			final var orPredicate = new Junction( Junction.Nature.DISJUNCTION );
			for ( int i = 0; i < treatPredicates.length; i++ ) {
				final var treatType = (EntityDomainType<?>) sqmTreats.get( i ).getTreatTarget();
				orPredicate.add( combinePredicates(
						discriminatedAssociationTreatJoin
								? createDiscriminatedAssociationTreatTypeRestriction( joinedTableGroup, modelPart, treatType )
								: createTreatTypeRestriction( sqmJoin, treatType ),
						treatPredicates[i] == null ? null : visitNestedTopLevelPredicate( treatPredicates[i] )
				) );
			}
			predicate = predicate != null ? combinePredicates( predicate, orPredicate ) : orPredicate;
		}
		final var joinForPredicate = determineJoinForPredicateApply( joinedTableGroupJoin );
		if ( discriminatedAssociationTreatJoin
				&& sqmJoinType == SqmJoinType.INNER
				&& (joinedTableGroup.isVirtual() || joinForPredicate.getJoinedGroup().isVirtual() ) ) {
			// Predicates on the virtual join itself are not rendered, so for inner joins
			// we can safely enforce the treat restriction as an equivalent query predicate.
			additionalRestrictions = combinePredicates( additionalRestrictions, predicate );
		}
		// If translating the join predicate didn't initialize the table group,
		// we can safely apply it on the collection table group instead
		else if ( joinForPredicate.getJoinedGroup().isInitialized() ) {
			joinForPredicate.applyPredicate( predicate );
		}
		else {
			joinedTableGroupJoin.applyPredicate( predicate );
		}
		currentlyProcessingJoin = oldJoin;
		return joinForPredicate;
	}

	private static boolean isDiscriminatedAssociationTreatJoin(
			SqmAttributeJoin<?, ?> sqmJoin, List<SqmTreatedFrom<?, ?, ?>> sqmTreats, ModelPart modelPart) {
		return !sqmTreats.isEmpty()
			&& ( modelPart instanceof DiscriminatedAssociationModelPart
				|| modelPart instanceof PluralAttributeMapping pluralAttributeMapping
						&& pluralAttributeMapping.getElementDescriptor() instanceof DiscriminatedAssociationModelPart
				|| sqmJoin.getNodeType() instanceof AnyMappingDomainType<?> );
	}

	private TableGroup consumeCrossJoin(SqmCrossJoin<?> sqmJoin, TableGroup lhsTableGroup, boolean transitive) {
		final var tableGroup =
				resolveEntityPersister( sqmJoin.getReferencedPathSource() )
						.createRootTableGroup(
								true,
								sqmJoin.getNavigablePath(),
								sqmJoin.getExplicitAlias(),
								null,
								() -> predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
								this
						);
		final var tableGroupJoin = new TableGroupJoin(
				sqmJoin.getNavigablePath(),
				SqlAstJoinType.CROSS,
				tableGroup
		);
		lhsTableGroup.addTableGroupJoin( tableGroupJoin );
		registerSqmFromTableGroup( sqmJoin, tableGroup );
		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
		}
		return tableGroup;
	}

	private TableGroup consumeEntityJoin(SqmEntityJoin<?,?> sqmJoin, TableGroup lhsTableGroup, boolean transitive) {
		final var entityDescriptor = resolveEntityPersister( sqmJoin.getReferencedPathSource() );
		final var loadQueryInfluencers = getLoadQueryInfluencers();
		final var correspondingSqlJoinType = sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType();
		final var tableGroup = entityDescriptor.createRootTableGroup(
				correspondingSqlJoinType == SqlAstJoinType.INNER
					|| correspondingSqlJoinType == SqlAstJoinType.CROSS,
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				null,
				() -> p -> {},
				this
		);
		registerSqmFromTableGroup( sqmJoin, tableGroup );

		if ( entityDescriptor.isInherited() && !sqmJoin.hasTreats() ) {
			// Register new treat to apply the discriminator condition to the table reference itself, see #pruneTableGroupJoins
			registerEntityNameUsage( tableGroup, EntityNameUse.TREAT, entityDescriptor.getEntityName() );
		}

		final var tableGroupJoin = new TableGroupJoin(
				sqmJoin.getNavigablePath(),
				correspondingSqlJoinType,
				tableGroup
		);
		lhsTableGroup.addTableGroupJoin( tableGroupJoin );

		entityDescriptor.applyBaseRestrictions(
				tableGroupJoin::applyPredicate,
				tableGroup,
				true,
				loadQueryInfluencers.getEnabledFilters(),
				false,
				null,
				this
		);

		final var auxiliaryMapping = entityDescriptor.getAuxiliaryMapping();
		if ( auxiliaryMapping != null ) {
			auxiliaryMapping.applyPredicate( tableGroupJoin, loadQueryInfluencers );
		}

		final var joinPredicate = sqmJoin.getJoinPredicate();
		if ( joinPredicate != null ) {
			final var oldJoin = currentlyProcessingJoin;
			currentlyProcessingJoin = sqmJoin;
			tableGroupJoin.applyPredicate( visitNestedTopLevelPredicate( joinPredicate ) );
			currentlyProcessingJoin = oldJoin;
		}
		else if ( correspondingSqlJoinType != SqlAstJoinType.CROSS ) {
			// TODO: should probably be a SyntaxException
			throw new SemanticException( "Entity join did not specify a join condition [" + sqmJoin + "]"
					+ " (specify a join condition with 'on' or use 'cross join')" );
		}

		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, tableGroupJoin.getJoinedGroup() );
		}
		return tableGroup;
	}

	private TableGroup consumeDerivedJoin(SqmDerivedJoin<?> sqmJoin, TableGroup parentTableGroup, boolean transitive) {
		if ( !sqmJoin.isLateral() ) {
			// Temporarily push an empty FromClauseIndex to disallow access to aliases from the top query
			// Only lateral subqueries are allowed to see the aliases
			fromClauseIndexStack.push( new FromClauseIndex( null ) );
		}
		final var statement = (SelectStatement) sqmJoin.getQueryPart().accept( this );
		if ( !sqmJoin.isLateral() ) {
			fromClauseIndexStack.pop();
		}
		final var tupleType = (AnonymousTupleType<?>) sqmJoin.getNodeType();
		final var tableGroupProducer = tupleType.resolveTableGroupProducer(
				sqmJoin.getExplicitAlias(),
				statement.getQueryPart().getFirstQuerySpec().getSelectClause().getSqlSelections(),
				lastPoppedFromClauseIndex
		);
		final var sqlAliasBase =
				getSqlAliasBaseGenerator()
						.createSqlAliasBase( sqmJoin.getExplicitAlias() == null
								? "derived"
								: sqmJoin.getExplicitAlias() );
		final String identifierVariable = sqlAliasBase.generateNewAlias();
		final var queryPartTableGroup = new QueryPartTableGroup(
				sqmJoin.getNavigablePath(),
				tableGroupProducer,
				statement,
				identifierVariable,
				tableGroupProducer.getColumnNames(),
				tableGroupProducer.getCompatibleTableExpressions(),
				sqmJoin.isLateral(),
				false,
				creationContext.getSessionFactory()
		);
		getFromClauseIndex().register( sqmJoin, queryPartTableGroup );

		final var tableGroupJoin = new TableGroupJoin(
				queryPartTableGroup.getNavigablePath(),
				sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
				queryPartTableGroup
		);
		parentTableGroup.addTableGroupJoin( tableGroupJoin );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			final var oldJoin = currentlyProcessingJoin;
			currentlyProcessingJoin = sqmJoin;
			tableGroupJoin.applyPredicate( visitNestedTopLevelPredicate( sqmJoin.getJoinPredicate() ) );
			currentlyProcessingJoin = oldJoin;
		}

		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, queryPartTableGroup );
		}
		return queryPartTableGroup;
	}

	private TableGroup consumeFunctionJoin(SqmFunctionJoin<?> sqmJoin, TableGroup parentTableGroup, boolean transitive) {
		final var correspondingSqlJoinType = sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType();
		final var tableGroup = createFunctionTableGroup(
				sqmJoin.getFunction(),
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				sqmJoin.isLateral(),
				correspondingSqlJoinType == SqlAstJoinType.INNER
					|| correspondingSqlJoinType == SqlAstJoinType.CROSS,
				sqmJoin.getReusablePath( CollectionPart.Nature.INDEX.getName() ) != null
		);
		getFromClauseIndex().register( sqmJoin, tableGroup );

		final var tableGroupJoin = new TableGroupJoin(
				tableGroup.getNavigablePath(),
				correspondingSqlJoinType,
				tableGroup
		);
		parentTableGroup.addTableGroupJoin( tableGroupJoin );

		// add any additional join restrictions
		final var joinPredicate = sqmJoin.getJoinPredicate();
		if ( joinPredicate != null ) {
			final var oldJoin = currentlyProcessingJoin;
			currentlyProcessingJoin = sqmJoin;
			tableGroupJoin.applyPredicate( visitNestedTopLevelPredicate( joinPredicate ) );
			currentlyProcessingJoin = oldJoin;
		}

		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, tableGroup );
		}
		return tableGroup;
	}

	private TableGroup consumeCteJoin(SqmCteJoin<?> sqmJoin, TableGroup parentTableGroup, boolean transitive) {
		final var correspondingSqlJoinType = sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType();
		final var tableGroup = createCteTableGroup(
				getCteName( sqmJoin.getCte().getCteTable() ),
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				correspondingSqlJoinType == SqlAstJoinType.INNER
					|| correspondingSqlJoinType == SqlAstJoinType.CROSS
		);
		getFromClauseIndex().register( sqmJoin, tableGroup );

		final var tableGroupJoin = new TableGroupJoin(
				tableGroup.getNavigablePath(),
				correspondingSqlJoinType,
				tableGroup
		);
		parentTableGroup.addTableGroupJoin( tableGroupJoin );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			final var oldJoin = currentlyProcessingJoin;
			currentlyProcessingJoin = sqmJoin;
			tableGroupJoin.applyPredicate( visitNestedTopLevelPredicate( sqmJoin.getJoinPredicate() ) );
			currentlyProcessingJoin = oldJoin;
		}

		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, tableGroup );
		}
		return tableGroup;
	}

	private TableGroup consumePluralPartJoin(SqmPluralPartJoin<?, ?> sqmJoin, TableGroup lhsTableGroup, boolean transitive) {
		final var pluralTableGroup = (PluralTableGroup) lhsTableGroup;
		final var tableGroup = getPluralPartTableGroup( pluralTableGroup, sqmJoin.getReferencedPathSource() );
		getFromClauseIndex().register( sqmJoin, tableGroup );

		assert sqmJoin.getJoinPredicate() == null;
		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, tableGroup );
		}
		return tableGroup;
	}

	private TableGroup getPluralPartTableGroup(PluralTableGroup pluralTableGroup, SqmPathSource<?> pathSource) {
		final var nature = CollectionPart.Nature.fromNameExact( pathSource.getPathName() );
		if ( nature != null ) {
			switch ( nature ) {
				case INDEX:
					return pluralTableGroup.getIndexTableGroup();
				case ELEMENT:
					return pluralTableGroup.getElementTableGroup();
			}
		}
		throw new UnsupportedOperationException( "Unsupported plural part join nature: " + nature );
	}

	private <X> X prepareReusablePath(SqmPath<?> sqmPath, Supplier<X> supplier) {
		return prepareReusablePath( sqmPath, fromClauseIndexStack.getCurrent(), supplier, false );
	}

	private <X> X prepareReusablePath(
			SqmPath<?> sqmPath,
			FromClauseIndex fromClauseIndex,
			Supplier<X> supplier,
			boolean allowLeftJoins) {
		final Consumer<TableGroup> implicitJoinChecker = tg -> {};
//		if ( getCurrentClauseStack().getCurrent() != Clause.SET_EXPRESSION ) {
//			implicitJoinChecker = tg -> {};
//		}
//		else {
//			implicitJoinChecker = BaseSqmToSqlAstConverter::verifyManipulationImplicitJoin;
//		}
		prepareReusablePath( fromClauseIndex, sqmPath, implicitJoinChecker );

		// Create the table group for every path that can potentially require one,
		// as some paths require joining the target table i.e. inverse one-to-one
		// Note that this will not necessarily create joins immediately, as table groups are lazy
		if ( sqmPath instanceof SqmEntityValuedSimplePath<?>
				|| sqmPath instanceof SqmEmbeddedValuedSimplePath<?>
				|| sqmPath instanceof SqmAnyValuedSimplePath<?> ) {
			final var existingTableGroup =
					fromClauseIndex.findTableGroupForGetOrCreate( sqmPath.getNavigablePath(), allowLeftJoins );
			if ( existingTableGroup == null ) {
				final var createdTableGroup = createTableGroup(
						getActualTableGroup(
							fromClauseIndex.getTableGroup( sqmPath.getLhs().getNavigablePath() ),
							sqmPath
						),
						sqmPath,
						allowLeftJoins
				);
				if ( createdTableGroup != null && sqmPath instanceof SqmTreatedPath<?, ?> ) {
					fromClauseIndex.register( sqmPath, createdTableGroup );
				}
			}
		}
		return supplier.get();
	}

	private TableGroup prepareReusablePath(
			FromClauseIndex fromClauseIndex,
			JpaPath<?> path,
			Consumer<TableGroup> implicitJoinChecker) {
		final var sqmPath = (SqmPath<?>) path;
		final SqmPath<?> parentPath;
		final boolean treated;
		if ( sqmPath instanceof SqmTreatedPath<?, ?> treatedPath ) {
			parentPath = treatedPath.getWrappedPath();
			treated = true;
		}
		else {
			parentPath = sqmPath.getLhs();
			treated = false;
		}
		if ( parentPath == null ) {
			return sqmPath instanceof SqmFunctionPath<?> functionPath
				&& functionPath.getReferencedPathSource() instanceof CompositeSqmPathSource<?>
					? (TableGroup) visitFunctionPath( functionPath )
					: null;
		}
		final var parentTableGroup = getActualTableGroup(
				fromClauseIndex.findTableGroupForGetOrCreate( parentPath.getNavigablePath() ),
				sqmPath
		);
		if ( parentTableGroup == null ) {
			final var createdParentTableGroup =
					prepareReusablePath( fromClauseIndex, parentPath, implicitJoinChecker );
			if ( createdParentTableGroup == null ) {
				throw new SqlTreeCreationException( "Could not locate TableGroup - " + parentPath.getNavigablePath() );
			}
			final TableGroup newTableGroup;
			if ( parentPath instanceof SqmTreatedPath<?, ?> ) {
				fromClauseIndex.register( parentPath, createdParentTableGroup );
				newTableGroup = createdParentTableGroup;
			}
			else if ( createdParentTableGroup instanceof PluralTableGroup pluralTableGroup ) {
				final var nature = CollectionPart.Nature.fromName( parentPath.getNavigablePath().getLocalName() );
				assert nature != null;
				newTableGroup = pluralTableGroup.getTableGroup( nature );
			}
			else {
				newTableGroup = getActualTableGroup(
						createTableGroup( createdParentTableGroup, parentPath, false ),
						sqmPath
				);
			}
			if ( newTableGroup != null ) {
				implicitJoinChecker.accept( newTableGroup );
				registerPathAttributeEntityNameUsage( sqmPath, newTableGroup );
				if ( treated ) {
					fromClauseIndex.register( sqmPath, newTableGroup );
				}
			}
			return newTableGroup;
		}
		else if ( treated ) {
			fromClauseIndex.register( sqmPath, parentTableGroup );
		}

		upgradeToInnerJoinIfNeeded( parentTableGroup, sqmPath, parentPath, fromClauseIndex );

		registerPathAttributeEntityNameUsage( sqmPath, parentTableGroup );

		return parentTableGroup;
	}

	private void upgradeToInnerJoinIfNeeded(
			TableGroup parentTableGroup,
			SqmPath<?> sqmPath,
			SqmPath<?> parentPath,
			FromClauseIndex fromClauseIndex) {
		if ( getCurrentClauseStack().getCurrent() != Clause.SELECT
				&& parentPath instanceof SqmSimplePath<?>
				&& CollectionPart.Nature.fromName( parentPath.getNavigablePath().getLocalName() ) == null
				&& parentPath.getParentPath() != null
				&& parentTableGroup.getModelPart() instanceof ToOneAttributeMapping toOneAttributeMapping ) {
			// we need to handle the case of an implicit path involving a to-one
			// association that path has been previously joined using left.
			// typically, this indicates that the to-one is being
			// fetched - the fetch would use a left-join.  however, since the path is
			// used outside the select-clause also, we need to force the join to be inner
			final String partName = sqmPath.getResolvedModel().getPathName();
			if ( !toOneAttributeMapping.isFkOptimizationAllowed()
					|| !( toOneAttributeMapping.findSubPart( partName ) instanceof ValuedModelPart pathPart )
					|| !toOneAttributeMapping.getForeignKeyDescriptor().isKeyPart( pathPart ) ) {
				final NavigablePath parentParentPath = parentPath.getParentPath().getNavigablePath();
				final TableGroup parentParentTableGroup = fromClauseIndex.findTableGroup( parentParentPath );
				final TableGroupJoin tableGroupJoin = parentParentTableGroup.findTableGroupJoin( parentTableGroup );
				// We might get null here if the parentParentTableGroup is correlated and tableGroup is from the outer query
				// In this case, we don't want to override the join type, though it is debatable if it's ok to reuse a join in this case
				if ( tableGroupJoin != null ) {
					tableGroupJoin.setJoinType( SqlAstJoinType.INNER );
				}
			}
		}
	}

	private void prepareForSelection(SqmPath<?> selectionPath) {
		// Don't create joins for plural part paths as that will be handled
		// through a cardinality preserving mechanism in visitIndexAggregateFunction/visitElementAggregateFunction
		final var path =
				selectionPath instanceof AbstractSqmSpecificPluralPartPath<?>
						? selectionPath.getLhs().getLhs()
						: selectionPath;
		final var fromClauseIndex = getFromClauseIndex();
		final var tableGroup = fromClauseIndex.findTableGroupForGetOrCreate( path.getNavigablePath() );
		if ( tableGroup == null ) {
			prepareReusablePath( path, () -> null );
			if ( path.getLhs() != null && !( path instanceof SqmEntityValuedSimplePath<?>
					|| path instanceof SqmEmbeddedValuedSimplePath<?>
					|| path instanceof SqmAnyValuedSimplePath<?>
					|| path instanceof SqmTreatedPath<?, ?> ) ) {
				// Since this is a selection, we must create a table group for the path as a DomainResult will be created
				// But only create it for paths that are not handled by #prepareReusablePath anyway
				final var createdTableGroup = createTableGroup(
						getActualTableGroup( fromClauseIndex.getTableGroup( path.getLhs().getNavigablePath() ), path ),
						path,
						false
				);
				if ( createdTableGroup != null ) {
					registerEntityNameProjectionUsage( path, createdTableGroup );
				}
			}
			else {
				registerEntityNameProjectionUsage( path, fromClauseIndex.findTableGroup( path.getNavigablePath() ) );
			}
		}
		else {
			registerEntityNameProjectionUsage( path, tableGroup );
			if ( path instanceof SqmSimplePath<?> && CollectionPart.Nature.fromName( path.getNavigablePath().getLocalName() ) == null ) {
				// If a table group for a selection already exists, we must make sure that the join type is INNER
				fromClauseIndex.findTableGroup( path.getNavigablePath().getParent() )
						.findTableGroupJoin( tableGroup )
						.setJoinType( SqlAstJoinType.INNER );
			}
		}
	}

	private TableGroup createTableGroup(TableGroup parentTableGroup, SqmPath<?> joinedPath, boolean allowLeftJoins) {
		final var lhsPath = joinedPath.getLhs();
		final var fromClauseIndex = getFromClauseIndex();
		final var subPart = parentTableGroup.getModelPart().findSubPart(
				joinedPath.getReferencedPathSource().getPathName(),
				lhsPath instanceof SqmTreatedPath<?, ?> treatedPath && treatedPath.getTreatTarget().getPersistenceType() == ENTITY
						? resolveEntityPersister( (EntityDomainType<?>) treatedPath.getTreatTarget() )
						: null
		);

		final TableGroup tableGroup;
		if ( subPart instanceof TableGroupJoinProducer joinProducer ) {
			if ( fromClauseIndex.findTableGroupOnCurrentFromClause( parentTableGroup.getNavigablePath() ) == null
					&& !isRecursiveCte( parentTableGroup ) ) {
				final var queryNodeProcessingState = currentQueryNodeProcessingState();
				// The parent table group is on a parent query, so we need a root table group
				tableGroup = joinProducer.createRootTableGroupJoin(
						joinedPath.getNavigablePath(),
						parentTableGroup,
						null,
						null,
						null,
						false,
						queryNodeProcessingState::applyPredicate,
						this
				);
				queryNodeProcessingState.getFromClause().addRoot( tableGroup );
			}
			else {
				final boolean mappedByOrNotFoundToOne = isMappedByOrNotFoundToOne( joinProducer );
				final var compatibleLeftJoin =
						mappedByOrNotFoundToOne
								? parentTableGroup.findCompatibleJoin( joinProducer, SqlAstJoinType.LEFT )
								: null;
				final var sqlAstJoinType = mappedByOrNotFoundToOne ? SqlAstJoinType.LEFT : null;
				final var compatibleTableGroup =
						compatibleLeftJoin != null
								? compatibleLeftJoin.getJoinedGroup()
								: parentTableGroup.findCompatibleJoinedGroup( joinProducer, SqlAstJoinType.INNER );
				if ( compatibleTableGroup == null ) {
					final var tableGroupJoin = joinProducer.createTableGroupJoin(
							joinedPath.getNavigablePath(),
							parentTableGroup,
							null,
							null,
							allowLeftJoins ? sqlAstJoinType : null,
							false,
							false,
							this
					);
					// Implicit joins in the ON clause need to be added as nested table group joins
					final boolean nested = currentlyProcessingJoin != null;
					if ( nested ) {
						parentTableGroup.addNestedTableGroupJoin( tableGroupJoin );
					}
					else {
						parentTableGroup.addTableGroupJoin( tableGroupJoin );
					}
					tableGroup = tableGroupJoin.getJoinedGroup();
				}
				else {
					tableGroup = compatibleTableGroup;
					// Also register the table group under its original navigable path, which possibly contains an alias
					// This is important, as otherwise we might create new joins in subqueries which are unnecessary
					fromClauseIndex.registerTableGroup( tableGroup.getNavigablePath(), tableGroup );
					// Upgrade the join type to inner if the context doesn't allow left joins
					if ( compatibleLeftJoin != null && !allowLeftJoins ) {
						compatibleLeftJoin.setJoinType( SqlAstJoinType.INNER );
					}
				}
			}

			fromClauseIndex.register( joinedPath, tableGroup );
			registerPluralTableGroupParts( joinedPath.getNavigablePath(), tableGroup );
		}
		else {
			tableGroup = null;
		}
		return tableGroup;
	}

	private boolean isMappedByOrNotFoundToOne(TableGroupJoinProducer joinProducer) {
		if ( joinProducer instanceof ToOneAttributeMapping toOne ) {
			return toOne.hasNotFoundAction()
				// ToOne( mappedBy = "..." ) always has a referenced property name and is the target side
				|| toOne.getReferencedPropertyName() != null && toOne.getSideNature() == ForeignKeyDescriptor.Nature.TARGET;
		}
		return false;
	}

	private boolean isRecursiveCte(TableGroup tableGroup) {
		return tableGroup instanceof CteTableGroup cteTableGroup
			&& cteContainer.getCteStatement( cteTableGroup.getPrimaryTableReference().getTableId() ).isRecursive();
	}

	private void registerPluralTableGroupParts(TableGroup tableGroup) {
		registerPluralTableGroupParts( null, tableGroup );
	}

	private void registerPluralTableGroupParts(NavigablePath navigablePath, TableGroup tableGroup) {
		if ( tableGroup instanceof PluralTableGroup pluralTableGroup ) {
			final var elementTableGroup = pluralTableGroup.getElementTableGroup();
			if ( elementTableGroup != null ) {
				getFromClauseAccess().registerTableGroup(
						navigablePath == null || navigablePath == tableGroup.getNavigablePath()
								? elementTableGroup.getNavigablePath()
								: navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
						elementTableGroup
				);
			}
			if ( pluralTableGroup.getIndexTableGroup() != null ) {
				getFromClauseAccess().registerTableGroup(
						navigablePath == null || navigablePath == tableGroup.getNavigablePath()
								? pluralTableGroup.getIndexTableGroup().getNavigablePath()
								: navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
						pluralTableGroup.getIndexTableGroup()
				);
			}
		}
	}

	@Override
	public @Nullable SqlAstJoinType getCurrentlyProcessingJoinType() {
		return currentlyProcessingJoin == null
				? null
				: currentlyProcessingJoin.getSqmJoinType().getCorrespondingSqlJoinType();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath handling
	//		- Note that SqmFrom references defined in the FROM-clause are already
	//			handled during `#visitFromClause`

	@Override
	public Expression visitRootPath(SqmRoot<?> sqmRoot) {
		final var resolved = getFromClauseAccess().findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolved != null ) {
//			LOG.tracef( "SqmRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolved );
			return visitTableGroup( resolved, sqmRoot );
		}

		throw new InterpretationException( "SqmRoot not yet resolved to TableGroup" );
	}

	@Override
	public Object visitRootDerived(SqmDerivedRoot<?> sqmRoot) {
		final var resolved = getFromClauseAccess().findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolved != null ) {
//			LOG.tracef( "SqmDerivedRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolved );
			return visitTableGroup( resolved, sqmRoot );
		}

		throw new InterpretationException( "SqmDerivedRoot not yet resolved to TableGroup" );
	}

	@Override
	public Object visitRootFunction(SqmFunctionRoot<?> sqmRoot) {
		final var resolved = getFromClauseAccess().findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolved != null ) {
//			LOG.tracef( "SqmFunctionRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolved );
			return visitTableGroup( resolved, sqmRoot );
		}

		throw new InterpretationException( "SqmFunctionRoot not yet resolved to TableGroup" );
	}

	@Override
	public Object visitRootCte(SqmCteRoot<?> sqmRoot) {
		final var resolved = getFromClauseAccess().findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolved != null ) {
//			LOG.tracef( "SqmCteRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolved );
			return visitTableGroup( resolved, sqmRoot );
		}

		throw new InterpretationException( "SqmCteRoot not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> sqmJoin) {
		final var existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
//			LOG.tracef( "SqmAttributeJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmAttributeJoin not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitQualifiedDerivedJoin(SqmDerivedJoin<?> sqmJoin) {
		final var existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
//			LOG.tracef( "SqmDerivedJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmDerivedJoin not yet resolved to TableGroup" );
	}

	@Override
	public Object visitQualifiedFunctionJoin(SqmFunctionJoin<?> sqmJoin) {
		final var existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
//			LOG.tracef( "SqmFunctionJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmFunctionJoin not yet resolved to TableGroup" );
	}

	@Override
	public Object visitQualifiedCteJoin(SqmCteJoin<?> sqmJoin) {
		final var existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
//			LOG.tracef( "SqmCteJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmCteJoin not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitCrossJoin(SqmCrossJoin<?> sqmJoin) {
		final var existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
//			LOG.tracef( "SqmCrossJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmCrossJoin not yet resolved to TableGroup" );
	}

	@Override
	public Object visitPluralPartJoin(SqmPluralPartJoin<?, ?> sqmJoin) {
		final var existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
//			LOG.tracef( "SqmPluralPartJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmPluralPartJoin not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitQualifiedEntityJoin(SqmEntityJoin<?,?> sqmJoin) {
		final var existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
//			LOG.tracef( "SqmEntityJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmEntityJoin not yet resolved to TableGroup" );
	}

	private Expression visitTableGroup(TableGroup tableGroup, SqmPath<?> path) {
		final var tableGroupModelPart = tableGroup.getModelPart();

		final ModelPart actualModelPart;
		final NavigablePath navigablePath;
		if ( tableGroupModelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
			actualModelPart = pluralAttributeMapping.getElementDescriptor();
			navigablePath = tableGroup.getNavigablePath().append( actualModelPart.getPartName() );
		}
		else {
			actualModelPart = tableGroupModelPart;
			navigablePath = tableGroup.getNavigablePath();
		}
		return createExpression( tableGroup, navigablePath, actualModelPart, path );
	}

	private Expression createExpression(
			TableGroup tableGroup,
			NavigablePath navigablePath,
			ModelPart actualModelPart,
			SqmPath<?> path) {
		final Expression result;
		if ( actualModelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			final var inferredEntityMapping = (EntityValuedModelPart) getInferredValueMapping();
			final ModelPart resultModelPart;
			final EntityValuedModelPart interpretationModelPart;
			final TableGroup tableGroupToUse;
			if ( inferredEntityMapping == null ) {
				// When the inferred mapping is null, we try to resolve to the FK by default, which is fine because
				// expansion to all target columns for select and group by clauses is handled in EntityValuedPathInterpretation
				if ( entityValuedModelPart instanceof EntityAssociationMapping associationMapping
						&& isFkOptimizationAllowed( path, associationMapping ) ) {
					// If the table group uses an association mapping that is not a one-to-many,
					// we make use of the FK model part - unless the path is a non-optimizable join,
					// for which we should always use the target's identifier to preserve semantics
					final ModelPart targetPart = associationMapping.getForeignKeyDescriptor().getPart(
							associationMapping.getSideNature()
					);
					if ( entityValuedModelPart.getPartMappingType() == associationMapping.getPartMappingType() ) {
						resultModelPart = targetPart;
					}
					else {
						// If the table group is for a different mapping type i.e. an inheritance subtype,
						// lookup the target part on that mapping type
						resultModelPart = entityValuedModelPart.findSubPart( targetPart.getPartName(), null );
					}
				}
				else if ( entityValuedModelPart instanceof AnonymousTupleEntityValuedModelPart ) {
					// The FK of AnonymousTupleEntityValuedModelParts always refers to the PK, so we can safely use the FK
					resultModelPart = ( (AnonymousTupleEntityValuedModelPart) entityValuedModelPart ).getForeignKeyPart();
				}
				else {
					// Otherwise, we use the identifier mapping of the target entity type
					resultModelPart = entityValuedModelPart.getEntityMappingType().getIdentifierMapping();
				}
				interpretationModelPart = entityValuedModelPart;
				tableGroupToUse = null;
			}
			else if ( inferredEntityMapping instanceof ToOneAttributeMapping toOneAttributeMapping ) {
				// If the inferred mapping is a to-one association,
				// we use the FK target part, which must be located on the entity mapping
				final ModelPart targetPart = toOneAttributeMapping.getForeignKeyDescriptor().getPart(
						toOneAttributeMapping.getSideNature().inverse()
				);
				if ( entityValuedModelPart.getPartMappingType() == toOneAttributeMapping.getPartMappingType() ) {
					resultModelPart = targetPart;
				}
				else {
					// If the table group is for a different mapping type i.e. an inheritance subtype,
					// lookup the target part on that mapping type
					resultModelPart = entityValuedModelPart.findSubPart( targetPart.getPartName(), null );
				}
				interpretationModelPart = toOneAttributeMapping;
				tableGroupToUse = null;
			}
			else if ( inferredEntityMapping instanceof EntityCollectionPart collectionPart ) {
				// If the inferred mapping is a collection part, we try to make use of the FK again to avoid joins

				// If the inferred mapping is a collection part, we try to make use of the FK again to avoid joins
				if ( tableGroup.getModelPart() instanceof CollectionPart ) {
					tableGroupToUse = findTableGroup( tableGroup.getNavigablePath().getParent() );
				}
				else {
					tableGroupToUse = tableGroup;
				}

				if ( collectionPart.getCardinality() == EntityCollectionPart.Cardinality.ONE_TO_MANY ) {
					resultModelPart = collectionPart.getAssociatedEntityMappingType().getIdentifierMapping();
				}
				else {
					assert collectionPart.getCardinality() == EntityCollectionPart.Cardinality.MANY_TO_MANY;
					final var manyToManyPart = (ManyToManyCollectionPart) collectionPart;

					if ( entityValuedModelPart == collectionPart ) {
						// When we compare the same collection parts, we can just use the FK part
						resultModelPart = manyToManyPart.getForeignKeyDescriptor().getKeyPart();
					}
					else if ( entityValuedModelPart instanceof EntityAssociationMapping tableGroupAssociation ) {
						// If the table group model part is an association, we check if the FK targets are compatible
						final var pathTargetPart =
								tableGroupAssociation.getForeignKeyDescriptor()
										.getPart( tableGroupAssociation.getSideNature().inverse() );
						final var inferredTargetPart =
								manyToManyPart.getForeignKeyDescriptor()
										.getPart( ForeignKeyDescriptor.Nature.TARGET );

						// If the inferred association and table group association targets are the same,
						// or the table group association refers to the primary key, we can safely use the FK part
						if ( pathTargetPart == inferredTargetPart || tableGroupAssociation.isReferenceToPrimaryKey() ) {
							resultModelPart = tableGroupAssociation.getForeignKeyDescriptor().getKeyPart();
						}
						else {
							// Otherwise, we must force the use of the identifier mapping and possibly create a join,
							// because comparing by primary key is the only sensible thing to do in this case.
							// Note that EntityValuedPathInterpretation does the same
							resultModelPart = collectionPart.getAssociatedEntityMappingType().getIdentifierMapping();
						}
					}
					else if ( entityValuedModelPart instanceof AnonymousTupleEntityValuedModelPart anonymousTupleEntityValuedModelPart ) {
					resultModelPart = anonymousTupleEntityValuedModelPart.getForeignKeyPart();
				}
				else {
						// Since the table group model part is an EntityMappingType,
						// we can render the FK target model part of the inferred collection part,
						// which might be a UK, but usually a PK
						assert entityValuedModelPart instanceof EntityMappingType;
						if ( collectionPart.getCardinality() == EntityCollectionPart.Cardinality.ONE_TO_MANY ) {
							// When the inferred mapping is a one-to-many collection part,
							// we will render the entity identifier mapping for that collection part,
							// so we will have to do the same for the EntityMappingType side
							resultModelPart = collectionPart.getAssociatedEntityMappingType().getIdentifierMapping();
						}
						else {
							resultModelPart =
									manyToManyPart.getForeignKeyDescriptor()
											.getPart( manyToManyPart.getSideNature().inverse() );
						}
					}
				}
				interpretationModelPart = inferredEntityMapping;
			}
			else if ( entityValuedModelPart instanceof AnonymousTupleEntityValuedModelPart ) {
				resultModelPart = ( (AnonymousTupleEntityValuedModelPart) entityValuedModelPart ).getForeignKeyPart();
				interpretationModelPart = inferredEntityMapping;
				tableGroupToUse = null;
			}
			else if ( inferredEntityMapping instanceof EntityMappingType entityMappingType ) {
				// Render the identifier mapping if the inferred mapping is an EntityMappingType
				resultModelPart = entityMappingType.getIdentifierMapping();
				interpretationModelPart = inferredEntityMapping;
				tableGroupToUse = null;
			}
			else {
				throw new AssertionFailure( "Unexpected inferred entity mapping type" );
			}

			final EntityMappingType treatedMapping;
			if ( path instanceof SqmTreatedPath<?, ?> treatedPath
					&& treatedPath.getTreatTarget().getPersistenceType() == ENTITY ) {
				final var treatTarget = treatedPath.getTreatTarget();
				treatedMapping =
						creationContext.getMappingMetamodel()
								.findEntityDescriptor( treatTarget.getTypeName() );
			}
			else {
				treatedMapping = interpretationModelPart.getEntityMappingType();
			}

			result = EntityValuedPathInterpretation.from(
					navigablePath,
					tableGroupToUse == null ? tableGroup : tableGroupToUse,
					resultModelPart,
					interpretationModelPart,
					treatedMapping,
					this
			);
		}
		else if ( actualModelPart instanceof EmbeddableValuedModelPart mapping ) {
			result = new EmbeddableValuedPathInterpretation<>(
					mapping.toSqlExpression(
							findTableGroup( navigablePath.getParent() ),
							currentClauseStack.getCurrent(),
							this,
							getSqlAstCreationState()
					),
					navigablePath,
					mapping,
					tableGroup
			);
		}
		else {
			final var mapping = actualModelPart.asBasicValuedModelPart();
			if ( mapping != null ) {
				final var tableReference = tableGroup.resolveTableReference(
						navigablePath.append( actualModelPart.getPartName() ),
						mapping,
						mapping.getContainingTableExpression()
				);

				final var expression =
						getSqlExpressionResolver()
								.resolveSqlExpression( tableReference, mapping );
				final ColumnReference columnReference;
				if ( expression instanceof ColumnReference columnRef ) {
					columnReference = columnRef;
				}
				else if ( expression instanceof SqlSelectionExpression sqlSelectionExpression ) {
					columnReference = (ColumnReference) sqlSelectionExpression.getSelection().getExpression();
				}
				else {
					throw new UnsupportedOperationException( "Unsupported basic-valued path expression : " + expression );
				}
				result = new BasicValuedPathInterpretation<>(
						columnReference,
						navigablePath,
						mapping,
						tableGroup
				);
			}
			else if ( actualModelPart instanceof AnonymousTupleTableGroupProducer tableGroupProducer ) {
				final var subPart = tableGroupProducer.findSubPart(
						CollectionPart.Nature.ELEMENT.getName(),
						null
				);
				if ( subPart != null ) {
					return createExpression( tableGroup, navigablePath, subPart, path );
				}
				else {
					throw new SemanticException(
							"The derived SqmFrom" + ( (AnonymousTupleType<?>) path.getReferencedPathSource() ).getComponentNames() + " can not be used in a context where the expression needs to " +
									"be expanded to identifying parts, because a derived model part does not have identifying parts. " +
									"Replace uses of the root with paths instead e.g. `derivedRoot.get(\"alias1\")` or `derivedRoot.alias1`"
					);
				}
			}
			else if ( actualModelPart instanceof DiscriminatedAssociationModelPart discriminatedAssociationModelPart ) {
				result = DiscriminatedAssociationPathInterpretation.from(
						navigablePath,
						discriminatedAssociationModelPart,
						tableGroup,
						this
				);
			}
			else {
				throw new SemanticException(
						"The SqmFrom node [" + path + "] can not be used in a context where the expression needs to " +
								"be expanded to identifying parts, because the model part [" + actualModelPart +
								"] does not have identifying parts."
				);
			}
		}

		return withTreatRestriction( result, path );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath

	@Override
	public Expression visitBasicValuedPath(SqmBasicValuedSimplePath<?> sqmPath) {
		final var path = prepareReusablePath(
				sqmPath,
				() -> BasicValuedPathInterpretation.from(
						sqmPath,
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
						(ReturnableType<?>) adjustedTimestampType,
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
				result = unitConversion( scaledExpression );
			}
			else {
				// a "bare" Duration value in nanoseconds
				result = scaledExpression;
			}
		}

		return withTreatRestriction( result, sqmPath );
	}

	private Expression unitConversion(Expression scaledExpression) {
		final var durationType = (BasicValuedMapping) scaledExpression.getExpressionType();
		final var scalarType = (BasicValuedMapping) appliedByUnit.getNodeType();
		final var duration =
				durationType.getSingleJdbcMapping().getJdbcType().isInterval()
						// For interval types, we need to extract the epoch for integer arithmetic for the 'by unit' operator
						? new Duration( extractEpoch( scaledExpression ), SECOND, durationType )
						// Durations are stored as nanoseconds (see DurationJavaType)
						: new Duration( scaledExpression, NANOSECOND, durationType );
		return new Conversion( duration, appliedByUnit.getUnit().getUnit(), scalarType );
	}

	private Expression extractEpoch(Expression intervalExpression) {
		final var intType = getTypeConfiguration().getBasicTypeForJavaType( Integer.class );
		final var patternRenderer = new PatternRenderer( creationContext.getDialect().extractPattern( EPOCH ) );
		return new SelfRenderingFunctionSqlAstExpression<>(
				"extract",
				(sqlAppender, sqlAstArguments, returnType, walker) ->
						patternRenderer.render( sqlAppender, sqlAstArguments, walker ),
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
						() -> EmbeddableValuedPathInterpretation.from(
								sqmPath,
								this,
								jpaQueryComplianceEnabled
						)
				),
				sqmPath
		);
	}

	@Override
	public Expression visitAnyValuedValuedPath(SqmAnyValuedSimplePath<?> sqmPath) {
		final var currentClause = getCurrentClauseStack().getCurrent();
		return withTreatRestriction(
				currentClause == Clause.INSERT || currentClause == Clause.SET
						? DiscriminatedAssociationPathInterpretation.from( sqmPath, this )
						: prepareReusablePath( sqmPath, () -> DiscriminatedAssociationPathInterpretation.from( sqmPath, this ) ),
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
				prepareReusablePath( sqmPath, () -> EntityValuedPathInterpretation.from( sqmPath, getInferredValueMapping(), this ) ),
				sqmPath
		);
	}

	@Override
	public Expression visitAnyDiscriminatorTypeExpression(AnyDiscriminatorSqmPath<?> sqmPath) {
		return withTreatRestriction(
				prepareReusablePath(
						sqmPath,
						() -> AnyDiscriminatorPathInterpretation.from( sqmPath, this )
				),
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
	public Object visitFkExpression(SqmFkExpression<?> fkExpression) {
		final var toOnePath = fkExpression.getLhs();
		final var lhs = toOnePath.getLhs();
		prepareReusablePath( lhs, () -> null );
		final var tableGroup = getFromClauseIndex().findTableGroup( lhs.getNavigablePath() );
		final var subPart =
				tableGroup.getModelPart()
						.findSubPart( toOnePath.getReferencedPathSource().getPathName(), null );
		assert subPart instanceof ToOneAttributeMapping;

		final var toOneMapping = (ToOneAttributeMapping) subPart;
		final var fkDescriptor = toOneMapping.getForeignKeyDescriptor();
		final var tableReference = tableGroup.resolveTableReference( toOneMapping.getContainingTableExpression() );

		final var fkKeyPart = fkDescriptor.getPart( toOneMapping.getSideNature() );
		final var basicFkPart = fkKeyPart.asBasicValuedModelPart();
		if ( basicFkPart != null ) {
			return getSqlExpressionResolver().resolveSqlExpression(
					tableReference,
					basicFkPart
			);
		}
		else {
			assert fkKeyPart instanceof EmbeddableValuedModelPart;
			final var compositeFkPart = (EmbeddableValuedModelPart) fkKeyPart;
			final int count = compositeFkPart.getJdbcTypeCount();
			final ArrayList<Expression> tupleElements = new ArrayList<>( count );
			for ( int i = 0; i < count; i++ ) {
				tupleElements.add(
						getSqlExpressionResolver().resolveSqlExpression(
								tableReference,
								compositeFkPart.getSelectable( i )
						)
				);
			}
			return new SqlTuple( tupleElements, compositeFkPart );
		}
	}

	@Override
	public Object visitDiscriminatorPath(DiscriminatorSqmPath<?> sqmPath) {
		return prepareReusablePath(
				sqmPath,
				() -> {
					registerTypeUsage( sqmPath );
					return DiscriminatorPathInterpretation.from( sqmPath, this );
				}
		);
	}

	protected Expression createMinOrMaxIndexOrElement(
			AbstractSqmSpecificPluralPartPath<?> pluralPartPath,
			boolean index,
			String functionName) {
		// Try to create a lateral sub-query join if possible which allows the re-use of the expression
		return creationContext.getDialect().supportsLateral()
				? createLateralJoinExpression( pluralPartPath, index, functionName )
				: createCorrelatedAggregateSubQuery( pluralPartPath, index, functionName );
	}

	@Override
	public Expression visitElementAggregateFunction(SqmElementAggregateFunction<?> path) {
		return createMinOrMaxIndexOrElement( path, false, path.getFunctionName() );
	}

	@Override
	public Expression visitIndexAggregateFunction(SqmIndexAggregateFunction<?> path) {
		return createMinOrMaxIndexOrElement( path, true, path.getFunctionName() );
	}

	@Override
	public Expression visitFunctionPath(SqmFunctionPath<?> functionPath) {
		final var navigablePath = functionPath.getNavigablePath();
		final var tableGroup = getFromClauseAccess().findTableGroup( navigablePath );
		if ( tableGroup == null ) {
			final var functionExpression = (Expression) functionPath.getFunction().accept( this );
			final var jdbcType = functionExpression.getExpressionType().getSingleJdbcMapping().getJdbcType();
			if ( jdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
				final var embeddableFunctionTableGroup =
						new EmbeddableFunctionTableGroup(
								navigablePath,
								aggregateJdbcType.getEmbeddableMappingType(),
								functionExpression
						);
				getFromClauseAccess()
						.registerTableGroup( navigablePath, embeddableFunctionTableGroup );
				return embeddableFunctionTableGroup;
			}
			else {
				return functionExpression;
			}
		}
		else {
			return tableGroup;
		}
	}

	@Override
	public Expression visitCorrelation(SqmCorrelation<?, ?> correlation) {
		final var resolved = getFromClauseAccess().findTableGroup( correlation.getNavigablePath() );
		if ( resolved != null ) {
//			LOG.tracef( "SqmCorrelation [%s] resolved to existing TableGroup [%s]", correlation, resolved );
			return visitTableGroup( resolved, correlation );
		}
		throw new InterpretationException( "SqmCorrelation not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitTreatedPath(SqmTreatedPath<?, @Nullable ?> sqmTreatedPath) {
		prepareReusablePath( sqmTreatedPath, () -> null );
		final var resolved = getFromClauseAccess().findTableGroup( sqmTreatedPath.getNavigablePath() );
		if ( resolved != null ) {
//			LOG.tracef( "SqmTreatedPath [%s] resolved to existing TableGroup [%s]", sqmTreatedPath, resolved );
			return visitTableGroup( resolved, sqmTreatedPath );
		}
		throw new InterpretationException( "SqmTreatedPath not yet resolved to TableGroup" );
	}


	@Override
	public Expression visitPluralAttributeSizeFunction(SqmCollectionSize function) {
		final var pluralPath = function.getPluralPath();

		prepareReusablePath( pluralPath, () -> null );
		final var navigablePath = pluralPath.getNavigablePath();
		final var parentTableGroup =
				getFromClauseAccess()
						.getTableGroup( navigablePath.getParent() );
		assert parentTableGroup != null;

		final var pluralAttributeMapping =
				(PluralAttributeMapping)
						parentTableGroup.getModelPart()
								.findSubPart( navigablePath.getLocalName(), null );
		assert pluralAttributeMapping != null;

		final var subQuerySpec = new QuerySpec( false );
		pushProcessingState(
				new SqlAstQueryPartProcessingStateImpl(
						subQuerySpec,
						getCurrentProcessingState(),
						this,
						currentClauseStack::getCurrent,
						false
				)
		);
		try {
			final var tableGroup = pluralAttributeMapping.createRootTableGroup(
					true,
					navigablePath,
					null,
					null,
					() -> subQuerySpec::applyPredicate,
					this
			);

			pluralAttributeMapping.applyBaseRestrictions(
					subQuerySpec::applyPredicate,
					tableGroup,
					true,
					getLoadQueryInfluencers().getEnabledFilters(),
					false,
					null,
					this
			);

			getFromClauseAccess().registerTableGroup( navigablePath, tableGroup );
			registerPluralTableGroupParts( tableGroup );
			subQuerySpec.getFromClause().addRoot( tableGroup );

			final var functionDescriptor = resolveFunction( "count" );
			final var integerType =
					creationContext.getMappingMetamodel().getTypeConfiguration()
							.getBasicTypeForJavaType( Integer.class );
			final var expression = new SelfRenderingAggregateFunctionSqlAstExpression<>(
					functionDescriptor.getName(),
					functionDescriptor,
					singletonList( Star.INSTANCE ),
					null,
					integerType,
					integerType
			);
			subQuerySpec.getSelectClause()
					.addSqlSelection( new SqlSelectionImpl( expression ) );

			subQuerySpec.applyPredicate(
					pluralAttributeMapping.getKeyDescriptor()
							.generateJoinPredicate( parentTableGroup, tableGroup, this )
			);
		}
		finally {
			popProcessingStateStack();
		}
		return new SelectStatement( subQuerySpec );
	}

	@Override
	public Object visitIndexedPluralAccessPath(SqmIndexedCollectionAccessPath<?> path) {
		// SemanticQueryBuilder applies the index expression to the generated join
		return path.getLhs().accept( this );
	}

	@Override
	public Object visitMapEntryFunction(SqmMapEntryReference<?, ?> entryRef) {
		final var mapPath = entryRef.getMapPath();
		prepareReusablePath( mapPath, () -> null );

		final var mapNavigablePath = mapPath.getNavigablePath();
		final var tableGroup = getFromClauseAccess().resolveTableGroup(
				mapNavigablePath,
				(navigablePath) -> {
					final var parentTableGroup =
							getFromClauseAccess()
									.getTableGroup( mapNavigablePath.getParent() );
					final var mapAttribute =
							(PluralAttributeMapping)
									parentTableGroup.getModelPart()
											.findSubPart( mapNavigablePath.getLocalName(), null );
					final var tableGroupJoin = mapAttribute.createTableGroupJoin(
							mapNavigablePath,
							parentTableGroup,
							null,
							null,
							SqlAstJoinType.INNER,
							false,
							false,
							this
					);
					parentTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);

		final var mapDescriptor = (PluralAttributeMapping) tableGroup.getModelPart();

		final var indexDescriptor = mapDescriptor.getIndexDescriptor();
		final var indexNavigablePath = mapNavigablePath.append( indexDescriptor.getPartName() );
		final DomainResult<?> indexResult = indexDescriptor.createDomainResult(
				indexNavigablePath,
				tableGroup,
				null,
				this
		);
		registerProjectionUsageFromDescriptor( tableGroup, indexDescriptor );

		final var valueDescriptor = mapDescriptor.getElementDescriptor();
		final var valueNavigablePath = mapNavigablePath.append( valueDescriptor.getPartName() );
		final DomainResult<?> valueResult =
				valueDescriptor.createDomainResult( valueNavigablePath, tableGroup,
						null, this );
		registerProjectionUsageFromDescriptor( tableGroup, valueDescriptor );

		//noinspection rawtypes
		return new DomainResultProducer<Map.Entry>() {
			@Override
			public DomainResult<Map.Entry> createDomainResult(
					String resultVariable,
					DomainResultCreationState creationState) {
				return new SqmMapEntryResult<>( indexResult, valueResult, resultVariable,
						getTypeConfiguration().getJavaTypeRegistry()
								.resolveDescriptor( Map.Entry.class ) );
			}
			@Override
			public void applySqlSelections(DomainResultCreationState creationState) {
				throw new UnsupportedOperationException();
			}
		};
	}

	private void registerProjectionUsageFromDescriptor(TableGroup tableGroup, CollectionPart descriptor) {
		if ( descriptor instanceof EntityCollectionPart entityCollectionPart ) {
			final var entityMappingType = entityCollectionPart.getEntityMappingType();
			registerEntityNameUsage( tableGroup, EntityNameUse.PROJECTION, entityMappingType.getEntityName(), true );
		}
	}

	protected Expression createCorrelatedAggregateSubQuery(
			AbstractSqmSpecificPluralPartPath<?> pluralPartPath,
			boolean index,
			String function) {
		prepareReusablePath( pluralPartPath.getLhs(), () -> null );

		final var parentFromClauseAccess = getFromClauseAccess();
		final var pluralAttributeMapping =
				(PluralAttributeMapping)
						determineValueMapping( pluralPartPath.getPluralDomainPath() );

		final var subQuerySpec = new QuerySpec( false );

		pushProcessingState(
				new SqlAstQueryPartProcessingStateImpl(
						subQuerySpec,
						getCurrentProcessingState(),
						this,
						currentClauseStack::getCurrent,
						false
				)
		);
		try {
			final var tableGroup = pluralAttributeMapping.createRootTableGroup(
					true,
					pluralPartPath.getNavigablePath(),
					null,
					null,
					() -> subQuerySpec::applyPredicate,
					this
			);

			pluralAttributeMapping.applyBaseRestrictions(
					subQuerySpec::applyPredicate,
					tableGroup,
					true,
					getLoadQueryInfluencers().getEnabledFilters(),
					false,
					null,
					this
			);

			getFromClauseAccess().registerTableGroup( pluralPartPath.getNavigablePath(), tableGroup );
			registerPluralTableGroupParts( tableGroup );
			subQuerySpec.getFromClause().addRoot( tableGroup );

			final var functionDescriptor = resolveFunction( function );
			final var collectionPart = index
					? pluralAttributeMapping.getIndexDescriptor()
					: pluralAttributeMapping.getElementDescriptor();
			final ModelPart modelPart;
			if ( collectionPart instanceof OneToManyCollectionPart toManyPart ) {
				modelPart = toManyPart.getAssociatedEntityMappingType().getIdentifierMapping();
//				modelPart = pluralAttributeMapping.getKeyDescriptor().getTargetPart();
			}
			else if ( collectionPart instanceof ManyToManyCollectionPart manyToManyCollectionPart ) {
				modelPart = manyToManyCollectionPart.getKeyTargetMatchPart();
			}
			else {
				modelPart = collectionPart;
			}
			final List<Expression> arguments = new ArrayList<>( 1 );
			final var navigablePath = pluralPartPath.getNavigablePath();
			final int jdbcTypeCount = modelPart.getJdbcTypeCount();
			final List<Expression> tupleElements =
					jdbcTypeCount == 1
							? arguments
							: new ArrayList<>( jdbcTypeCount );
			modelPart.forEachSelectable(
					(selectionIndex, selectionMapping) -> tupleElements.add(
							new ColumnReference(
									tableGroup.resolveTableReference(
											navigablePath,
											(ValuedModelPart) modelPart,
											selectionMapping.getContainingTableExpression()
									),
									selectionMapping
							)
					)
			);
			if ( jdbcTypeCount != 1 ) {
				arguments.add( new SqlTuple( tupleElements, modelPart ) );
			}
			final var expression = new SelfRenderingAggregateFunctionSqlAstExpression<>(
					functionDescriptor.getName(),
					functionDescriptor,
					arguments,
					null,
					(ReturnableType<?>)
							functionDescriptor.getReturnTypeResolver()
									.resolveFunctionReturnType( () -> null, arguments )
									.getJdbcMapping(),
					modelPart
			);
			subQuerySpec.getSelectClause()
					.addSqlSelection( new SqlSelectionImpl( expression ) );

			final var parent = pluralPartPath.getPluralDomainPath().getNavigablePath().getParent();
			subQuerySpec.applyPredicate(
					pluralAttributeMapping.getKeyDescriptor().generateJoinPredicate(
							parentFromClauseAccess.findTableGroup( parent ),
							tableGroup,
							this
					)
			);
		}
		finally {
			popProcessingStateStack();
		}
		return new SelectStatement( subQuerySpec );
	}

	private AbstractSqmSelfRenderingFunctionDescriptor resolveFunction(String function) {
		return (AbstractSqmSelfRenderingFunctionDescriptor)
				creationContext.getSqmFunctionRegistry()
						.findFunctionDescriptor( function );
	}

	protected Expression createLateralJoinExpression(
			AbstractSqmSpecificPluralPartPath<?> pluralPartPath,
			boolean index,
			String functionName) {
		prepareReusablePath( pluralPartPath.getLhs(), () -> null );

		final var pluralAttributeMapping =
				(PluralAttributeMapping)
						determineValueMapping( pluralPartPath.getPluralDomainPath() );
		final var parentFromClauseAccess = getFromClauseAccess();
		final var parentTableGroup = parentFromClauseAccess.findTableGroup(
				pluralPartPath.getNavigablePath().getParent()
		);
		final var collectionPart = index
				? pluralAttributeMapping.getIndexDescriptor()
				: pluralAttributeMapping.getElementDescriptor();
		final ModelPart modelPart;
		if ( collectionPart instanceof OneToManyCollectionPart toManyPart ) {
			modelPart = toManyPart.getAssociatedEntityMappingType().getIdentifierMapping();
//				modelPart = pluralAttributeMapping.getKeyDescriptor().getTargetPart();
		}
		else if ( collectionPart instanceof ManyToManyCollectionPart manyToManyCollectionPart ) {
			modelPart = manyToManyCollectionPart.getKeyTargetMatchPart();
		}
		else {
			modelPart = collectionPart;
		}
		final int jdbcTypeCount = modelPart.getJdbcTypeCount();
		final String pathName = functionName + ( index ? "_index" : "_element" );
		final String identifierVariable =
				parentTableGroup.getPrimaryTableReference().getIdentificationVariable()
						+ "_" + pathName;
		final var queryPath =
				new NavigablePath( parentTableGroup.getNavigablePath(),
						pathName, identifierVariable );
		TableGroup lateralTableGroup = parentFromClauseAccess.findTableGroup( queryPath );
		if ( lateralTableGroup == null ) {
			final var subQuerySpec = new QuerySpec( false );
			pushProcessingState(
					new SqlAstQueryPartProcessingStateImpl(
							subQuerySpec,
							getCurrentProcessingState(),
							this,
							currentClauseStack::getCurrent,
							false
					)
			);
			try {
				final var tableGroup = pluralAttributeMapping.createRootTableGroup(
						true,
						pluralPartPath.getNavigablePath(),
						null,
						null,
						() -> subQuerySpec::applyPredicate,
						this
				);

				pluralAttributeMapping.applyBaseRestrictions(
						subQuerySpec::applyPredicate,
						tableGroup,
						true,
						getLoadQueryInfluencers().getEnabledFilters(),
						false,
						null,
						this
				);

				getFromClauseAccess().registerTableGroup( pluralPartPath.getNavigablePath(), tableGroup );
				registerPluralTableGroupParts( tableGroup );
				subQuerySpec.getFromClause().addRoot( tableGroup );

				final List<String> columnNames = new ArrayList<>( jdbcTypeCount );
				final List<ColumnReference> resultColumnReferences = new ArrayList<>( jdbcTypeCount );
				final var navigablePath = pluralPartPath.getNavigablePath();
				final var max = switch ( functionName.toLowerCase( Locale.ROOT ) ) {
					case "max" -> true;
					case "min" -> false;
					default -> null;
				};
				final var functionDescriptor = resolveFunction( functionName );
				final List<ColumnReference> subQueryColumns = new ArrayList<>( jdbcTypeCount );
				modelPart.forEachSelectable(
						(selectionIndex, selectionMapping) -> {
							final var columnReference = new ColumnReference(
									tableGroup.resolveTableReference(
											navigablePath,
											(ValuedModelPart) modelPart,
											selectionMapping.getContainingTableExpression()
									),
									selectionMapping
							);
							final String columnName =
									selectionMapping.isFormula()
											? "col" + columnNames.size()
											: selectionMapping.getSelectionExpression();
							columnNames.add( columnName );
							subQueryColumns.add( columnReference );
							if ( max != null ) {
								subQuerySpec.addSortSpecification(
										new SortSpecification(
												columnReference,
												max ? SortDirection.DESCENDING : SortDirection.ASCENDING
										)
								);
							}
						}
				);

				if ( max != null ) {
					for ( int i = 0; i < subQueryColumns.size(); i++ ) {
						subQuerySpec.getSelectClause().addSqlSelection(
								new SqlSelectionImpl( i, subQueryColumns.get( i ) )
						);
						resultColumnReferences.add(
								new ColumnReference(
										identifierVariable,
										columnNames.get( i ),
										false,
										null,
										subQueryColumns.get( i ).getJdbcMapping()
								)
						);
					}
					subQuerySpec.setFetchClauseExpression(
							new QueryLiteral<>( 1, basicType( Integer.class ) ),
							FetchClauseType.ROWS_ONLY
					);
				}
				else {
					final var arguments =
							jdbcTypeCount == 1
									? subQueryColumns
									: singletonList( new SqlTuple( subQueryColumns, modelPart ) );
					final var expression = new SelfRenderingAggregateFunctionSqlAstExpression<>(
							functionDescriptor.getName(),
							functionDescriptor,
							arguments,
							null,
							(ReturnableType<?>)
									functionDescriptor.getReturnTypeResolver()
											.resolveFunctionReturnType( () -> null, arguments )
											.getJdbcMapping(),
							modelPart
					);

					subQuerySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( expression ) );
					resultColumnReferences.add(
							new ColumnReference(
									identifierVariable,
									columnNames.get( 0 ),
									false,
									null,
									expression.getExpressionType().getSingleJdbcMapping()
							)
					);
				}
				subQuerySpec.applyPredicate(
						pluralAttributeMapping.getKeyDescriptor().generateJoinPredicate(
								parentFromClauseAccess.findTableGroup(
										pluralPartPath.getPluralDomainPath().getNavigablePath().getParent()
								),
								tableGroup,
								this
						)
				);
				lateralTableGroup = new QueryPartTableGroup(
						queryPath,
						null,
						new SelectStatement( subQuerySpec ),
						identifierVariable,
						columnNames,
						getCompatibleTableExpressions( modelPart ),
						true,
						false,
						creationContext.getSessionFactory()
				);
				if ( currentlyProcessingJoin == null ) {
					parentTableGroup.addTableGroupJoin(
							new TableGroupJoin(
									lateralTableGroup.getNavigablePath(),
									SqlAstJoinType.LEFT,
									lateralTableGroup
							)
					);
				}
				else {
					// In case this is used in the ON condition, we must prepend this lateral join
					final TableGroup targetTableGroup;
					if ( currentlyProcessingJoin.getLhs() == null ) {
						targetTableGroup = parentFromClauseAccess.getTableGroup(
								currentlyProcessingJoin.findRoot().getNavigablePath()
						);
					}
					else {
						targetTableGroup = parentFromClauseAccess.getTableGroup(
								currentlyProcessingJoin.getLhs().getNavigablePath()
						);
					}
					// Many databases would support modelling this as nested table group join,
					// but at least SQL Server doesn't like that, saying that the correlated columns can't be "bound"
					// Since there is no dependency on the currentlyProcessingJoin, we can safely prepend this join
					targetTableGroup.prependTableGroupJoin(
							currentlyProcessingJoin.getNavigablePath(),
							new TableGroupJoin(
									lateralTableGroup.getNavigablePath(),
									SqlAstJoinType.LEFT,
									lateralTableGroup
							)
					);
				}
				parentFromClauseAccess.registerTableGroup( lateralTableGroup.getNavigablePath(), lateralTableGroup );
				if ( jdbcTypeCount == 1 ) {
					final var resulltColumnReference = resultColumnReferences.get( 0 );
					return new SelfRenderingFunctionSqlAstExpression<>(
							pathName,
							(sqlAppender, sqlAstArguments, returnType, walker)
									-> sqlAstArguments.get( 0 ).accept( walker ),
							resultColumnReferences,
							(ReturnableType<?>) resulltColumnReference.getJdbcMapping(),
							resulltColumnReference.getJdbcMapping()
					);
				}
				else {
					return new SqlTuple( resultColumnReferences, modelPart );
				}
			}
			finally {
				popProcessingStateStack();
			}
		}
		final var tableReference =
				(QueryPartTableReference)
						lateralTableGroup.getPrimaryTableReference();
		if ( jdbcTypeCount == 1 ) {
			final var sqlSelections =
					tableReference.getQueryPart().getFirstQuerySpec()
							.getSelectClause().getSqlSelections();
			final var selectionJdbcMapping =
					sqlSelections.get( 0 ).getExpressionType()
							.getSingleJdbcMapping();
			return new SelfRenderingFunctionSqlAstExpression<>(
					pathName,
					(sqlAppender, sqlAstArguments, returnType, walker)
							-> sqlAstArguments.get( 0 ).accept( walker ),
					singletonList(
							new ColumnReference(
									identifierVariable,
									tableReference.getColumnNames().get( 0 ),
									false,
									null,
									selectionJdbcMapping
							)
					),
					(ReturnableType<?>) selectionJdbcMapping,
					sqlSelections.get( 0 ).getExpressionType()
			);
		}
		else {
			final List<ColumnReference> resultColumnReferences = new ArrayList<>( jdbcTypeCount );
			modelPart.forEachSelectable(
					(selectionIndex, selectionMapping) -> resultColumnReferences.add(
							new ColumnReference(
									identifierVariable,
									tableReference.getColumnNames().get( selectionIndex ),
									false,
									null,
									selectionMapping.getJdbcMapping()
							)
					)
			);
			return new SqlTuple( resultColumnReferences, modelPart );
		}
	}

	private static Set<String> getCompatibleTableExpressions(ModelPart modelPart) {
		final var basicPart = modelPart.asBasicValuedModelPart();
		if ( basicPart != null ) {
			return singleton( basicPart.getContainingTableExpression() );
		}
		else if ( modelPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
			return singleton( embeddableValuedModelPart.getContainingTableExpression() );
		}
		else {
			return emptySet();
		}
	}

	private Expression withTreatRestriction(Expression expression, SqmPath<?> path) {
		final var lhs = path instanceof SqmTreatedPath<?, ?> ? path : path.getLhs();
		if ( lhs instanceof SqmTreatedPath<?, ?> treatedPath ) {
			final var treatTarget = treatedPath.getTreatTarget();
			final var treatTargetJavaType = treatTarget.getJavaType();
			final var wrappedPath = treatedPath.getWrappedPath();
			final var originalJavaType = wrappedPath.getJavaType();
			if ( treatTargetJavaType.isAssignableFrom( originalJavaType ) ) {
				// Treating a node to a super type can be ignored
				return expression;
			}
			if ( treatTarget instanceof EmbeddableDomainType<?> ) {
				// For embedded treats we simply register a TREAT use
				final var tableGroup = getFromClauseIndex().findTableGroup( wrappedPath.getNavigablePath() );
				registerEntityNameUsage( tableGroup, EntityNameUse.TREAT, treatTarget.getTypeName(), false );
				return expression;
			}
			if ( !( expression.getExpressionType() instanceof BasicValuedMapping ) ) {
				// A case wrapper for non-basic paths is not possible,
				// because a case expression must return a scalar value.
				if ( lhs instanceof SqmRoot ) {
					// For treated roots we need to add the type restriction predicate as conjunct
					// by registering the treat into tableGroupEntityNameUses.
					// Joins don't need the restriction as it will be embedded into
					// the joined table group itself by #pruneTableGroupJoins
					final var tableGroup = getFromClauseIndex().findTableGroup( wrappedPath.getNavigablePath() );
					registerEntityNameUsage( tableGroup, EntityNameUse.TREAT, treatTarget.getTypeName(), false );
				}
				return expression;
			}
			final var basicPath = (BasicValuedPathInterpretation<?>) expression;
			final var tableGroup = basicPath.getTableGroup();
			final var elementTableGroup =
					tableGroup instanceof PluralTableGroup pluralTableGroup
							? pluralTableGroup.getElementTableGroup()
							: tableGroup;
			final var persister = (EntityPersister) elementTableGroup.getModelPart().getPartMappingType();
			// Only need a case expression around the basic valued path for the parent treat expression
			// if the column of the basic valued path is shared between subclasses
			if ( persister.isSharedColumn( basicPath.getColumnReference().getColumnExpression() ) ) {
				return createCaseExpression( wrappedPath, (EntityDomainType<?>) treatTarget, expression );
			}
		}
		return expression;
	}

	private Expression createCaseExpression(SqmPath<?> lhs, EntityDomainType<?> treatTarget, Expression expression) {
		final var treatTypeRestriction = createTreatTypeRestriction( lhs, treatTarget );
		if ( treatTypeRestriction == null ) {
			return expression;
		}
		else {
			final var mappingModelExpressible = (BasicValuedMapping) expression.getExpressionType();
			final List<CaseSearchedExpression.WhenFragment> whenFragments = new ArrayList<>( 1 );
			whenFragments.add(
					new CaseSearchedExpression.WhenFragment(
							treatTypeRestriction,
							expression
					)
			);
			return new CaseSearchedExpression(
					mappingModelExpressible,
					whenFragments,
					null
			);
		}
	}

	private Predicate consumeConjunctTreatTypeRestrictions() {
		return consumeConjunctTreatTypeRestrictions( tableGroupEntityNameUses );
	}

	private Predicate consumeConjunctTreatTypeRestrictions(Map<TableGroup, Map<String, EntityNameUse>> conjunctTreatUsages) {
		if ( conjunctTreatUsages == null || conjunctTreatUsages.isEmpty() ) {
			return null;
		}
		Predicate predicate = null;
		for ( var entry : conjunctTreatUsages.entrySet() ) {
			final var tableGroup = entry.getKey();
			final var modelPart = tableGroup.getModelPart();

			final Set<String> typeNames;
			final EntityMappingType entityMapping;
			final EmbeddableMappingType embeddableMapping;
			if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
				entityMapping = (EntityMappingType) pluralAttributeMapping.getElementDescriptor().getPartMappingType();
				embeddableMapping = null;
			}
			else if ( modelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
				entityMapping = entityValuedModelPart.getEntityMappingType();
				embeddableMapping = null;
			}
			else if ( modelPart instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
				embeddableMapping = embeddableValuedModelPart.getEmbeddableTypeDescriptor();
				entityMapping = null;
			}
			else {
				throw new IllegalStateException( "Unrecognized model part for treated table group: " + tableGroup );
			}

			final DiscriminatorPathInterpretation<?> typeExpression;
			final var usage = entry.getValue();
			if ( entityMapping != null ) {
				typeNames = determineEntityNamesForTreatTypeRestriction( entityMapping, usage );
				if ( typeNames.isEmpty() ) {
					continue;
				}
				typeExpression = new DiscriminatorPathInterpretation<>(
						tableGroup.getNavigablePath().append( DISCRIMINATOR_ROLE_NAME ),
						entityMapping,
						tableGroup,
						this
				);
				registerTypeUsage( tableGroup );
			}
			else {
				assert embeddableMapping != null;
				typeNames = determineEmbeddableNamesForTreatTypeRestriction( embeddableMapping, usage );
				if ( typeNames.isEmpty() ) {
					continue;
				}
				typeExpression = new DiscriminatorPathInterpretation<>(
						tableGroup.getNavigablePath().append( DISCRIMINATOR_ROLE_NAME ),
						embeddableMapping.getDiscriminatorMapping(),
						tableGroup,
						this
				);
			}

			// We need to check if this is a treated left or full join, which case we should
			// allow null discriminator values to maintain correct semantics
			final var join = getParentTableGroupJoin( tableGroup );
			final boolean allowNulls = join != null
					&& ( join.getJoinType() == SqlAstJoinType.LEFT
							|| join.getJoinType() == SqlAstJoinType.FULL );
			predicate = combinePredicates(
					predicate,
					createTreatTypeRestriction(
							typeExpression,
							typeNames,
							allowNulls,
							entityMapping != null
					)
			);
		}

		return predicate;
	}

	private TableGroupJoin getParentTableGroupJoin(TableGroup tableGroup) {
		final var parentNavigablePath = tableGroup.getNavigablePath().getParent();
		if ( parentNavigablePath != null ) {
			final var parentTableGroup = getFromClauseIndex().findTableGroup( parentNavigablePath );
			if ( parentTableGroup instanceof PluralTableGroup ) {
				return getParentTableGroupJoin( parentTableGroup );
			}
			else if ( parentTableGroup != null ) {
				return parentTableGroup.findTableGroupJoin( tableGroup );
			}
		}
		return null;
	}

	private Set<String> determineEntityNamesForTreatTypeRestriction(
			EntityMappingType partMappingType,
			Map<String, EntityNameUse> entityNameUses) {
		final Set<String> entityNameUsesSet = new HashSet<>( entityNameUses.size() );
		for ( var entry : entityNameUses.entrySet() ) {
			if ( entry.getValue() != EntityNameUse.PROJECTION ) {
				entityNameUsesSet.add( entry.getKey() );
			}
		}

		if ( entityNameUsesSet.containsAll( partMappingType.getSubclassEntityNames() ) ) {
			// No need to create a restriction if all subclasses are used
			return emptySet();
		}
		if ( entityNameUses.containsValue( EntityNameUse.FILTER ) ) {
			// If the conjunct contains FILTER uses we can omit the treat type restriction
			return emptySet();
		}
		final String partEntityName = partMappingType.getEntityName();
		final String baseEntityNameToAdd;
		if ( entityNameUsesSet.contains( partEntityName ) ) {
			baseEntityNameToAdd = partMappingType.isAbstract() ? null : partEntityName;
			if ( entityNameUses.size() == 1 ) {
				return emptySet();
			}
		}
		else {
			baseEntityNameToAdd = null;
		}
		final Set<String> entityNames = new HashSet<>( entityNameUsesSet.size() );
		for ( var entityNameUse : entityNameUses.entrySet() ) {
			if ( entityNameUse.getValue() == EntityNameUse.TREAT ) {
				final String entityName = entityNameUse.getKey();
				final var entityDescriptor =
						creationContext.getMappingMetamodel()
								.findEntityDescriptor( entityName );
				if ( !entityDescriptor.isAbstract() ) {
					entityNames.add( entityDescriptor.getEntityName() );
				}
				for ( var subMappingType : entityDescriptor.getSubMappingTypes() ) {
					if ( !subMappingType.isAbstract() ) {
						entityNames.add( subMappingType.getEntityName() );
					}
				}
			}
		}
		do {
			entityNames.remove( partEntityName );
			partMappingType = partMappingType.getSuperMappingType();
		} while ( partMappingType != null );
		if ( !entityNames.isEmpty() && baseEntityNameToAdd != null ) {
			entityNames.add( baseEntityNameToAdd );
		}
		return entityNames;
	}

	private Predicate createDiscriminatedAssociationTreatTypeRestriction(
			TableGroup tableGroup,
			ModelPart modelPart,
			EntityDomainType<?> treatTarget) {
		final var associationModelPart = resolveDiscriminatedAssociationModelPart( modelPart );
		if ( associationModelPart == null ) {
			return null;
		}

		final var entityDescriptor =
				domainModel.findEntityDescriptor( treatTarget.getHibernateEntityName() );
		final var entityNames =
				entityDescriptor.isPolymorphic()
						? entityDescriptor.getSubclassEntityNames()
						: Set.of( entityDescriptor.getEntityName() );
		final var discriminatorMapping = associationModelPart.getDiscriminatorMapping();
		final var tableReference =
				tableGroup.resolveTableReference( null,
						discriminatorMapping.getContainingTableExpression() );
		final var discriminatorColumn = new ColumnReference( tableReference, discriminatorMapping );
		if ( entityNames.size() == 1 ) {
			return new ComparisonPredicate(
					discriminatorColumn,
					ComparisonOperator.EQUAL,
					discriminatorValueLiteral( discriminatorMapping,
							entityNames.iterator().next() )
			);
		}
		else {
			final List<Expression> typeLiterals = new ArrayList<>( entityNames.size() );
			for ( String entityName : entityNames ) {
				typeLiterals.add( discriminatorValueLiteral( discriminatorMapping, entityName ) );
			}
			return new InListPredicate( discriminatorColumn, typeLiterals );
		}
	}

	private static @NonNull QueryLiteral<Object> discriminatorValueLiteral(
			DiscriminatorMapping discriminatorMapping, String entityName) {
		return new QueryLiteral<>(
				discriminatorMapping.getValueConverter()
						.getDetailsForEntityName( entityName ).getValue(),
				discriminatorMapping
		);
	}

	private DiscriminatedAssociationModelPart resolveDiscriminatedAssociationModelPart(ModelPart modelPart) {
		if ( modelPart instanceof DiscriminatedAssociationModelPart associationModelPart ) {
			return associationModelPart;
		}
		else if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping
				&& pluralAttributeMapping.getElementDescriptor()
						instanceof DiscriminatedAssociationModelPart associationModelPart ) {
			return associationModelPart;
		}
		else {
			return null;
		}
	}

	private Set<String> determineEmbeddableNamesForTreatTypeRestriction(
			EmbeddableMappingType embeddableMappingType,
			Map<String, EntityNameUse> entityNameUses) {
		final var embeddableDomainType =
				creationContext.getJpaMetamodel()
						.embeddable( embeddableMappingType.getJavaType().getJavaTypeClass() );
		final Set<String> entityNameUsesSet = new HashSet<>( entityNameUses.keySet() );
		ManagedDomainType<?> superType = embeddableDomainType;
		while ( superType != null ) {
			entityNameUsesSet.remove( superType.getTypeName() );
			superType = superType.getSuperType();
		}
		return entityNameUsesSet;
	}

	private Predicate createTreatTypeRestriction(SqmPath<?> lhs, EntityDomainType<?> treatTarget) {
		final var entityDescriptor =
				domainModel.findEntityDescriptor( treatTarget.getHibernateEntityName() );
		final var nodeType = lhs.getNodeType();
		final boolean polymorphic = entityDescriptor.isPolymorphic();
		if ( nodeType instanceof AnyMappingDomainType<?>
				&& SqmExpressionHelper.get( lhs, DISCRIMINATOR_ROLE_NAME )
						instanceof AnyDiscriminatorSqmPath<?> ) {
			return createTreatTypeRestriction(
					lhs,
					polymorphic
							? entityDescriptor.getSubclassEntityNames()
							: Set.of( entityDescriptor.getEntityName() )
			);
		}
		else if ( polymorphic && nodeType != treatTarget ) {
			return createTreatTypeRestriction( lhs,
					entityDescriptor.getSubclassEntityNames() );
		}
		else {
			return null;
		}
	}

	private Predicate createTreatTypeRestriction(SqmPath<?> lhs, Set<String> subclassEntityNames) {
		// Do what visitSelfInterpretingSqmPath does, except for calling preparingReusablePath
		// as that would register a type usage for the table group that we don't want here
		final var discriminatorSqmPath =
				(DiscriminatorSqmPath<?>)
						SqmExpressionHelper.get( lhs, DISCRIMINATOR_ROLE_NAME );
		registerTypeUsage( discriminatorSqmPath );
		return createTreatTypeRestriction(
				discriminatorSqmPath instanceof AnyDiscriminatorSqmPath<?> anyDiscriminatorSqmPath
						? AnyDiscriminatorPathInterpretation.from( anyDiscriminatorSqmPath, this )
						: DiscriminatorPathInterpretation.from( discriminatorSqmPath, this ),
				subclassEntityNames,
				false,
				true
		);
	}

	private Predicate createTreatTypeRestriction(
			SqmPathInterpretation<?> typeExpression,
			Set<String> subtypeNames,
			boolean allowNulls,
			boolean entity) {
		final Predicate discriminatorPredicate;
		if ( subtypeNames.size() == 1 ) {
			discriminatorPredicate = new ComparisonPredicate(
					typeExpression,
					ComparisonOperator.EQUAL,
					getTypeLiteral( typeExpression, subtypeNames.iterator().next(), entity )
			);
		}
		else {
			final List<Expression> typeLiterals = new ArrayList<>( subtypeNames.size() );
			for ( String subtypeName : subtypeNames ) {
				typeLiterals.add( getTypeLiteral( typeExpression, subtypeName, entity ) );
			}
			discriminatorPredicate = new InListPredicate( typeExpression, typeLiterals );
		}
		if ( allowNulls ) {
			return new Junction(
					Junction.Nature.DISJUNCTION,
					List.of( discriminatorPredicate, new NullnessPredicate( typeExpression ) ),
					getBooleanType()
			);
		}
		return discriminatorPredicate;
	}

	private Expression getTypeLiteral(SqmPathInterpretation<?> typeExpression, String typeName, boolean entity) {
		if ( entity ) {
			if ( typeExpression instanceof AnyDiscriminatorPathInterpretation<?> ) {
				final var discriminatorMapping = (DiscriminatorMapping) typeExpression.getExpressionType();
				return new QueryLiteral<>(
						discriminatorMapping.getValueConverter()
								.getDetailsForEntityName( typeName )
								.getValue(),
						discriminatorMapping
				);
			}
			else {
				return new EntityTypeLiteral( domainModel.findEntityDescriptor( typeName ) );
			}
		}
		else {
			return new EmbeddableTypeLiteral(
					creationContext.getJpaMetamodel().embeddable( typeName ),
					(BasicType<?>)
							typeExpression.getExpressionType()
									.getSingleJdbcMapping()
			);
		}
	}

	private MappingModelExpressible<?> resolveInferredType() {
		final var inferableTypeAccess = inferrableTypeAccessStack.getCurrent();
		if ( inTypeInference || inferableTypeAccess == null ) {
			return null;
		}
		else {
			inTypeInference = true;
			final var inferredType = inferableTypeAccess.get();
			inTypeInference = false;
			return inferredType;
		}
	}

	@Override
	public boolean isInTypeInference() {
		return inImpliedResultTypeInference || inTypeInference;
	}

	@Override
	public MappingModelExpressible<?> resolveFunctionImpliedReturnType() {
		if ( inImpliedResultTypeInference || inTypeInference
				|| functionImpliedResultTypeAccess == null ) {
			return null;
		}
		else {
			inImpliedResultTypeInference = true;
			final var inferredType = functionImpliedResultTypeAccess.get();
			inImpliedResultTypeInference = false;
			return inferredType;
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// General expressions

	@Override
	public Expression visitLiteral(SqmLiteral<?> literal) {
		if ( literal instanceof SqmLiteralNull<?> sqmLiteralNull ) {
			return nullLiteral( sqmLiteralNull );
		}
		else {
			final var inferableExpressible = getInferredValueMapping();
			if ( inferableExpressible instanceof DiscriminatorMapping discriminatorMapping ) {
				return entityTypeLiteral( literal, discriminatorMapping );
			}
			else if ( inferableExpressible instanceof BasicValuedMapping basicValuedMapping ) {
				@SuppressWarnings("rawtypes")
				final BasicValueConverter valueConverter = basicValuedMapping.getJdbcMapping().getValueConverter();
				if ( valueConverter != null ) {
					return queryLiteral( literal, basicValuedMapping, valueConverter );
				}
			}

			final var expressible = literalExpressible( literal, inferableExpressible );
			if ( expressible instanceof EntityIdentifierMapping identifierMapping
					//TODO: remove test against impl class EntityTypeImpl
					&& literal.getNodeType() instanceof EntityTypeImpl ) {
				return new QueryLiteral<>(
						identifierMapping.getIdentifier( literal.getLiteralValue() ),
						(BasicValuedMapping) expressible
				);
			}
			else if ( expressible instanceof BasicValuedMapping mapping ) {
				return new QueryLiteral<>( literal.getLiteralValue(), mapping );
			}
			// Handling other values might seem unnecessary, but with JPA Criteria it is totally possible to have such literals
			else if ( expressible instanceof EmbeddableValuedModelPart ) {
				return sqlTuple( expressible, literal.getLiteralValue() );
			}
			else if ( expressible instanceof EntityValuedModelPart entityValuedModelPart ) {
				return associationLiteral( literal, entityValuedModelPart );
			}
			else {
				final var nodeType = (BasicSqmPathSource<?>) literal.getNodeType();
				return new QueryLiteral<>(
						literal.getLiteralValue(),
						creationContext.getTypeConfiguration().getBasicTypeRegistry()
								.getRegisteredType( nodeType.getPathType().getTypeName() )
				);
			}
		}
	}

	private MappingModelExpressible<?> literalExpressible
			(SqmLiteral<?> literal, MappingModelExpressible<?> inferableExpressible) {
		final var localExpressible =
				resolveMappingModelExpressible( literal,
						creationContext.getMappingMetamodel(),
						getFromClauseAccess() == null ? null : getFromClauseAccess()::findTableGroup );
		if ( localExpressible == null ) {
			return getElementExpressible( inferableExpressible );
		}
		else {
			final var elementExpressible = getElementExpressible( localExpressible );
			return elementExpressible instanceof BasicType basicType
					? resolveSqlTypeIndicators( this, basicType, literal.getJavaTypeDescriptor() )
					: elementExpressible;
		}
	}

	private Expression nullLiteral(SqmLiteralNull<?> literal) {
		final var inferredType = resolveInferredType();
		final var mappingModelExpressible =
				inferredType == null
						? determineCurrentExpressible( literal )
						: inferredType;
		if ( mappingModelExpressible instanceof BasicValuedMapping basicValuedMapping ) {
			return new QueryLiteral<>( null, basicValuedMapping );
		}
		else {
			// When selecting a literal null entity type, we should simply return a single null object
			return nullLiteral( mappingModelExpressible instanceof EntityMappingType ? null : mappingModelExpressible );
		}
	}

	private Expression nullLiteral(MappingModelExpressible<?> mappingModelExpressible) {
		final var keyExpressible = getKeyExpressible( mappingModelExpressible );
		if ( keyExpressible == null ) {
			// treat Void as the bottom type, the class of null
			return new QueryLiteral<>( null, BottomType.INSTANCE );
		}
		else {
			final List<Expression> expressions = new ArrayList<>( keyExpressible.getJdbcTypeCount() );
			if ( keyExpressible instanceof ModelPart modelPart ) {
				// Use the selectable mappings as BasicValuedMapping if possible to retain column definition info for possible casts
				modelPart.forEachSelectable(
						(index, selectableMapping) -> expressions.add(
								new QueryLiteral<>(
										null,
										selectableMapping instanceof BasicValuedMapping
												? (BasicValuedMapping) selectableMapping
												: (BasicValuedMapping) selectableMapping.getJdbcMapping()
								)
						)
				);
			}
			else {
				keyExpressible.forEachJdbcType(
						(index, jdbcMapping) -> expressions.add(
								new QueryLiteral<>( null, (BasicValuedMapping) jdbcMapping )
						)
				);
			}
			return new SqlTuple( expressions, mappingModelExpressible );
		}
	}

	private static Expression associationLiteral(SqmLiteral<?> literal, EntityValuedModelPart entityValuedModelPart) {
		final Object associationKey;
		final ModelPart associationKeyPart;
		if ( entityValuedModelPart instanceof EntityAssociationMapping association ) {
			final var foreignKeyDescriptor = association.getForeignKeyDescriptor();
			if ( association.getSideNature() == ForeignKeyDescriptor.Nature.TARGET ) {
				// If the association is the target, we must use the identifier of the EntityMappingType
				final var associatedEntityMappingType = association.getAssociatedEntityMappingType();
				associationKey =
						associatedEntityMappingType.getIdentifierMapping()
								.getIdentifier( literal.getLiteralValue() );
				associationKeyPart = associatedEntityMappingType.getIdentifierMapping();
			}
			else {
				associationKey = foreignKeyDescriptor.getAssociationKeyFromSide(
						literal.getLiteralValue(),
						association.getSideNature().inverse(),
						null
				);
				associationKeyPart = foreignKeyDescriptor.getPart( association.getSideNature() );
			}
		}
		else {
			final var identifierMapping =
					entityValuedModelPart.getEntityMappingType().getIdentifierMapping();
			associationKeyPart = identifierMapping;
			associationKey = identifierMapping.getIdentifier( literal.getLiteralValue() );
		}

		return associationKeyPart instanceof BasicValuedMapping basicValuedMapping
				? new QueryLiteral<>( associationKey, basicValuedMapping )
				: sqlTuple( associationKeyPart, associationKey );
	}

	private static SqlTuple sqlTuple(MappingModelExpressible<?> associationKeyPart, Object associationKey) {
		final List<Expression> list = new ArrayList<>( associationKeyPart.getJdbcTypeCount() );
		associationKeyPart.forEachJdbcValue(
				associationKey,
				list,
				null,
				(selectionIndex, expressions, noop, value, jdbcMapping)
						-> expressions.add( new QueryLiteral<>( value, (BasicValuedMapping) jdbcMapping ) ),
				null
		);
		return new SqlTuple( list, associationKeyPart );
	}

	private QueryLiteral<Object> queryLiteral(
			SqmLiteral<?> literal, BasicValuedMapping basicValuedMapping, BasicValueConverter valueConverter) {
		return new QueryLiteral<>( sqlLiteralValue( valueConverter, literal.getLiteralValue() ), basicValuedMapping );
	}

	private <D> Object sqlLiteralValue(BasicValueConverter<D,?> valueConverter, D value) {
		// For converted query literals, we support both, the domain and relational java type
		if ( value == null || valueConverter.getDomainJavaType().isInstance( value ) ) {
			return valueConverter.toRelationalValue( value );
		}
		else if ( valueConverter.getRelationalJavaType().isInstance( value ) ) {
			return value;
		}
		else if ( Character.class.isAssignableFrom( valueConverter.getRelationalJavaType().getJavaTypeClass() )
				&& value instanceof CharSequence charSequence && charSequence.length() == 1 ) {
			return charSequence.charAt( 0 );
		}
		// In HQL, number literals might not match the relational java type exactly,
		// so we allow coercion between the number types
		else if ( Number.class.isAssignableFrom( valueConverter.getRelationalJavaType().getJavaTypeClass() )
				&& value instanceof Number ) {
			return valueConverter.getRelationalJavaType().coerce( value );
		}
		else {
			throw new SemanticException(
					String.format(
							Locale.ROOT,
							"Literal type '%s' did not match domain type '%s' nor converted type '%s'",
							value.getClass(),
							valueConverter.getDomainJavaType().getJavaTypeClass().getName(),
							valueConverter.getRelationalJavaType().getJavaTypeClass().getName()
					)
			);
		}
	}

	private <E> EntityTypeLiteral entityTypeLiteral(SqmLiteral<E> literal, DiscriminatorMapping inferableExpressible) {
		final E literalValue = literal.getLiteralValue();
		final EntityPersister entityDescriptor;
		if ( literalValue instanceof Class<?> clazz ) {
			entityDescriptor = creationContext.getMappingMetamodel().findEntityDescriptor( clazz );
		}
		else {
			@SuppressWarnings("unchecked")
			final var valueConverter =
					(DiscriminatorConverter<?, E>)
							inferableExpressible.getValueConverter();
			entityDescriptor =
					discriminatorValueDetails( valueConverter, literalValue )
							.getIndicatedEntity().getEntityPersister();
		}
		return new EntityTypeLiteral( entityDescriptor );
	}

	private <E> DiscriminatorValueDetails discriminatorValueDetails(DiscriminatorConverter<?, E> valueConverter, E literalValue) {
		if ( valueConverter.getDomainJavaType().isInstance( literalValue ) ) {
			return valueConverter.getDetailsForDiscriminatorValue( literalValue );
		}
		else if ( valueConverter.getRelationalJavaType().isInstance( literalValue ) ) {
			return valueConverter.getDetailsForRelationalForm( literalValue );
		}
		else {
			// Special case when passing the discriminator value as e.g. string literal,
			// but the expected relational type is Character.
			// In this case, we use wrap to transform the value to the correct type
			final E relationalForm =
					valueConverter.getRelationalJavaType()
							.wrap( literalValue, creationContext.getWrapperOptions() );
			return valueConverter.getDetailsForRelationalForm( relationalForm );
		}
	}

	@Override
	public <N extends Number> Expression visitHqlNumericLiteral(SqmHqlNumericLiteral<N> numericLiteral) {
		final var inferredExpressible = (BasicValuedMapping) getInferredValueMapping();
		final var expressible =
				inferredExpressible == null
						? (BasicValuedMapping) determineCurrentExpressible( numericLiteral )
						: inferredExpressible;
		final var jdbcMapping = expressible.getJdbcMapping();
		return jdbcMapping.getValueConverter() != null
				// special case where we need to parse the value in order to apply the conversion
				? convertedUnparsedNumericLiteral( numericLiteral, expressible )
				: unparsedNumericLiteral( numericLiteral, jdbcMapping );
	}

	private static <N extends Number> UnparsedNumericLiteral<Number> unparsedNumericLiteral(
			SqmHqlNumericLiteral<N> numericLiteral, JdbcMapping jdbcMapping) {
		return new UnparsedNumericLiteral<>(
				numericLiteral.getUnparsedLiteralValue(),
				numericLiteral.getTypeCategory(),
				jdbcMapping
		);
	}

	private <N extends Number> Expression convertedUnparsedNumericLiteral(
			SqmHqlNumericLiteral<N> numericLiteral,
			BasicValuedMapping expressible) {
		final BasicValueConverter valueConverter = expressible.getJdbcMapping().getValueConverter();
		assert valueConverter != null;
		final Number parsedValue =
				numericLiteral.getTypeCategory()
						.parseLiteralValue( numericLiteral.getUnparsedLiteralValue() );
		return new QueryLiteral<>( sqlLiteralValue( valueConverter, parsedValue ), expressible );
	}

	private MappingModelExpressible<?> getKeyExpressible(JdbcMappingContainer mappingModelExpressible) {
		return mappingModelExpressible instanceof EntityAssociationMapping entityAssociationMapping
				? entityAssociationMapping.getKeyTargetMatchPart()
				: (MappingModelExpressible<?>) mappingModelExpressible;
	}

	private MappingModelExpressible<?> getElementExpressible(MappingModelExpressible<?> mappingModelExpressible) {
		return mappingModelExpressible instanceof PluralAttributeMapping pluralAttributeMapping
				? pluralAttributeMapping.getElementDescriptor()
				: mappingModelExpressible;
	}

	@Override
	public Map<SqmParameter<?>, List<List<JdbcParameter>>> getJdbcParamsBySqmParam() {
		return jdbcParamsBySqmParam;
	}

	@Override
	public Expression visitNamedParameterExpression(SqmNamedParameter<?> expression) {
		return consumeSqmParameter( expression );
	}

	protected Expression consumeSqmParameter(
			SqmParameter<?> sqmParameter,
			MappingModelExpressible<?> valueMapping,
			BiConsumer<Integer, JdbcParameter> jdbcParameterConsumer) {
		final List<JdbcParameter> jdbcParametersForSqm = new ArrayList<>();

		resolveSqmParameter(
				sqmParameter,
				valueMapping,
				(index, jdbcParameter) -> {
					jdbcParameterConsumer.accept( index, jdbcParameter );
					jdbcParametersForSqm.add( jdbcParameter );
				}
		);

		this.jdbcParameters.addParameters( jdbcParametersForSqm );
		this.jdbcParamsBySqmParam
				.computeIfAbsent( sqmParameter, k -> new ArrayList<>( 1 ) )
				.add( jdbcParametersForSqm );

		final var queryParameter = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding binding = domainParameterBindings.getBinding( queryParameter );
		binding.setType( valueMapping );
		return new SqmParameterInterpretation( jdbcParametersForSqm, valueMapping );
	}

	protected Expression consumeSqmParameter(SqmParameter<?> sqmParameter) {
		if ( sqmParameter.allowMultiValuedBinding() ) {
			final var domainParam = domainParameterXref.getQueryParameter( sqmParameter );
			final var domainParamBinding = domainParameterBindings.getBinding( domainParam );
			return !domainParamBinding.isMultiValued()
					? consumeSingleSqmParameter( sqmParameter )
					: expandParameter( sqmParameter, domainParamBinding, domainParam );
		}
		else {
			return consumeSingleSqmParameter( sqmParameter );
		}
	}

	private SqlTuple expandParameter(SqmParameter<?> sqmParameter, QueryParameterBinding<?> domainParamBinding, QueryParameterImplementor<?> domainParam) {
		final var bindValues = domainParamBinding.getBindValues();
		final List<Expression> expressions = new ArrayList<>( bindValues.size() );
		boolean first = true;
		for ( Object bindValue : bindValues ) {
			final SqmParameter<?> sqmParamToConsume;
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

	protected Expression consumeSingleSqmParameter(SqmParameter<?> sqmParameter) {
		return consumeSqmParameter( sqmParameter, determineValueMapping( sqmParameter ), (integer, jdbcParameter) -> {} );
	}

	@Override
	public MappingModelExpressible<?> determineValueMapping(SqmExpression<?> sqmExpression) {
		return determineValueMapping( sqmExpression, fromClauseIndexStack.getCurrent() );
	}

	private MappingModelExpressible<?> determineValueMapping(SqmExpression<?> sqmExpression, FromClauseIndex fromClauseIndex) {
		if ( sqmExpression instanceof SqmParameter<?> ) {
			return determineValueMapping( getSqmParameter( sqmExpression ) );
		}

		if ( sqmExpression instanceof SqmPath ) {
//			LOG.tracef( "Determining mapping-model type for SqmPath: %s ", sqmExpression );
			final var mappingModelExpressible =
					resolveMappingModelExpressible( sqmExpression, domainModel,
							fromClauseIndex::findTableGroup );
			if ( mappingModelExpressible != null ) {
				return mappingModelExpressible;
			}
		}

		if ( sqmExpression instanceof SqmBooleanExpressionPredicate expressionPredicate ) {
			return determineValueMapping( expressionPredicate.getBooleanExpression(), fromClauseIndex );
		}

		// The model type of an enum literal is always inferred
		if ( sqmExpression instanceof SqmEnumLiteral<?> ) {
			final var mappingModelExpressible = resolveInferredType();
			if ( mappingModelExpressible != null ) {
				return mappingModelExpressible;
			}
		}

		if ( sqmExpression instanceof SqmSubQuery<?> subquery ) {
			final var selectClause = subquery.getQuerySpec().getSelectClause();
			final var selections = selectClause.getSelections();
			if ( selections.size() == 1 ) {
				final var subQuerySelection = selections.get( 0 );
				if ( subQuerySelection.getSelectableNode() instanceof SqmExpression<?> expression ) {
					return determineValueMapping( expression, fromClauseIndex );
				}
				else {
					final var selectionNodeType = subQuerySelection.getNodeType();
					if ( selectionNodeType != null ) {
						final var expressible =
								domainModel.resolveMappingExpressible( selectionNodeType,
										this::findTableGroupByPath );
						if ( expressible != null ) {
							return expressible;
						}
						else {
							try {
								final var mappingModelExpressible = resolveInferredType();
								if ( mappingModelExpressible != null ) {
									return mappingModelExpressible;
								}
							}
							catch (Exception ignore) {
								return null;
							}
						}
					}
				}
			}
		}

		if ( sqmExpression instanceof SelfRenderingSqmFunction<?> selfRenderingSqmFunction ) {
			final var returnableType = selfRenderingSqmFunction.resolveResultType( this );
			return domainModel.resolveMappingExpressible(
					getCreationContext().resolveExpressible( returnableType ),
					this::findTableGroupByPath
			);
		}

		if ( sqmExpression instanceof SqmBinaryArithmetic<?> binaryArithmetic ) {
			if ( binaryArithmetic.getNodeType() == null ) {
				final var lhs = determineValueMapping( binaryArithmetic.getLeftHandOperand() );
				final var rhs = determineValueMapping( binaryArithmetic.getRightHandOperand() );
				// This cast should be fine since the result of this will be a BasicType
				return (MappingModelExpressible<?>) getTypeConfiguration().resolveArithmeticType(
						// These casts should be safe, since the only JdbcMapping is BasicType
						// which also implements SqmExpressible
						lhs == null ? null : (SqmBindableType<?>) lhs.getSingleJdbcMapping(),
						rhs == null ? null : (SqmBindableType<?>) rhs.getSingleJdbcMapping()
				);
			}
		}

//		LOG.tracef( "Determining mapping-model type for generalized SqmExpression: %s", sqmExpression );
		final var nodeType = sqmExpression.getNodeType();
		if ( nodeType == null ) {
			// We can't determine the type of the expression
			return null;
		}

		if ( nodeType instanceof EmbeddedSqmPathSource<?>
				&& sqmExpression instanceof SqmBinaryArithmetic<?> binaryArithmetic ) {
			final var lhs = binaryArithmetic.getLeftHandOperand();
			if ( lhs.getNodeType() == nodeType ) {
				return determineValueMapping( lhs, fromClauseIndex );
			}
			final var rhs = binaryArithmetic.getRightHandOperand();
			if ( rhs.getNodeType() == nodeType ) {
				return determineValueMapping( rhs, fromClauseIndex );
			}
		}


		final var valueMapping =
				domainModel.resolveMappingExpressible( nodeType, fromClauseIndex::getTableGroup );

		if ( valueMapping == null ) {
			final var mappingModelExpressible = resolveInferredType();
			if ( mappingModelExpressible != null ) {
				return mappingModelExpressible;
			}
		}

		if ( valueMapping == null ) {
			// For literals, it is totally possible that we can't figure out a mapping type
			if ( sqmExpression instanceof SqmLiteral<?> ) {
				return null;
			}
			throw new ConversionException( "Could not determine ValueMapping for SqmExpression: " + sqmExpression );
		}

		return valueMapping;
	}

	protected MappingModelExpressible<?> getInferredValueMapping() {
		final var inferredMapping = resolveInferredType();
		if ( inferredMapping != null ) {
			if ( inferredMapping instanceof PluralAttributeMapping pluralAttributeMapping ) {
				return pluralAttributeMapping.getElementDescriptor();
			}
			else if ( !( inferredMapping instanceof JavaObjectType ) ) {
				// Never report back the "object type" as inferred type and instead rely on the value type
				return inferredMapping;
			}
		}
		return null;
	}

	protected MappingModelExpressible<?> determineValueMapping(SqmParameter<?> sqmParameter) {
//		LOG.tracef( "Determining mapping-model type for SqmParameter: %s", sqmParameter );
		final var queryParameter = domainParameterXref.getQueryParameter( sqmParameter );
		final var binding = domainParameterBindings.getBinding( queryParameter );
		final boolean bindingTypeExplicit = binding.getExplicitTemporalPrecision() != null;
		BindableType<?> paramType = binding.getBindType();
		if ( paramType == null ) {
			paramType = queryParameter.getHibernateType();
			if ( paramType == null ) {
				paramType = sqmParameter.getAnticipatedType();
			}
//			bindingTypeExplicit = false;
		}
		else {
//			bindingTypeExplicit = binding.getExplicitTemporalPrecision() != null;
		}
		return determineValueMapping( sqmParameter, paramType, bindingTypeExplicit );
	}

	private MappingModelExpressible<?> determineValueMapping
			(SqmParameter<?> sqmParameter, @Nullable BindableType<?> paramType, boolean bindingTypeExplicit) {
		if ( paramType == null ) {
			final var inferredValueMapping = getInferredValueMapping();
			return inferredValueMapping != null
					? resolveInferredValueMappingForParameter( inferredValueMapping )
					: basicType( Object.class ); // Default to the Object type
		}
		else if ( paramType instanceof MappingModelExpressible<?> paramModelType ) {
			final var inferredValueMapping = getInferredValueMapping();
			// Prefer the model part type instead of the bind type if possible as the model part type contains size information
			if ( inferredValueMapping instanceof ModelPart inferredModelPart ) {
				final var paramJavaType = ((SqmBindableType<?>) paramType).getExpressibleJavaType();
				// Only use the inferred mapping as parameter type when the JavaType accepts values of the bind type
				if ( ( paramJavaType.getJavaTypeClass() == Object.class
						|| inferredModelPart.getJavaType() == paramJavaType
						|| inferredModelPart.getJavaType().isWider( paramJavaType ) )
						// and the bind type is not explicit or the bind type has the same JDBC type
						&& ( !bindingTypeExplicit || canUseInferredType( paramModelType, inferredValueMapping ) ) ) {
					return resolveInferredValueMappingForParameter( inferredValueMapping );
				}
			}
			return paramModelType;
		}
		else if ( sqmParameter.getAnticipatedType() == null ) {
			// this should indicate the condition that the user query did not define an
			// explicit type in regard to this parameter.  Here we should prefer the
			// inferrable type and fallback to resolving the binding type
			final var inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return resolveInferredValueMappingForParameter( inferredValueMapping );
			}
		}
		else if ( paramType instanceof EntityDomainType || paramType instanceof AnyMappingDomainType ) {
			// In JPA Criteria, it is possible to define a parameter of an entity type,
			// but that should infer the mapping type from context,
			// otherwise this would default to binding the PK which might be wrong
			final var inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return resolveInferredValueMappingForParameter( inferredValueMapping );
			}
		}
		return determineValueMapping( sqmParameter, paramType );
	}

	private MappingModelExpressible<?> determineValueMapping(SqmParameter<?> sqmParameter, BindableType<?> paramType) {
		final var paramSqmType = creationContext.resolveExpressible( paramType );
		if ( paramSqmType instanceof SqmPath<?> sqmPath ) {
			final var modelPart = determineValueMapping( sqmPath );
			if ( modelPart instanceof PluralAttributeMapping pluralAttributeMapping ) {
				return resolveInferredValueMappingForParameter( pluralAttributeMapping.getElementDescriptor() );
			}
			return modelPart;
		}
		else if ( paramSqmType instanceof BasicValuedMapping basicValuedMapping ) {
			return basicValuedMapping;
		}
		else if ( paramSqmType instanceof CompositeSqmPathSource || paramSqmType instanceof EmbeddableDomainType<?> ) {
			// Try to infer the value mapping since the other side apparently is a path source
			final var inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return resolveInferredValueMappingForParameter( inferredValueMapping );
			}
			else {
				throw new UnsupportedOperationException( "Support for embedded-valued parameters not yet implemented" );
			}
		}
		else if ( paramSqmType instanceof AnyDiscriminatorSqmPathSource<?> anyDiscriminatorSqmPathSource ) {
			return (MappingModelExpressible<?>) anyDiscriminatorSqmPathSource.getPathType();
		}
		else if ( paramSqmType instanceof SqmPathSource<?> || paramSqmType instanceof BasicDomainType<?> ) {
			// Try to infer the value mapping since the other side apparently is a path source
			final var inferredMapping = resolveInferredType();
			if ( inferredMapping instanceof PluralAttributeMapping pluralAttributeMapping ) {
				return resolveInferredValueMappingForParameter( pluralAttributeMapping.getElementDescriptor() );
			}
			else if ( inferredMapping != null && !( inferredMapping instanceof JavaObjectType ) ) {
				// Do not report back the "object type" as inferred type and instead try to rely on the paramSqmType.getExpressibleJavaType()
				return resolveInferredValueMappingForParameter( inferredMapping );
			}
			else {
				final var parameterJavaType = paramSqmType.getExpressibleJavaType().getJavaTypeClass();
				final var basicTypeForJavaType =
						getTypeConfiguration()
								.getBasicTypeForJavaType( parameterJavaType );
				if ( basicTypeForJavaType != null ) {
					return basicTypeForJavaType;
				}
				else {
					if ( paramSqmType instanceof EntityDomainType<?> entityDomainType ) {
						return resolveEntityPersister( entityDomainType );
					}
					else if ( paramSqmType instanceof SingularAttribute<?, ?> singularAttribute ) {
						if ( singularAttribute.getType() instanceof EntityDomainType<?> entityDomainType ) {
							return resolveEntityPersister( entityDomainType );
						}
					}
					// inferredMapping is null or JavaObjectType and we cannot deduct the type
					return inferredMapping;
				}
			}
		}
		else {
			throw new ConversionException( "Could not determine ValueMapping for SqmParameter: " + sqmParameter );
		}
	}

	private static boolean canUseInferredType(MappingModelExpressible<?> bindType, MappingModelExpressible<?> inferredType) {
		if ( inferredType.getJdbcTypeCount() != 1 ) {
			// The inferred type is an embeddable or entity with embeddable id, which is more concrete
			return true;
		}
		final var bindJdbcType = bindType.getSingleJdbcMapping().getJdbcType();
		final var inferredJdbcType = inferredType.getSingleJdbcMapping().getJdbcType();
		// If the bind type has a different JDBC type, we prefer that over the inferred type.
		return bindJdbcType == inferredJdbcType
			|| bindJdbcType instanceof ArrayJdbcType bindArrayType
				&& inferredJdbcType instanceof ArrayJdbcType inferredArrayType
				&& bindArrayType.getElementJdbcType() == inferredArrayType.getElementJdbcType();
	}

	private MappingModelExpressible<?> resolveInferredValueMappingForParameter(MappingModelExpressible<?> inferredValueMapping) {
		if ( inferredValueMapping instanceof PluralAttributeMapping pluralAttributeMapping ) {
			// For parameters, we resolve to the element descriptor
			inferredValueMapping = pluralAttributeMapping.getElementDescriptor();
		}
		if ( inferredValueMapping instanceof EntityCollectionPart entityCollectionPart ) {
			// For parameters, we resolve to the entity mapping type to bind the primary key
			return entityCollectionPart.getAssociatedEntityMappingType();
		}
		return inferredValueMapping;
	}

	private void resolveSqmParameter(
			SqmParameter<?> expression,
			MappingModelExpressible<?> valueMapping,
			BiConsumer<Integer,JdbcParameter> jdbcParameterConsumer) {
		sqmParameterMappingModelTypes.put( expression, valueMapping );
		final var jdbcParams = jdbcParamsBySqmParam.get( expression );
		final int parameterId = jdbcParams == null
				? jdbcParameters.getJdbcParameters().size()
				: castNonNull( jdbcParams.get( 0 ).get( 0 ).getParameterId() );
		final var bindable = bindable( valueMapping );
		if ( bindable instanceof SelectableMappings selectableMappings ) {
			selectableMappings.forEachSelectable(
					(index, selectableMapping)
							-> jdbcParameterConsumer.accept( index,
									new SqlTypedMappingJdbcParameter( selectableMapping, parameterId + index ) )
			);
		}
		else if ( bindable instanceof SelectableMapping selectableMapping ) {
			jdbcParameterConsumer.accept( 0,
					new SqlTypedMappingJdbcParameter( selectableMapping, parameterId ) );
		}
		else {
			final var sqlTypedMapping = sqlTypedMapping( expression, bindable );
			if ( sqlTypedMapping == null ) {
				if ( bindable == null ) {
					throw new ConversionException(
							"Could not determine neither the SqlTypedMapping nor the Bindable value for SqmParameter: " + expression );
				}
				bindable.forEachJdbcType(
						(index, jdbcMapping) -> jdbcParameterConsumer.accept(
								index,
								new JdbcParameterImpl( jdbcMapping, parameterId + index )
						)
				);
			}
			else {
				jdbcParameterConsumer.accept( 0,
						new SqlTypedMappingJdbcParameter( sqlTypedMapping, parameterId ) );
			}
		}
	}

	private SqlTypedMapping sqlTypedMapping(SqmParameter<?> expression, Bindable bindable) {
		if ( bindable instanceof BasicType<?> basicType) {
			final int sqlTypeCode = basicType.getJdbcType().getDdlTypeCode();
			if ( sqlTypeCode == SqlTypes.NUMERIC || sqlTypeCode == SqlTypes.DECIMAL ) {
				// For numeric and decimal parameter types we must determine the precision/scale of the value.
				// When we need to cast the parameter later, it is necessary to know the size to avoid truncation.
				final var binding = domainParameterBindings.getBinding(
						domainParameterXref.getQueryParameter( expression )
				);
				return sqlTypedMapping( binding, basicType );
			}
		}
		return null;
	}

	private static SqlTypedMapping sqlTypedMapping(QueryParameterBinding<?> binding, BasicType<?> bindable) {
		final Object bindValue;
		if ( binding.isMultiValued() ) {
			final var bindValues = binding.getBindValues();
			bindValue = bindValues.isEmpty() ? null : bindValues.iterator().next();
		}
		else {
			bindValue = binding.getBindValue();
		}
		return bindValue != null ? precision( bindable, bindValue ) : null;
	}

	private static SqlTypedMappingImpl precision(BasicType<?> bindable, Object bindValue) {
		if ( bindValue instanceof BigInteger bigInteger ) {
			final int precision =
					bindValue.toString().length()
						- ( bigInteger.signum() < 0 ? 1 : 0 );
			return new SqlTypedMappingImpl(
					null,
					null,
					precision,
					0,
					null,
					bindable.getJdbcMapping()
			);
		}
		else if ( bindValue instanceof BigDecimal bigDecimal ) {
			return new SqlTypedMappingImpl(
					null,
					null,
					bigDecimal.precision(),
					bigDecimal.scale(),
					null,
					bindable.getJdbcMapping()
			);
		}
		else {
			return null;
		}
	}

	private static Bindable bindable(MappingModelExpressible<?> valueMapping) {
		if ( valueMapping instanceof EntityAssociationMapping mapping ) {
			return mapping.getForeignKeyDescriptor().getPart( mapping.getSideNature() );
		}
		else if ( valueMapping instanceof EntityMappingType entityMappingType ) {
			return entityMappingType.getIdentifierMapping();
		}
		else {
			return valueMapping;
		}
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

		final var supplier = jpaCriteriaParamResolutions.get( expression );
		if ( supplier == null ) {
			throw new IllegalStateException( "Criteria parameter [" + expression + "] not known to be a parameter of the processing tree" );
		}
		return supplier;
	}

	@Override
	public Object visitTuple(SqmTuple<?> sqmTuple) {
		final var groupedExpressions = sqmTuple.getGroupedExpressions();
		final int size = groupedExpressions.size();
		final List<Expression> expressions = new ArrayList<>( size );
		if ( resolveInferredType() instanceof ValueMapping valueMapping
				&& valueMapping.getMappedType() instanceof EmbeddableMappingType embeddableMappingType ) {
			for ( int i = 0; i < size; i++ ) {
				final var attributeMapping = embeddableMappingType.getAttributeMapping( i );
				inferrableTypeAccessStack.push( () -> attributeMapping );
				try {
					expressions.add( (Expression) groupedExpressions.get( i ).accept( this ) );
				}
				finally {
					inferrableTypeAccessStack.pop();
				}
			}
		}
		else {
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
		return new SqlTuple( expressions, tupleValueMapping( sqmTuple ) );
	}

	private MappingModelExpressible<?> tupleValueMapping(SqmTuple<?> sqmTuple) {
		final var inferredType = resolveInferredType();
		if ( inferredType != null ) {
			return inferredType;
		}
		else if ( sqmTuple.getExpressible() instanceof MappingModelExpressible<?> modelExpressible ) {
			return modelExpressible;
		}
		else {
			return null;
		}
	}

	@Override
	public Object visitCollation(SqmCollation sqmCollation) {
		return new Collation( sqmCollation.getLiteralValue() );
	}

	@Override
	public Expression visitFunction(SqmFunction<?> sqmFunction) {
		final boolean oldInNestedContext = inNestedContext;
		inNestedContext = true;
		final var oldFunctionImpliedResultTypeAccess = functionImpliedResultTypeAccess;
		functionImpliedResultTypeAccess = inferrableTypeAccessStack.getCurrent();
		inferrableTypeAccessStack.push( () -> null );
		try {
			final var expression = sqmFunction.convertToSqlAst( this );
			if ( sqmFunction.getFunctionDescriptor().isPredicate()
					&& expression instanceof SelfRenderingExpression selfRenderingExpression) {
				final var booleanType = getBooleanType();
				return new CaseSearchedExpression(
						booleanType,
						List.of(
								new CaseSearchedExpression.WhenFragment(
										new SelfRenderingPredicate( selfRenderingExpression ),
										new QueryLiteral<>( true, booleanType )
								)
						),
						new QueryLiteral<>( false, booleanType )
				);
			}
			return expression;
		}
		finally {
			inferrableTypeAccessStack.pop();
			functionImpliedResultTypeAccess = oldFunctionImpliedResultTypeAccess;
			inNestedContext = oldInNestedContext;
		}
	}

	@Override
	public FunctionTableGroup visitSetReturningFunction(SqmSetReturningFunction<?> sqmFunction) {
		throw new UnsupportedOperationException("Should be handled by #consumeFromClauseRoot");
	}

	@Override
	public void registerQueryTransformer(QueryTransformer transformer) {
		queryTransformers.getCurrent().add( transformer );
	}

	@Override
	public Star visitStar(SqmStar sqmStar) {
		return new Star();
	}

	@Override
	public Object visitOver(SqmOver<?> over) {
		currentClauseStack.push( Clause.OVER );
		final var expression = (Expression) over.getExpression().accept( this );
		final var window = over.getWindow();
		final List<Expression> partitions = new ArrayList<>( window.getPartitions().size() );
		for ( var partition : window.getPartitions() ) {
			partitions.add( (Expression) partition.accept( this ) );
		}
		final List<SortSpecification> orderList = new ArrayList<>( window.getOrderList().size() );
		for ( var sortSpecification : window.getOrderList() ) {
			orderList.add( visitSortSpecification( sortSpecification ) );
		}
		final var startExpression = window.getStartExpression();
		final var endExpression = window.getEndExpression();
		final Over<Object> overExpression = new Over<>(
				expression,
				partitions,
				orderList,
				window.getMode(),
				window.getStartKind(),
				startExpression == null
						? null
						: (Expression) startExpression.accept( this ),
				window.getEndKind(),
				endExpression == null
						? null
						: (Expression) endExpression.accept( this ),
				window.getExclusion()
		);
		currentClauseStack.pop();
		return overExpression;
	}

	@Override
	public Object visitDistinct(SqmDistinct<?> sqmDistinct) {
		return new Distinct( (Expression) sqmDistinct.getExpression().accept( this ) );
	}

	@Override
	public Object visitOverflow(SqmOverflow<?> sqmOverflow) {
		final var fillerExpression = sqmOverflow.getFillerExpression();
		return new Overflow(
				(Expression) sqmOverflow.getSeparatorExpression().accept( this ),
				fillerExpression == null
						? null
						: (Expression) fillerExpression.accept( this ),
				sqmOverflow.isWithCount()
		);
	}

	@Override
	public Object visitTrimSpecification(SqmTrimSpecification specification) {
		return new TrimSpecification( specification.getSpecification() );
	}

	@Override
	public Object visitCastTarget(SqmCastTarget<?> target) {
		var targetType = (BasicValuedMapping) target.getType();
		if ( targetType instanceof BasicType basicType ) {
			targetType = resolveSqlTypeIndicators( this, basicType, target.getNodeJavaType() );
		}
		if ( targetType instanceof BasicPluralType<?, ?>
				&& target.getLength() != null
				&& target.getScale() == null ) {
			// Assume that the given length is the array length
			return new CastTarget(
					targetType.getJdbcMapping(),
					null,
					target.getLength().intValue(),
					null,
					null
			);
		}
		else {
			return new CastTarget(
					targetType.getJdbcMapping(),
					target.getLength(),
					target.getPrecision(),
					target.getScale()
			);
		}
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
		return new Format( sqmFormat.getLiteralValue() );
	}

	@Override
	public Object visitCoalesce(SqmCoalesce<?> sqmCoalesce) {
		final var queryEngine = creationContext.getSessionFactory().getQueryEngine();
		return creationContext.getSqmFunctionRegistry()
				.findFunctionDescriptor( "coalesce" )
				.generateSqmExpression( sqmCoalesce.getArguments(), null, queryEngine )
				.accept( this );
	}

	@Override
	public Object visitUnaryOperationExpression(SqmUnaryOperation<?> expression) {
		return new UnaryOperation(
				interpret( expression.getOperation() ),
				toSqlExpression( expression.getOperand().accept( this ) ),
				getExpressionType( expression )
		);
	}

	private UnaryArithmeticOperator interpret(UnaryArithmeticOperator operator) {
		return operator;
	}

	@Override
	public Object visitBinaryArithmeticExpression(SqmBinaryArithmetic<?> expression) {
		final var leftOperand = expression.getLeftHandOperand();
		final var rightOperand = expression.getRightHandOperand();

		// Need to infer the operand types here first to decide how to transform the expression
		final var fromClauseIndex = fromClauseIndexStack.getCurrent();
		inferrableTypeAccessStack.push( () -> determineValueMapping( rightOperand, fromClauseIndex ) );
		final var leftOperandType = determineValueMapping( leftOperand );
		inferrableTypeAccessStack.pop();
		inferrableTypeAccessStack.push( () -> determineValueMapping( leftOperand, fromClauseIndex ) );
		final var rightOperandType = determineValueMapping( rightOperand );
		inferrableTypeAccessStack.pop();

		final boolean durationToRight = isDuration( rightOperand.getNodeType() );
		final var temporalTypeToLeft = getSqlTemporalType( leftOperandType );
		final var temporalTypeToRight = getSqlTemporalType( rightOperandType );
		final boolean temporalTypeSomewhereToLeft = adjustedTimestamp != null || temporalTypeToLeft != null;

		if ( temporalTypeToLeft != null && durationToRight ) {
			if ( adjustmentScale != null || negativeAdjustment ) {
				//we can't distribute a scale over a date/timestamp
				throw new SemanticException( "Scalar multiplication of temporal value" );
			}
		}

		if ( durationToRight && temporalTypeSomewhereToLeft ) {
			return transformDurationArithmetic( expression );
		}
		else if ( temporalTypeToLeft != null && temporalTypeToRight != null ) {
			return transformDatetimeArithmetic( expression );
		}
		else {
			// Infer one operand type through the other
			inferrableTypeAccessStack.push( () -> determineValueMapping( rightOperand, fromClauseIndex ) );
			final var lhs = toSqlExpression( leftOperand.accept( this ) );
			inferrableTypeAccessStack.pop();
			inferrableTypeAccessStack.push( () -> determineValueMapping( leftOperand, fromClauseIndex ) );
			final var rhs = toSqlExpression( rightOperand.accept( this ) );
			inferrableTypeAccessStack.pop();

			if ( durationToRight && appliedByUnit != null ) {
				return new BinaryArithmeticExpression(
						lhs,
						expression.getOperator(),
						rhs,
						//after distributing the 'by unit' operator
						//we always get a Long value back
						(BasicValuedMapping) appliedByUnit.getNodeType()
				);
			}
			else {
				return new BinaryArithmeticExpression(
						lhs,
						expression.getOperator(),
						rhs,
						getExpressionType( expression )
				);
			}
		}
	}

	private BasicValuedMapping getExpressionType(SqmExpression<?> expression) {
		final var nodeType = expression.getNodeType();
		if ( nodeType == null ) {
			return JavaObjectType.INSTANCE;
		}
		else if ( nodeType instanceof BasicValuedMapping basicValuedMapping ) {
			return basicValuedMapping;
		}
		else if ( nodeType.getSqmType() instanceof BasicValuedMapping basicValuedMapping ) {
			return basicValuedMapping;
		}
		else {
			return getTypeConfiguration().getBasicTypeForJavaType(
					nodeType.getExpressibleJavaType().getJavaTypeClass()
			);
		}
	}

	private Expression toSqlExpression(Object value) {
		return (Expression) value;
	}

	private Object transformDurationArithmetic(SqmBinaryArithmetic<?> expression) {
		final var operator = expression.getOperator();
		final var leftHandOperand = expression.getLeftHandOperand();
		final var rightHandOperand = expression.getRightHandOperand();
		final var lhs = SqmExpressionHelper.getActualExpression( leftHandOperand );
		final var rhs = SqmExpressionHelper.getActualExpression( rightHandOperand );
		final var fromClauseIndex = fromClauseIndexStack.getCurrent();

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

				final var timestamp = adjustedTimestamp;
				final var timestampType = adjustedTimestampType;
				inferrableTypeAccessStack.push( () -> determineValueMapping( rhs, fromClauseIndex ) );
				adjustedTimestamp = toSqlExpression( lhs.accept( this ) );
				inferrableTypeAccessStack.pop();
				final var type = adjustedTimestamp.getExpressionType();
				if ( type instanceof SqmExpressible<?> sqmExpressible ) {
					adjustedTimestampType = sqmExpressible;
				}
//				else if ( type instanceof ValueMapping valueMapping ) {
//					adjustedTimestampType = (SqmExpressible<?>) valueMapping.getMappedType();
//				}
				else {
					// else we know it has not been transformed
					adjustedTimestampType = lhs.getNodeType();
				}
				if ( operator == SUBTRACT ) {
					negativeAdjustment = !negativeAdjustment;
				}
				try {
					inferrableTypeAccessStack.push( () -> determineValueMapping( lhs, fromClauseIndex ) );
					final Object result = rhs.accept( this );
					if ( result instanceof SqlTupleContainer ) {
						return result;
					}
					final NavigablePath baseNavigablePath;
					final Object offset;
					if ( lhs != leftHandOperand ) {
						final var temporalPath = (SqmPath<?>) leftHandOperand;
						baseNavigablePath = temporalPath.getNavigablePath().getParent();
						offset = SqmExpressionHelper.get( temporalPath, ZONE_OFFSET_NAME ).accept( this );
					}
					else if ( rhs != rightHandOperand ) {
						final var temporalPath = (SqmPath<?>) rightHandOperand;
						baseNavigablePath = temporalPath.getNavigablePath().getParent();
						offset = SqmExpressionHelper.get( temporalPath, ZONE_OFFSET_NAME ).accept( this );
					}
					else {
						return result;
					}
					final var valueMapping =
							(EmbeddableValuedModelPart) determineValueMapping( expression );
					return new EmbeddableValuedExpression<>(
							baseNavigablePath,
							valueMapping,
							new SqlTuple( List.of( (Expression) result, (Expression) offset ), valueMapping )
					);
				}
				finally {
					inferrableTypeAccessStack.pop();
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
				inferrableTypeAccessStack.push( () -> determineValueMapping( rhs, fromClauseIndex ) );
				final Expression duration = toSqlExpression( lhs.accept( this ) );
				inferrableTypeAccessStack.pop();
				final Expression scale = adjustmentScale;
				final boolean negate = negativeAdjustment;
				adjustmentScale = applyScale( duration );
				negativeAdjustment = false; //was sucked into the scale
				try {
					inferrableTypeAccessStack.push( () -> determineValueMapping( lhs, fromClauseIndex ) );
					return rhs.accept( this );
				}
				finally {
					inferrableTypeAccessStack.pop();
					adjustmentScale = scale;
					negativeAdjustment = negate;
				}
			default:
				throw new SemanticException( "Illegal operator for a duration " + operator );
		}
	}

	private Object transformDatetimeArithmetic(SqmBinaryArithmetic<?> expression) {
		final var operator = expression.getOperator();

		// the only kind of algebra we know how to
		// do on dates/timestamps is subtract them,
		// producing a duration - all other binary
		// operator expressions with two dates or
		// timestamps are ill-formed
		if ( operator != SUBTRACT ) {
			throw new SemanticException( "Illegal operator for temporal type: " + operator );
		}

		// a difference between two dates or two
		// timestamps is a leaf duration, so we
		// must apply the scale, and the 'by unit'
		// ts1 - ts2

		final var lhs = SqmExpressionHelper.getActualExpression( expression.getLeftHandOperand() );
		final var rhs = SqmExpressionHelper.getActualExpression( expression.getRightHandOperand() );

		final var fromClauseIndex = fromClauseIndexStack.getCurrent();
		inferrableTypeAccessStack.push( () -> determineValueMapping( rhs, fromClauseIndex ) );
		final var left = getActualExpression( cleanly( () -> toSqlExpression( lhs.accept( this ) ) ) );
		inferrableTypeAccessStack.pop();
		inferrableTypeAccessStack.push( () -> determineValueMapping( lhs, fromClauseIndex ) );
		final var right = getActualExpression( cleanly( () -> toSqlExpression( rhs.accept( this ) ) ) );
		inferrableTypeAccessStack.pop();

		// The result of timestamp subtraction is always a `Duration`, unless a unit is applied
		// So use SECOND granularity with fractions as that is what the `DurationJavaType` expects
		final TemporalUnit baseUnit = NATIVE;
		final BasicType<Long> diffResultType = basicType( Long.class );

		if ( adjustedTimestamp != null ) {
			if ( appliedByUnit != null ) {
				throw new IllegalStateException();
			}
			// we're using the resulting duration to
			// adjust a date or timestamp on the left

			// baseUnit is the finest resolution for the
			// temporal type, so we must use it for both
			// the diff, and then the subsequent add

			final var unit = new DurationUnit( baseUnit, diffResultType );
			final var durationType = (BasicValuedMapping) expression.getNodeType();
			final var scaledMagnitude = applyScale(
					timestampdiff().expression(
							(ReturnableType<?>) expression.getNodeType(),
							durationType.getJdbcMapping().getJdbcType().isInterval() ? null : unit,
							right,
							left
					)
			);
			return timestampadd().expression(
					(ReturnableType<?>) adjustedTimestampType, //TODO should be adjustedTimestamp.getType()
					unit,
					scaledMagnitude,
					adjustedTimestamp
			);
		}
		else if ( appliedByUnit != null ) {
			// we're immediately converting the resulting
			// duration to a scalar in the given unit

			final var unit = (DurationUnit) appliedByUnit.getUnit().accept( this );
			return applyScale( timestampdiff().expression( null, unit, right, left ) );
		}
		else {
			// a plain "bare" Duration
			final var unit = new DurationUnit( baseUnit, diffResultType );
			final var durationType = (BasicValuedMapping) expression.getNodeType();
			final var scaledMagnitude = applyScale(
					timestampdiff().expression(
							(ReturnableType<?>) expression.getNodeType(),
							durationType.getJdbcMapping().getJdbcType().isInterval() ? null : unit,
							right,
							left
					)
			);
			return new Duration( scaledMagnitude, baseUnit, durationType );
		}
	}

	private static Expression getActualExpression(Expression expression) {
		if ( expression.getExpressionType() instanceof EmbeddableValuedModelPart embeddableValuedModelPart ) {
			if ( JavaTypeHelper.isTemporal( embeddableValuedModelPart.getJavaType() ) ) {
				return ( (SqlTupleContainer) expression ).getSqlTuple().getExpressions().get( 0 );
			}
		}
		return expression;
	}

	private <J> BasicType<J> basicType(Class<J> javaType) {
		return creationContext.getMappingMetamodel().getTypeConfiguration().getBasicTypeForJavaType( javaType );
	}

	private TimestampaddFunction timestampadd() {
		return (TimestampaddFunction)
				getCreationContext().getSqmFunctionRegistry()
						.findFunctionDescriptor( "timestampadd" );
	}

	private TimestampdiffFunction timestampdiff() {
		return (TimestampdiffFunction)
				getCreationContext().getSqmFunctionRegistry()
						.findFunctionDescriptor( "timestampdiff" );
	}

	private <X> X cleanly(Supplier<X> supplier) {
		final var byUnit = appliedByUnit;
		final var timestamp = adjustedTimestamp;
		final var timestampType = adjustedTimestampType;
		final var scale = adjustmentScale;
		final boolean negate = negativeAdjustment;
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

		if ( magnitude instanceof UnaryOperation unary ) {
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
					final var magnitudeType = (BasicValuedMapping) magnitude.getExpressionType();
					magnitude = new BinaryArithmeticExpression(
							adjustmentScale,
							MULTIPLY,
							magnitude,
							magnitudeType.getJdbcMapping().getJdbcType().isInterval()
									? magnitudeType
									: widestNumeric(
											(BasicValuedMapping)
													adjustmentScale.getExpressionType(),
											magnitudeType
									)
					);
				}
			}
		}

		if ( negate ) {
			magnitude = new UnaryOperation(
					UNARY_MINUS,
					magnitude,
					(BasicValuedMapping)
							magnitude.getExpressionType()
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
		inferrableTypeAccessStack.push( () -> null );
		final var magnitude = toSqlExpression( toDuration.getMagnitude().accept( this ) );
		inferrableTypeAccessStack.pop();
		final var unit = (DurationUnit) toDuration.getUnit().accept( this );

		// let's start by applying the propagated scale
		// so we don't forget to do it in what follows
		final var scaledMagnitude = applyScale( magnitude );

		if ( adjustedTimestamp != null ) {
			// we're adding this literal duration to the
			// given date or timestamp, producing an
			// adjusted date or timestamp
			if ( appliedByUnit != null ) {
				throw new IllegalStateException();
			}
			return timestampadd().expression(
					(ReturnableType<?>) adjustedTimestampType, //TODO should be adjustedTimestamp.getType()
					unit, scaledMagnitude, adjustedTimestamp
			);
		}
		else {
			final var inferredValueMapping = getInferredValueMapping();
			final var durationType =
					inferredValueMapping != null
							? (BasicValuedMapping) inferredValueMapping
							: (BasicValuedMapping) toDuration.getNodeType();
			final var duration =
					scaledMagnitude.getExpressionType().getSingleJdbcMapping().getJdbcType().isInterval()
							? new Duration( extractEpoch( scaledMagnitude ), SECOND, durationType )
							: new Duration( scaledMagnitude, unit.getUnit(), durationType );

			if ( appliedByUnit != null ) {
				// we're applying the 'by unit' operator,
				// producing a literal scalar value in
				// the given unit
				final var appliedUnit = appliedByUnit.getUnit().getUnit();
				final var scalarType = (BasicValuedMapping) appliedByUnit.getNodeType();
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
		final var outer = appliedByUnit;
		appliedByUnit = byUnit;
		try {
			return byUnit.getDuration().accept( this );
		}
		finally {
			appliedByUnit = outer;
		}
	}

	private BasicValuedMapping widestNumeric(BasicValuedMapping lhs, BasicValuedMapping rhs) {
		final var lhsCastType = lhs.getJdbcMapping().getJdbcType().getCastType();
		final var rhsCastType = rhs.getJdbcMapping().getJdbcType().getCastType();
		if ( lhsCastType == CastType.FIXED ) {
			return lhs;
		}
		else if ( rhsCastType == CastType.FIXED ) {
			return rhs;
		}
		else if ( lhsCastType == CastType.DOUBLE ) {
			return lhs;
		}
		else if ( rhsCastType == CastType.DOUBLE ) {
			return rhs;
		}
		else if ( lhsCastType == CastType.FLOAT ) {
			return lhs;
		}
		else if ( rhsCastType == CastType.FLOAT ) {
			return rhs;
		}
		else {
			return lhs;
		}
	}

	@Override
	public Object visitModifiedSubQueryExpression(SqmModifiedSubQueryExpression<?> expr) {
		return new ModifiedSubQueryExpression(
				visitSubQueryExpression( expr.getSubQuery() ),
				convert( expr.getModifier() )
		);
	}

	private ModifiedSubQueryExpression.Modifier convert(SqmModifiedSubQueryExpression.Modifier modifier) {
		return switch ( modifier ) {
			case ALL -> ModifiedSubQueryExpression.Modifier.ALL;
			case ANY -> ModifiedSubQueryExpression.Modifier.ANY;
			case SOME -> ModifiedSubQueryExpression.Modifier.SOME;
		};
	}

	@Override
	public SelectStatement visitSubQueryExpression(SqmSubQuery<?> sqmSubQuery) {
		// The only purpose for tracking the current join is to
		// Reset the current join for subqueries because in there, we won't add nested joins
		final var oldJoin = currentlyProcessingJoin;
		final var oldCteContainer = cteContainer;
		currentlyProcessingJoin = null;
		final var cteContainer = this.visitCteContainer( sqmSubQuery );
		final var queryPart = visitQueryPart( sqmSubQuery.getQueryPart() );
		currentlyProcessingJoin = oldJoin;
		this.cteContainer = oldCteContainer;
		return new SelectStatement( cteContainer, queryPart, emptyList() );
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(SqmCaseSimple<?, ?> expression) {
		final List<CaseSimpleExpression.WhenFragment> whenFragments =
				new ArrayList<>( expression.getWhenFragments().size() );
		final var inferenceSupplier = inferrableTypeAccessStack.getCurrent();
		final boolean oldInNestedContext = inNestedContext;

		inNestedContext = true;
		inferrableTypeAccessStack.push(
				() -> {
					for ( var whenFragment : expression.getWhenFragments() ) {
						final var resolved = determineCurrentExpressible( whenFragment.getCheckValue() );
						if ( resolved != null ) {
							return resolved;
						}
					}
					return null;
				}
		);
		final var fixture = (Expression) expression.getFixture().accept( this );
		final var fixtureType = (MappingModelExpressible<?>) fixture.getExpressionType();
		inferrableTypeAccessStack.pop();
		var resolved = determineCurrentExpressible( expression );
		Expression otherwise = null;
		for ( var whenFragment : expression.getWhenFragments() ) {
			inferrableTypeAccessStack.push( () -> fixtureType );
			final var checkValue = (Expression) whenFragment.getCheckValue().accept( this );
			inferrableTypeAccessStack.pop();
			handleTypeInCaseExpression( fixture, checkValue );

			final MappingModelExpressible<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			final var resultExpression = (Expression) whenFragment.getResult().accept( this );
			inferrableTypeAccessStack.pop();
			resolved = (MappingModelExpressible<?>) highestPrecedence( resolved, resultExpression.getExpressionType() );

			whenFragments.add( new CaseSimpleExpression.WhenFragment( checkValue, resultExpression ) );
		}

		if ( expression.getOtherwise() != null ) {
			final MappingModelExpressible<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			otherwise = (Expression) expression.getOtherwise().accept( this );
			inferrableTypeAccessStack.pop();
			resolved = (MappingModelExpressible<?>) highestPrecedence( resolved, otherwise.getExpressionType() );
		}

		inNestedContext = oldInNestedContext;
		return new CaseSimpleExpression( resolved, fixture, whenFragments, otherwise );
	}

	private void handleTypeInCaseExpression(Expression fixture, Expression checkValue) {
		if ( fixture instanceof DiscriminatorPathInterpretation<?> typeExpression ) {
			final var tableGroup =
					getFromClauseIndex()
							.getTableGroup( typeExpression.getNavigablePath().getParent() );
			final var partMappingType = tableGroup.getModelPart().getPartMappingType();
			if ( partMappingType instanceof EntityMappingType entityMappingType ) {
				if ( entityMappingType.getDiscriminatorMapping().hasPhysicalColumn() ) {
					// If the entity type has a physical type column we only need to register an expression
					// usage for the root type to prevent pruning the table where the discriminator is found
					registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
							entityMappingType.getRootEntityDescriptor().getEntityName() );
				}
				else if ( checkValue instanceof EntityTypeLiteral typeLiteral ) {
					// Register an expression type usage for the literal subtype to prevent pruning its table group
					registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
							typeLiteral.getEntityTypeDescriptor().getEntityName() );
				}
				else {
					// We have to assume all types are possible and can't do optimizations
					registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
							entityMappingType.getEntityName() );
					for ( var subMappingType : entityMappingType.getSubMappingTypes() ) {
						registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
								subMappingType.getEntityName() );
					}
				}
			}
		}
	}

	@Override
	public CaseSearchedExpression visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		final List<CaseSearchedExpression.WhenFragment> whenFragments =
				new ArrayList<>( expression.getWhenFragments().size() );
		final var inferenceSupplier = inferrableTypeAccessStack.getCurrent();
		final boolean oldInNestedContext = inNestedContext;

		inNestedContext = true;
		var resolved = determineCurrentExpressible( expression );

		Expression otherwise = null;
		for ( var whenFragment : expression.getWhenFragments() ) {
			final var whenPredicate = visitNestedTopLevelPredicate( whenFragment.getPredicate() );
			final MappingModelExpressible<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			final var resultExpression = (Expression) whenFragment.getResult().accept( this );
			inferrableTypeAccessStack.pop();
			resolved = (MappingModelExpressible<?>) highestPrecedence( resolved, resultExpression.getExpressionType() );

			whenFragments.add( new CaseSearchedExpression.WhenFragment( whenPredicate, resultExpression ) );
		}

		if ( expression.getOtherwise() != null ) {
			final MappingModelExpressible<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			otherwise = (Expression) expression.getOtherwise().accept( this );
			inferrableTypeAccessStack.pop();
			resolved = (MappingModelExpressible<?>) highestPrecedence( resolved, otherwise.getExpressionType() );
		}

		inNestedContext = oldInNestedContext;
		return new CaseSearchedExpression( resolved, whenFragments, otherwise );
	}

	private MappingModelExpressible<?> determineCurrentExpressible(SqmTypedNode<?> expression) {
		return creationContext.getMappingMetamodel()
				.resolveMappingExpressible( expression.getNodeType(), getFromClauseIndex()::findTableGroup );
	}

	private <X> X visitWithInferredType(SqmExpression<?> expression, SqmExpression<?> inferred) {
		final var fromClauseIndex = fromClauseIndexStack.getCurrent();
		inferrableTypeAccessStack.push( () -> determineValueMapping( inferred, fromClauseIndex ) );
		try {
			return (X) expression.accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}
	}

	@Override
	public Object visitWithInferredType(
			SqmVisitableNode node,
			Supplier<MappingModelExpressible<?>> inferredTypeAccess) {
		inferrableTypeAccessStack.push( inferredTypeAccess );
		try {
			return node.accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}
	}

	@Override
	public Object visitAny(SqmAny<?> sqmAny) {
		return new Any(
				visitSubQueryExpression( sqmAny.getSubquery() ),
				null //resolveMappingExpressible( sqmAny.getNodeType() )
		);
	}

	@Override
	public Object visitEvery(SqmEvery<?> sqmEvery) {
		return new Every(
				visitSubQueryExpression( sqmEvery.getSubquery() ),
				null //resolveMappingExpressible( sqmEvery.getNodeType() )
		);
	}

	@Override
	public Object visitSummarization(SqmSummarization<?> sqmSummarization) {
		final var groupingExpressions = sqmSummarization.getGroupings();
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
		return switch ( kind ) {
			case CUBE -> Summarization.Kind.CUBE;
			case ROLLUP -> Summarization.Kind.ROLLUP;
		};
	}

	@Override
	public Expression visitEntityTypeLiteralExpression(SqmLiteralEntityType<?> sqmExpression) {
		final var mappingDescriptor =
				creationContext.getMappingMetamodel()
						.getEntityDescriptor( sqmExpression.getNodeType().getHibernateEntityName() );
		return new EntityTypeLiteral( mappingDescriptor );
	}

	@Override
	public Object visitEmbeddableTypeLiteralExpression(SqmLiteralEmbeddableType<?> expression) {
		// The node type of literal embeddable types will either be the discriminator
		// type for polymorphic embeddables or the Class standard basic type
		return new EmbeddableTypeLiteral( expression.getEmbeddableDomainType(),
				(BasicType<?>) expression.getNodeType() );
	}

	@Override
	public Expression visitAnyDiscriminatorTypeValueExpression(SqmAnyDiscriminatorValue<?> expression) {
		final var domainType = expression.getDomainType();
		return new QueryLiteral<>(
				domainType.convertToRelationalValue( expression.getEntityValue().getJavaType() ),
				domainType
		);
	}

	@Override
	public Expression visitParameterizedEntityTypeExpression(SqmParameterizedEntityType<?> sqmExpression) {
		assert resolveInferredType() instanceof EntityDiscriminatorMapping;
		return (Expression) sqmExpression.getDiscriminatorSource().accept( this );
	}

	@Override
	public Object visitEnumLiteral(SqmEnumLiteral<?> sqmEnumLiteral) {
		final var inferred = resolveInferredType();
		final SqlExpressible inferredType;
		if ( inferred instanceof PluralAttributeMapping pluralAttributeMapping ) {
			final var elementDescriptor = pluralAttributeMapping.getElementDescriptor();
			inferredType =
					elementDescriptor instanceof BasicValuedCollectionPart basicValuedCollectionPart
							? basicValuedCollectionPart
							: null;
		}
		else if ( inferred instanceof BasicValuedMapping basicValuedMapping ) {
			inferredType = basicValuedMapping;
		}
		else {
			inferredType = null;
		}
		if ( inferredType != null ) {
			return new QueryLiteral<>(
					inferredType.getJdbcMapping()
							.convertToRelationalValue( sqmEnumLiteral.getEnumValue() ),
					inferredType
			);
		}
		else {
			// This can only happen when selecting an enum literal, in which case we default to ordinal encoding
			return queryLiteral( sqmEnumLiteral, getTypeConfiguration() );
		}
	}

	private static <T extends Enum<T>> QueryLiteral<T> queryLiteral(
			SqmEnumLiteral<T> sqmEnumLiteral,
			TypeConfiguration typeConfiguration) {
		return new QueryLiteral<>(
				sqmEnumLiteral.getEnumValue(),
				new BasicTypeImpl<>(
						sqmEnumLiteral.getExpressibleJavaType(),
						typeConfiguration.getJdbcTypeRegistry()
								.getDescriptor( SqlTypes.SMALLINT )
				)
		);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Object visitFieldLiteral(SqmFieldLiteral<?> sqmFieldLiteral) {
		final var valueMapping = (BasicValuedMapping) determineValueMapping( sqmFieldLiteral );
		final Object value = sqmFieldLiteral.getValue();
		final BasicValueConverter converter = valueMapping.getJdbcMapping().getValueConverter();
		return new QueryLiteral<>( converter != null ? sqlLiteralValue( converter, value ) : value, valueMapping );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates

	@Override
	public Predicate visitNestedTopLevelPredicate(SqmPredicate predicate) {
		final Map<TableGroup, Map<String, EntityNameUse>> originalConjunctTableGroupTreatUsages =
				tableGroupEntityNameUses.isEmpty()
						? null
						: new IdentityHashMap<>( tableGroupEntityNameUses );
		tableGroupEntityNameUses.clear();
		inferrableTypeAccessStack.push( this::getBooleanType );
		final var result = (Predicate) predicate.accept( this );
		inferrableTypeAccessStack.pop();
		final var finalPredicate = combinePredicates( result, consumeConjunctTreatTypeRestrictions() );
		if ( originalConjunctTableGroupTreatUsages != null ) {
			for ( var entry : originalConjunctTableGroupTreatUsages.entrySet() ) {
				final var entityNameUses =
						tableGroupEntityNameUses.putIfAbsent( entry.getKey(), entry.getValue() );
				if ( entityNameUses != null && entityNameUses != entry.getValue() ) {
					for ( var useEntry : entry.getValue().entrySet() ) {
						final var currentUseKind = entityNameUses.get( useEntry.getKey() );
						final var value = currentUseKind == null
								? useEntry.getValue()
								: useEntry.getValue().stronger( currentUseKind );
						entityNameUses.put( useEntry.getKey(), value );
					}
				}
			}
		}
		return finalPredicate;
	}

	@Override
	public GroupedPredicate visitGroupedPredicate(SqmGroupedPredicate predicate) {
		return new GroupedPredicate( (Predicate) predicate.getSubPredicate().accept( this ) );
	}

	@Override
	public Junction visitJunctionPredicate(SqmJunctionPredicate predicate) {
		final var subpredicates = predicate.getPredicates();
		if ( predicate.getOperator() == BooleanOperator.AND ) {
			final List<Predicate> predicates = new ArrayList<>( subpredicates.size() );
			for ( var subpredicate : subpredicates ) {
				predicates.add( (Predicate) subpredicate.accept( this ) );
			}
			return new Junction( Junction.Nature.CONJUNCTION, predicates, getBooleanType() );
		}
		final var disjunction = new Junction(
				Junction.Nature.DISJUNCTION,
				new ArrayList<>( subpredicates.size() ),
				getBooleanType()
		);
		final Map<TableGroup, Map<String, EntityNameUse>> previousTableGroupEntityNameUses =
				new IdentityHashMap<>( tableGroupEntityNameUses );
		Map<TableGroup, Map<String, EntityNameUse>>[] disjunctEntityNameUsesArray = null;
		Map<TableGroup, Map<String, EntityNameUse>> entityNameUsesToPropagate = null;
		List<TableGroup> treatedTableGroups = null;
		List<TableGroup> filteredTableGroups = null;
		for ( int i = 0; i < subpredicates.size(); i++ ) {
			tableGroupEntityNameUses.clear();
			disjunction.add( (Predicate) subpredicates.get( i ).accept( this ) );
			if ( !tableGroupEntityNameUses.isEmpty() ) {
				if ( disjunctEntityNameUsesArray == null ) {
					disjunctEntityNameUsesArray = new Map[subpredicates.size()];
					entityNameUsesToPropagate = new IdentityHashMap<>( previousTableGroupEntityNameUses );
				}
				if ( i == 0 ) {
					// Collect the table groups for which filters are registered
					for ( var entry : tableGroupEntityNameUses.entrySet() ) {
						final var value = entry.getValue();
						if ( value.containsValue( EntityNameUse.TREAT )
							|| value.containsValue( EntityNameUse.OPTIONAL_TREAT ) ) {
							if ( treatedTableGroups == null ) {
								treatedTableGroups = new ArrayList<>( 1 );
							}
							treatedTableGroups.add( entry.getKey() );
						}
						if ( value.containsValue( EntityNameUse.FILTER ) ) {
							if ( filteredTableGroups == null ) {
								filteredTableGroups = new ArrayList<>( 1 );
							}
							filteredTableGroups.add( entry.getKey() );
						}
					}
				}
				// Create a copy of the filtered table groups from which we remove
				final List<TableGroup> missingTableGroupFilters =
						filteredTableGroups == null || i == 0
								? emptyList()
								: new ArrayList<>( filteredTableGroups );
				final List<TableGroup> missingTableGroupTreats =
						treatedTableGroups == null || i == 0
								? emptyList()
								: new ArrayList<>( treatedTableGroups );
				// Compute the entity name uses to propagate to the parent context
				// If every disjunct contains a FILTER, we can merge the filters
				// If every disjunct contains a TREAT, we can merge the treats
				// Otherwise, we downgrade the entity name uses to expression uses
				final var iterator = tableGroupEntityNameUses.entrySet().iterator();
				while ( iterator.hasNext() ) {
					final var entry = iterator.next();
					final var tableGroup = entry.getKey();
					final var entityNameUses =
							entityNameUsesToPropagate.computeIfAbsent( tableGroup, k -> new HashMap<>() );
					final boolean downgradeTreatUses;
					final boolean downgradeFilterUses;
					if ( getFromClauseAccess().findTableGroup( tableGroup.getNavigablePath() ) == null ) {
						// Always preserver name uses for table groups not found in the current from clause index
						previousTableGroupEntityNameUses.put( tableGroup, entry.getValue() );
						// Remove from the current junction context since no more processing is required
						if ( treatedTableGroups != null ) {
							treatedTableGroups.remove( tableGroup );
						}
						if ( filteredTableGroups != null ) {
							filteredTableGroups.remove( tableGroup );
						}
						iterator.remove();
						continue;
					}
					else if ( i == 0 ) {
						// Never downgrade treat or filter uses of the first disjunct
						downgradeTreatUses = false;
						downgradeFilterUses = false;
					}
					else {
						// If the table group is not part of the missingTableGroupTreats, we must downgrade treat uses
						downgradeTreatUses = !missingTableGroupTreats.contains( tableGroup );
						// If the table group is not part of the missingTableGroupFilters, we must downgrade filter uses
						downgradeFilterUses = !missingTableGroupFilters.contains( tableGroup );
					}
					for ( var useEntry : entry.getValue().entrySet() ) {
						final var useKind = useEntry.getValue().getKind();
						final var currentUseKind = entityNameUses.get( useEntry.getKey() );
						final EntityNameUse unionEntityNameUse;
						if ( useKind == EntityNameUse.UseKind.TREAT ) {
							if ( downgradeTreatUses ) {
								unionEntityNameUse = EntityNameUse.EXPRESSION;
							}
							else {
								unionEntityNameUse = useEntry.getValue();
								missingTableGroupTreats.remove( tableGroup );
							}
						}
						else if ( useKind == EntityNameUse.UseKind.FILTER ) {
							if ( downgradeFilterUses ) {
								unionEntityNameUse = EntityNameUse.EXPRESSION;
							}
							else {
								unionEntityNameUse = useEntry.getValue();
								missingTableGroupFilters.remove( tableGroup );
							}
						}
						else {
							unionEntityNameUse = useEntry.getValue();
						}
						entityNameUses.put( useEntry.getKey(),
								currentUseKind == null
										? unionEntityNameUse
										: unionEntityNameUse.stronger( currentUseKind ) );
					}
				}
				// Downgrade entity name uses for table groups that haven't been filtered in this disjunct
				for ( var missingTableGroupTreat : missingTableGroupTreats ) {
					treatedTableGroups.remove( missingTableGroupTreat );
					final var entityNameUses = entityNameUsesToPropagate.get( missingTableGroupTreat );
					for ( var entry : entityNameUses.entrySet() ) {
						if ( entry.getValue().getKind() == EntityNameUse.UseKind.TREAT ) {
							entry.setValue( EntityNameUse.EXPRESSION );
						}
					}
				}
				for ( var missingTableGroupFilter : missingTableGroupFilters ) {
					filteredTableGroups.remove( missingTableGroupFilter );
					final var entityNameUses = entityNameUsesToPropagate.get( missingTableGroupFilter );
					for ( var entry : entityNameUses.entrySet() ) {
						if ( entry.getValue() == EntityNameUse.FILTER ) {
							entry.setValue( EntityNameUse.EXPRESSION );
						}
					}
				}
				disjunctEntityNameUsesArray[i] = new IdentityHashMap<>( tableGroupEntityNameUses );
			}
			else {
				if ( treatedTableGroups != null ) {
					treatedTableGroups = null;
					for ( var entityNameUses : entityNameUsesToPropagate.values() ) {
						for ( var entry : entityNameUses.entrySet() ) {
							if ( entry.getValue().getKind() == EntityNameUse.UseKind.TREAT ) {
								entry.setValue( EntityNameUse.EXPRESSION );
							}
						}
					}
				}
				if ( filteredTableGroups != null ) {
					filteredTableGroups = null;
					for ( var entityNameUses : entityNameUsesToPropagate.values() ) {
						for ( var entry : entityNameUses.entrySet() ) {
							if ( entry.getValue() == EntityNameUse.FILTER ) {
								entry.setValue( EntityNameUse.EXPRESSION );
							}
						}
					}
				}
			}
		}
		if ( disjunctEntityNameUsesArray == null ) {
			tableGroupEntityNameUses.putAll( previousTableGroupEntityNameUses );
			return disjunction;
		}

		if ( !tableGroupEntityNameUses.isEmpty() ) {
			// Build the intersection of the conjunct treat usages,
			// so that we can push that up and infer during pruning, which entity subclasses can be omitted
			final var iterator = tableGroupEntityNameUses.entrySet().iterator();
			while ( iterator.hasNext() ) {
				final var entry = iterator.next();
				final Map<String, EntityNameUse> intersected = new HashMap<>( entry.getValue() );
				entry.setValue( intersected );
				boolean remove = false;
				for ( var conjunctTreatUsages : disjunctEntityNameUsesArray ) {
					final Map<String, EntityNameUse> entityNames;
					if ( conjunctTreatUsages == null
							|| ( entityNames = conjunctTreatUsages.get( entry.getKey() ) ) == null ) {
						remove = true;
						break;
					}
					// Intersect the two sets and transfer the common elements to the intersection
					final var intersectedIter = intersected.entrySet().iterator();
					while ( intersectedIter.hasNext() ) {
						final var intersectedEntry = intersectedIter.next();
						final var intersectedUseKind = intersectedEntry.getValue();
						final var useKind = entityNames.get( intersectedEntry.getKey() );
						if ( useKind == null ) {
							intersectedIter.remove();
						}
						else {
							// Possibly downgrade a FILTER use to EXPRESSION if one of the disjunctions does not use FILTER
							intersectedEntry.setValue( intersectedUseKind.weaker( useKind ) );
						}
					}
					if ( intersected.isEmpty() ) {
						remove = true;
						break;
					}
					entityNames.keySet().removeAll( intersected.keySet() );
					if ( entityNames.isEmpty() ) {
						conjunctTreatUsages.remove( entry.getKey() );
					}
				}

				if ( remove ) {
					entityNameUsesToPropagate.remove( entry.getKey() );
					iterator.remove();
				}
			}
		}
		else {
			// If there's no baseline to construct the intersection from don't propagate
			entityNameUsesToPropagate.clear();
		}

		// Prepend the treat type usages to the respective conjuncts
		for ( int i = 0; i < disjunctEntityNameUsesArray.length; i++ ) {
			final var conjunctTreatUsages = disjunctEntityNameUsesArray[i];
			if ( conjunctTreatUsages != null && !conjunctTreatUsages.isEmpty() ) {
				disjunction.getPredicates().set(
						i,
						combinePredicates(
								consumeConjunctTreatTypeRestrictions( conjunctTreatUsages ),
								disjunction.getPredicates().get( i )
						)
				);
			}
		}

		// Restore the parent context entity name uses state
		tableGroupEntityNameUses.clear();
		tableGroupEntityNameUses.putAll( previousTableGroupEntityNameUses );
		// Propagate the union of the entity name uses upwards
		for ( var entry : entityNameUsesToPropagate.entrySet() ) {
			final var entityNameUses =
					tableGroupEntityNameUses.putIfAbsent( entry.getKey(), entry.getValue() );
			if ( entityNameUses != null && entityNameUses != entry.getValue() ) {
				for ( var useEntry : entry.getValue().entrySet() ) {
					final var currentEntityNameUse = entityNameUses.get( useEntry.getKey() );
					final var value = currentEntityNameUse == null
							? useEntry.getValue()
							: useEntry.getValue().stronger( currentEntityNameUse );
					entityNameUses.put( useEntry.getKey(), value );
				}
			}
		}
		return disjunction;
	}

	@Override
	public Predicate visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
		final var pluralPath = predicate.getPluralPath();
		prepareReusablePath( pluralPath, () -> null );

		final var pluralAttributeMapping =
				(PluralAttributeMapping)
						determineValueMapping( pluralPath );

		inferrableTypeAccessStack.push( () -> pluralAttributeMapping );

		final Expression lhs;
		try {
			lhs = (Expression) predicate.getLeftHandExpression().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		final var parentFromClauseAccess = getFromClauseAccess();
		final var subQuerySpec = new QuerySpec( false );
		pushProcessingState(
				new SqlAstQueryPartProcessingStateImpl(
						subQuerySpec,
						getCurrentProcessingState(),
						this,
						currentClauseStack::getCurrent,
						false
				)
		);
		try {
			final var tableGroup = pluralAttributeMapping.createRootTableGroup(
					true,
					pluralPath.getNavigablePath(),
					null,
					null,
					() -> subQuerySpec::applyPredicate,
					this
			);

			pluralAttributeMapping.applyBaseRestrictions(
					subQuerySpec::applyPredicate,
					tableGroup,
					true,
					getLoadQueryInfluencers().getEnabledFilters(),
					false,
					null,
					this
			);

			getFromClauseAccess().registerTableGroup( pluralPath.getNavigablePath(), tableGroup );
			registerPluralTableGroupParts( tableGroup );
			subQuerySpec.getFromClause().addRoot( tableGroup );

			pluralAttributeMapping.getElementDescriptor().getInclusionCheckPart().applySqlSelections(
					pluralPath.getNavigablePath(),
					tableGroup,
					this
			);

			subQuerySpec.applyPredicate(
					pluralAttributeMapping.getKeyDescriptor().generateJoinPredicate(
							parentFromClauseAccess.findTableGroup( pluralPath.getNavigablePath().getParent() ),
							tableGroup,
							this
					)
			);
			final var subQuerySpecSelectClause = subQuerySpec.getSelectClause();
			subQuerySpec.applyPredicate(
					new ComparisonPredicate(
							toSingleExpression( subQuerySpecSelectClause.getSqlSelections(), lhs ),
							ComparisonOperator.EQUAL,
							lhs
					)
			);
			subQuerySpecSelectClause.getSqlSelections().clear();
			subQuerySpecSelectClause.addSqlSelection(
					new SqlSelectionImpl( new QueryLiteral<>( 1, basicType( Integer.class ) ) )
			);
		}
		finally {
			popProcessingStateStack();
		}

		return new ExistsPredicate(
				new SelectStatement( subQuerySpec ),
				predicate.isNegated(),
				getBooleanType()
		);
	}

	private Expression toSingleExpression(List<SqlSelection> sqlSelections, Expression inferenceSource) {
		assert !sqlSelections.isEmpty();

		if ( sqlSelections.size() == 1 ) {
			return sqlSelections.get( 0 ).getExpression();
		}
		else {
			final var expressions = new ArrayList<Expression>( sqlSelections.size() );
			for ( var sqlSelection : sqlSelections ) {
				expressions.add( sqlSelection.getExpression() );
			}
			return new SqlTuple( expressions,
					(MappingModelExpressible<?>)
							inferenceSource.getExpressionType() );
		}
	}

	@Override
	public NegatedPredicate visitNegatedPredicate(SqmNegatedPredicate predicate) {
		return new NegatedPredicate(
				(Predicate) predicate.getWrappedPredicate().accept( this )
		);
	}

	@Override
	public ComparisonPredicate visitComparisonPredicate(SqmComparisonPredicate predicate) {
		final var fromClauseIndex = fromClauseIndexStack.getCurrent();
		inferrableTypeAccessStack.push( () -> determineValueMapping( predicate.getRightHandExpression(), fromClauseIndex ) );

		final Expression lhs;
		try {
			lhs = (Expression) predicate.getLeftHandExpression().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		inferrableTypeAccessStack.push( () -> determineValueMapping( predicate.getLeftHandExpression(), fromClauseIndex ) );

		final Expression rhs;
		try {
			rhs = (Expression) predicate.getRightHandExpression().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		final var sqmOperator =
				predicate.isNegated()
						? predicate.getSqmOperator().negated()
						: predicate.getSqmOperator();
		handleTypeComparison( lhs, rhs, sqmOperator == ComparisonOperator.EQUAL );
		return new ComparisonPredicate( lhs, sqmOperator, rhs, getBooleanType() );
	}

	private void handleTypeComparison(Expression lhs, Expression rhs, boolean inclusive) {
		final DiscriminatorPathInterpretation<?> typeExpression;
		final EntityTypeLiteral literalExpression;
		if ( lhs instanceof DiscriminatorPathInterpretation ) {
			typeExpression = (DiscriminatorPathInterpretation<?>) lhs;
			literalExpression = rhs instanceof EntityTypeLiteral ? (EntityTypeLiteral) rhs : null;
		}
		else if ( rhs instanceof DiscriminatorPathInterpretation ) {
			typeExpression = (DiscriminatorPathInterpretation<?>) rhs;
			literalExpression = lhs instanceof EntityTypeLiteral ? (EntityTypeLiteral) lhs : null;
		}
		else {
			return;
		}
		handleTypeComparison(
				typeExpression,
				literalExpression != null ? singletonList( literalExpression ) : null,
				inclusive
		);
	}

	private void handleTypeComparison(
			DiscriminatorPathInterpretation<?> typeExpression,
			List<EntityTypeLiteral> literalExpressions,
			boolean inclusive) {
		final var tableGroup =
				getFromClauseIndex()
						.getTableGroup( typeExpression.getNavigablePath().getParent() );
		if ( tableGroup.getModelPart().getPartMappingType() instanceof EntityMappingType entityMappingType ) {
			if ( entityMappingType.getDiscriminatorMapping().hasPhysicalColumn() ) {
				// If the entity has a physical discriminator column we don't need to register any FILTER usages.
				// Register only an EXPRESSION usage to prevent pruning of the root type's table reference which
				// contains the physical discriminator column
				registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION,
						entityMappingType.getRootEntityDescriptor().getEntityName() );
			}
			else {
				final var subMappingTypes = entityMappingType.getSubMappingTypes();
				if ( literalExpressions == null ) {
					// We have to assume all types are possible and can't do optimizations
					registerEntityNameUsage( tableGroup, EntityNameUse.FILTER,
							entityMappingType.getEntityName() );
					for ( var subMappingType : subMappingTypes ) {
						registerEntityNameUsage( tableGroup, EntityNameUse.FILTER,
								subMappingType.getEntityName() );
					}
				}
				else {
					if ( inclusive ) {
						for ( var literalExpr : literalExpressions ) {
							registerEntityNameUsage( tableGroup, EntityNameUse.FILTER,
									literalExpr.getEntityTypeDescriptor().getEntityName() );
						}
					}
					else {
						final Set<String> excludedEntityNames =
								new HashSet<>( subMappingTypes.size() );
						for ( var literalExpr : literalExpressions ) {
							excludedEntityNames.add( literalExpr.getEntityTypeDescriptor().getEntityName() );
						}
						if ( !excludedEntityNames.contains( entityMappingType.getEntityName() ) ) {
							registerEntityNameUsage( tableGroup, EntityNameUse.FILTER,
									entityMappingType.getEntityName() );
						}
						for ( var subMappingType : subMappingTypes ) {
							if ( !excludedEntityNames.contains( subMappingType.getEntityName() ) ) {
								registerEntityNameUsage( tableGroup, EntityNameUse.FILTER,
										subMappingType.getEntityName() );
							}
						}
					}
				}
			}
		}
	}

	@Override
	public Object visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
		prepareReusablePath( predicate.getPluralPath(), () -> null );

		final var subQuerySpec = new QuerySpec( false, 1 );

		final var parentFromClauseAccess = getFromClauseAccess();
		final var subQueryState = new SqlAstQueryPartProcessingStateImpl(
				subQuerySpec,
				getCurrentProcessingState(),
				this,
				currentClauseStack::getCurrent,
				true
		);

		pushProcessingState( subQueryState );
		try {
			final var sqmPluralPath = predicate.getPluralPath();

			final var pluralPathNavPath = sqmPluralPath.getNavigablePath();
			final var parentNavPath = pluralPathNavPath.getParent();
			assert parentNavPath != null;

			final var parentTableGroup = parentFromClauseAccess.getTableGroup( parentNavPath );
			final var tableGroup = new CorrelatedTableGroup(
					parentTableGroup,
					sqlAliasBaseManager.createSqlAliasBase( parentTableGroup.getGroupAlias() ),
					subQuerySpec,
					subQuerySpec::applyPredicate,
					creationContext.getSessionFactory()
			);
			subQueryState.getSqlAstCreationState().getFromClauseAccess()
					.registerTableGroup( parentNavPath, tableGroup );
			registerPluralTableGroupParts( tableGroup );

			final var pluralAttributeMapping =
					(PluralAttributeMapping)
							visitPluralValuedPath( sqmPluralPath )
									.getExpressionType();
			// The creation of the table group join against the correlated table group
			// has the side effect that the from and where clause of the sub-query are set
			tableGroup.addTableGroupJoin(
					pluralAttributeMapping.createTableGroupJoin(
							pluralPathNavPath,
							tableGroup,
							sqmPluralPath.getExplicitAlias(),
							null,
							SqlAstJoinType.INNER,
							false,
							false,
							this
					)
			);

			final var collectionKeyDescriptor = pluralAttributeMapping.getKeyDescriptor();
			final int jdbcTypeCount = collectionKeyDescriptor.getJdbcTypeCount();
			assert jdbcTypeCount > 0;

			final var jdbcLiteral = new JdbcLiteral<>( 1, basicType( Integer.class ) );
			subQuerySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( jdbcLiteral ) );

			return new ExistsPredicate( subQuerySpec, !predicate.isNegated(), getBooleanType() );
		}
		finally {
			popProcessingStateStack();
		}
	}

	@Override
	public BetweenPredicate visitBetweenPredicate(SqmBetweenPredicate predicate) {
		final var fromClauseIndex = fromClauseIndexStack.getCurrent();
		final Expression expression;
		final Expression lowerBound;
		final Expression upperBound;

		inferrableTypeAccessStack.push(
				() -> coalesceSuppliedValues(
						() -> determineValueMapping( predicate.getLowerBound(), fromClauseIndex ),
						() -> determineValueMapping( predicate.getUpperBound(), fromClauseIndex )
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
						() -> determineValueMapping( predicate.getExpression(), fromClauseIndex ),
						() -> determineValueMapping( predicate.getUpperBound(), fromClauseIndex )
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
						() -> determineValueMapping( predicate.getExpression(), fromClauseIndex ),
						() -> determineValueMapping( predicate.getLowerBound(), fromClauseIndex )
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
		final var matchExpression = predicate.getMatchExpression();
		final var pattern = predicate.getPattern();
		return new LikePredicate(
				visitWithInferredType( matchExpression, pattern ),
				visitWithInferredType( pattern, matchExpression ),
				predicate.getEscapeCharacter() == null ?
						null :
						(Expression)
								visitWithInferredType( predicate.getEscapeCharacter(),
										() -> basicType( Character.class ) ),
				predicate.isNegated(),
				predicate.isCaseSensitive(),
				getBooleanType()
		);
	}

	@Override
	public NullnessPredicate visitIsNullPredicate(SqmNullnessPredicate predicate) {
		final Expression expression;
		if ( predicate.getExpression() instanceof SqmEntityValuedSimplePath<?> entityValuedPath ) {
			inferrableTypeAccessStack.push( () -> basicType( Object.class ) );
			expression = withTreatRestriction( prepareReusablePath(
					entityValuedPath,
					fromClauseIndexStack.getCurrent(),
					() -> EntityValuedPathInterpretation.from(
							entityValuedPath,
							getInferredValueMapping(),
							this
					),
					true
			), entityValuedPath );
			inferrableTypeAccessStack.pop();
		}
		else {
			expression = (Expression)
					visitWithInferredType( predicate.getExpression(),
							() -> basicType( Object.class ) );
		}
		return new NullnessPredicate( expression, predicate.isNegated(), getBooleanType() );
	}

	@Override
	public Object visitIsTruePredicate(SqmTruthnessPredicate predicate) {
		return new ThruthnessPredicate(
				(Expression)
						visitWithInferredType( predicate.getExpression(),
								() -> basicType( Boolean.class ) ),
				predicate.getBooleanValue(),
				predicate.isNegated(),
				getBooleanType()
		);
	}

	@Override
	public Predicate visitInListPredicate(SqmInListPredicate<?> predicate) {
		final var listExpressions = predicate.getListExpressions();
		// special case: if there is just a single "value" element and it is a parameter
		//		and the binding for that parameter is multi-valued we need special
		//		handling for "expansion"
		if ( listExpressions.size() == 1
				&& listExpressions.get( 0 ) instanceof SqmParameter<?> sqmParameter ) {
			if ( sqmParameter.allowMultiValuedBinding() ) {
				final var specialCase = processInListWithSingleParameter( predicate, sqmParameter );
				if ( specialCase != null ) {
					handleTypeComparison( specialCase );
					return specialCase;
				}
			}
		}

		// otherwise - no special case...
		final var fromClauseIndex = fromClauseIndexStack.getCurrent();
		inferrableTypeAccessStack.push(
				() -> {
					for ( var listExpression : listExpressions ) {
						final var mapping = determineValueMapping( listExpression, fromClauseIndex );
						if ( mapping != null ) {
							return mapping;
						}
					}
					return null;
				}
		);
		final Expression testExpression;
		try {
			testExpression = (Expression) predicate.getTestExpression().accept( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
		}

		final var inPredicate = new InListPredicate( testExpression, predicate.isNegated(), getBooleanType() );

		inferrableTypeAccessStack.push( () -> determineValueMapping( predicate.getTestExpression(), fromClauseIndex ) );

		try {
			for ( var expression : listExpressions ) {
				inPredicate.addExpression( (Expression) expression.accept( this ) );
			}
		}
		finally {
			inferrableTypeAccessStack.pop();
		}
		handleTypeComparison( inPredicate );
		return inPredicate;
	}

	private void handleTypeComparison(InListPredicate inPredicate) {
		if ( inPredicate.getTestExpression() instanceof DiscriminatorPathInterpretation<?> typeExpression ) {
			boolean containsNonLiteral = false;
			for ( var listExpression : inPredicate.getListExpressions() ) {
				if ( !( listExpression instanceof EntityTypeLiteral ) ) {
					containsNonLiteral = true;
					break;
				}
			}
			//noinspection unchecked
			handleTypeComparison(
					typeExpression,
					containsNonLiteral
							? null
							: (List<EntityTypeLiteral>) (List<?>)
									inPredicate.getListExpressions(),
					!inPredicate.isNegated()
			);
		}
	}

	private InListPredicate processInListWithSingleParameter(
			SqmInListPredicate<?> sqmPredicate,
			SqmParameter<?> sqmParameter) {
		assert sqmParameter.allowMultiValuedBinding();
		return sqmParameter instanceof JpaCriteriaParameter<?> parameter
				? processInSingleCriteriaParameter( sqmPredicate, parameter )
				: processInSingleHqlParameter( sqmPredicate, sqmParameter );

	}

	private InListPredicate processInSingleHqlParameter(SqmInListPredicate<?> sqmPredicate, SqmParameter<?> sqmParameter) {
		final var domainParam = domainParameterXref.getQueryParameter( sqmParameter );
		final var domainParamBinding = domainParameterBindings.getBinding( domainParam );
		// triggers normal processing
		return domainParamBinding.isMultiValued()
				? processInSingleParameter( sqmPredicate, sqmParameter, domainParam, domainParamBinding )
				: null;
	}

	private InListPredicate processInSingleCriteriaParameter(
			SqmInListPredicate<?> sqmPredicate,
			JpaCriteriaParameter<?> jpaCriteriaParameter) {
		assert jpaCriteriaParameter.allowsMultiValuedBinding();
		final var sqmWrapper = jpaCriteriaParamResolutions.get( jpaCriteriaParameter );
		final var domainParam = domainParameterXref.getQueryParameter( sqmWrapper );
		final var domainParamBinding = domainParameterBindings.getBinding( domainParam );
		return domainParamBinding.isMultiValued()
				? processInSingleParameter( sqmPredicate, sqmWrapper, domainParam, domainParamBinding )
				: null;
	}

	@SuppressWarnings( "rawtypes" )
	private InListPredicate processInSingleParameter(
			SqmInListPredicate<?> sqmPredicate,
			SqmParameter<?> sqmParameter,
			QueryParameterImplementor<?> domainParam,
			QueryParameterBinding<?> domainParamBinding) {
		final var iterator = domainParamBinding.getBindValues().iterator();

		final var inListPredicate = new InListPredicate(
				(Expression) sqmPredicate.getTestExpression().accept( this ),
				sqmPredicate.isNegated(),
				getBooleanType()
		);

		final var fromClauseIndex = fromClauseIndexStack.getCurrent();

		if ( !iterator.hasNext() ) {
			final var expressible =
					determineValueMapping( sqmPredicate.getTestExpression(), fromClauseIndex );
			domainParamBinding.setType( (MappingModelExpressible) expressible );
			return inListPredicate;
		}

		inferrableTypeAccessStack.push(
				() -> determineValueMapping( sqmPredicate.getTestExpression(), fromClauseIndex )
		);

		try {
			inListPredicate.addExpression( consumeSingleSqmParameter( sqmParameter ) );
			iterator.next();
			while ( iterator.hasNext() ) {
				iterator.next();
				// for each bind value create an "expansion"
				final var sqmParamToConsume = sqmParameter.copy();
				domainParameterXref.addExpansion( domainParam, sqmParameter, sqmParamToConsume );
				inListPredicate.addExpression( consumeSingleSqmParameter( sqmParamToConsume ) );
			}
			return inListPredicate;
		}
		finally {
			inferrableTypeAccessStack.pop();
		}
	}

	@Override
	public InSubQueryPredicate visitInSubQueryPredicate(SqmInSubQueryPredicate<?> predicate) {
		final var testExpression = predicate.getTestExpression();
		final var subQueryExpression = predicate.getSubQueryExpression();
		return new InSubQueryPredicate(
				visitWithInferredType( testExpression, subQueryExpression ),
				visitWithInferredType( subQueryExpression, testExpression ),
				predicate.isNegated(),
				getBooleanType()
		);
	}

	private BasicType<Boolean> getBooleanType() {
		return getTypeConfiguration().getBasicTypeForJavaType( Boolean.class );
	}

	@Override
	public Object visitBooleanExpressionPredicate(SqmBooleanExpressionPredicate predicate) {
		inferrableTypeAccessStack.push( this::getBooleanType );
		final var sqmExpression = predicate.getBooleanExpression();
		final var booleanExpression = (Expression) sqmExpression.accept( this );
		boolean negated = predicate.isNegated();
		if ( booleanExpression instanceof CaseSearchedExpression caseExpr
				&& sqmExpression instanceof SqmFunction<?> sqmFunction
				&& sqmFunction.getFunctionDescriptor().isPredicate() ) {
			// Functions that are rendered as predicates are always wrapped,
			// so the following unwraps the predicate and returns it directly instead of wrapping once more
			final var sqlPredicate = caseExpr.getWhenFragments().get( 0 ).getPredicate();
			return negated ? new NegatedPredicate( sqlPredicate ) : sqlPredicate;
		}

		inferrableTypeAccessStack.pop();
		if ( booleanExpression instanceof SelfRenderingExpression selfRenderingExpression ) {
			final var sqlPredicate = new SelfRenderingPredicate( selfRenderingExpression );
			return negated ? new NegatedPredicate( sqlPredicate ) : sqlPredicate;
		}
		else {
			final var jdbcMapping = booleanExpression.getExpressionType().getJdbcMapping( 0 );
			if ( jdbcMapping.getValueConverter() != null ) {
				// handle converted booleans (yes-no, etc)
				return new ComparisonPredicate(
						booleanExpression,
						ComparisonOperator.EQUAL,
						new JdbcLiteral<>( jdbcMapping.convertToRelationalValue( !negated ), jdbcMapping )
				);
			}
			return new BooleanExpressionPredicate( booleanExpression, negated, getBooleanType() );
		}
	}

	@Override
	public Object visitExistsPredicate(SqmExistsPredicate predicate) {
		inferrableTypeAccessStack.push( () -> null );
		final var selectStatement = (SelectStatement) predicate.getExpression().accept( this );
		inferrableTypeAccessStack.pop();
		return new ExistsPredicate( selectStatement, predicate.isNegated(), getBooleanType() );
	}

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}

	@Override
	public Object visitFullyQualifiedClass(Class<?> namedClass) {
		throw new UnsupportedOperationException();

		// what exactly is the expected end result here?

//		final MetamodelImplementor metamodel = getSessionFactory().getMetamodel();
//		final TypeConfiguration typeConfiguration = getSessionFactory().getTypeConfiguration();
//
//		// see if it is an entity-type
//		final EntityTypeDescriptor entityDescriptor = metamodel.findEntityDescriptor( namedClass );
//		if ( entityDescriptor != null ) {
//			throw new UnsupportedOperationException( "Add support for entity type literals as SqlExpression" );
//		}
//
//
//		final JavaType jtd = typeConfiguration
//				.getJavaTypeRegistry()
//				.getOrMakeJavaDescriptor( namedClass );
	}

	@Override
	public Object visitAsWrapperExpression(AsWrapperSqmExpression<?> sqmExpression) {
		return new AsWrappedExpression<>(
				(Expression) sqmExpression.getExpression().accept( this ),
				sqmExpression.getNodeType()
		);
	}

	@Override
	public Object visitNamedExpression(SqmNamedExpression<?> expression) {
		return new AliasedExpression(
				(Expression) expression.getExpression().accept( this ),
				expression.getName()
		);
	}

	@Override
	public Fetch visitIdentifierFetch(EntityResultGraphNode fetchParent) {
		final var identifierMapping =
				fetchParent.getReferencedMappingContainer().getIdentifierMapping();
		return createFetch( fetchParent, identifierMapping, false );
	}

	private Fetch createFetch(FetchParent fetchParent, Fetchable fetchable, Boolean isKeyFetchable) {
		if ( !fetchable.isSelectable() || isFetchableAuditExcluded( fetchable, loadQueryInfluencers ) ) {
			return null;
		}
		final var resolvedNavigablePath = fetchParent.resolveNavigablePath( fetchable );
		final var sqlSelectionsToTrack = trackedFetchSelectionsForGroup.get( resolvedNavigablePath );
		final int sqlSelectionStartIndexForFetch =
				sqlSelectionsToTrack == null
						? -1
						: currentSqlSelectionCollector()
								.getSelections( sqlSelectionsToTrack.getKey() )
								.size();

		final String alias;
		FetchTiming fetchTiming = fetchable.getMappedFetchOptions().getTiming();
		boolean joined = false;

		final Integer maxDepth = getCreationContext().getMaximumFetchDepth();
		final var fromClauseIndex = getFromClauseIndex();
		final var fetchedJoin = fromClauseIndex.findFetchedJoinByPath( resolvedNavigablePath );

		boolean explicitFetch = false;
		EntityGraphTraversalState.TraversalResult traversalResult = null;

		TableGroup joinedTableGroup = null;

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

			if ( entityGraphTraversalState != null ) {
				// Still do traverse the entity graph even if we encounter a fetch join
				traversalResult = entityGraphTraversalState.traverse(
						fetchParent,
						fetchable,
						isKeyFetchable
				);
			}
		}
		else {
			fetchablePath = resolvedNavigablePath;
			// there was not an explicit fetch in the SQM
			alias = null;

			if ( !( fetchable instanceof CollectionPart ) ) {
				if ( entityGraphTraversalState != null ) {
					traversalResult = entityGraphTraversalState.traverse( fetchParent, fetchable, isKeyFetchable );
					final var fetchStrategy = traversalResult.getFetchStrategy();
					if ( fetchStrategy != null ) {
						fetchTiming = fetchStrategy.getFetchTiming();
						joined = fetchStrategy.isJoined();
						if ( shouldExplicitFetch( maxDepth, fetchable ) ) {
							explicitFetch = true;
						}
					}
				}
				else if ( getLoadQueryInfluencers().hasEnabledFetchProfiles() ) {
					// There is no point in checking the fetch profile if it can't affect this fetchable
//					if ( fetchTiming != FetchTiming.IMMEDIATE || fetchable.incrementFetchDepth() ) {
						final String fetchableRole = fetchable.getNavigableRole().getFullPath();
						for ( String enabledFetchProfileName :
								getLoadQueryInfluencers().getEnabledFetchProfileNames() ) {
							final var enabledFetchProfile =
									getCreationContext().getFetchProfile( enabledFetchProfileName );
							final var profileFetch = enabledFetchProfile.getFetchByRole( fetchableRole );
							if ( profileFetch != null ) {
								fetchTiming = profileFetch.getTiming();
								joined = joined || profileFetch.getMethod() == FetchStyle.JOIN;
								if ( shouldExplicitFetch( maxDepth, fetchable ) ) {
									explicitFetch = true;
								}
								if ( currentBagRole != null
										&& fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
									final var collectionClassification =
											pluralAttributeMapping.getMappedType()
													.getCollectionSemantics()
													.getCollectionClassification();
									if ( collectionClassification == CollectionClassification.BAG ) {
										// To avoid a MultipleBagFetchException due to fetch profiles in a circular model,
										// we skip join fetching in case we encounter an existing bag role
										joined = false;
									}
								}
							}
//						}
					}
				}
			}

			// lastly, account for any app-defined max-fetch-depth
			if ( maxDepth != null && fetchDepth >= maxDepth ) {
				joined = false;
			}

			if ( joined && fetchable instanceof TableGroupJoinProducer joinProducer ) {
				joinedTableGroup = fromClauseIndex.resolveTableGroup(
						fetchablePath,
						np -> {
							// generate the join
							final TableGroup tableGroup;
							final var lhs = fromClauseIndex.getTableGroup( fetchParent.getNavigablePath() );
							final var compatibleTableGroup =
									lhs.findCompatibleJoinedGroup( joinProducer,
											joinProducer.getDefaultSqlAstJoinType( lhs ) );
							final var queryPart = getCurrentSqmQueryPart();
							if ( compatibleTableGroup == null
									// If the compatible table group is used in the where clause it cannot be reused for fetching
									|| ( queryPart != null && queryPart.getFirstQuerySpec()
											.whereClauseContains( compatibleTableGroup.getNavigablePath(), this ) ) ) {
								final var tableGroupJoin = joinProducer.createTableGroupJoin(
										fetchablePath,
										lhs,
										alias,
										null,
										null,
										true,
										false,
										BaseSqmToSqlAstConverter.this
								);
								lhs.addTableGroupJoin( tableGroupJoin );
								tableGroup = tableGroupJoin.getJoinedGroup();
							}
							else {
								tableGroup = compatibleTableGroup;
								if ( joinProducer instanceof PluralAttributeMapping attributeMapping ) {
									final var orderByFragment =
											attributeMapping.getOrderByFragment();
									if ( orderByFragment != null ) {
										applyOrdering( tableGroup, orderByFragment );
									}
									final var manyToManyOrderByFragment =
											attributeMapping.getManyToManyOrderByFragment();
									if ( manyToManyOrderByFragment != null ) {
										applyOrdering( tableGroup, manyToManyOrderByFragment );
									}
								}
							}
							// and return the joined group
							return tableGroup;
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
				final var biDirectionalFetch =
						fetchable.resolveCircularFetch( fetchablePath, fetchParent, fetchTiming, this );
				if ( biDirectionalFetch != null ) {
					return biDirectionalFetch;
				}
			}
			final var fetch = buildFetch( fetchablePath, fetchParent, fetchable, fetchTiming, joined, alias );

			if ( sqlSelectionsToTrack != null ) {
				final var selections = currentSqlSelectionCollector().getSelections( sqlSelectionsToTrack.getKey() );
				sqlSelectionsToTrack.getValue()
						.addAll( selections.subList( sqlSelectionStartIndexForFetch, selections.size() ) );
			}

			if ( fetch != null && fetch.getTiming() == FetchTiming.IMMEDIATE ) {
				if ( fetchable instanceof TableGroupJoinProducer ) {
					if ( joined && fetchable instanceof PluralAttributeMapping ) {
						containsCollectionFetches = true;
					}
					if ( joinedTableGroup != null ) {
						final var actualTableGroup =
								joinedTableGroup instanceof PluralTableGroup pluralTableGroup
										? pluralTableGroup.getElementTableGroup()
										: joinedTableGroup;
						final var mappingType =
								actualTableGroup == null
										? null
										: actualTableGroup.getModelPart().getPartMappingType();
						if ( mappingType instanceof EntityMappingType entityMappingType ) {
							registerEntityNameUsage( actualTableGroup, EntityNameUse.PROJECTION,
									entityMappingType.getEntityName(), true );
							if ( entityMappingType.getSuperMappingType() != null ) {
								// A joined table group was created by an enabled entity graph or fetch profile,
								// and it's of an inheritance subtype, so we should apply the discriminator
								getCurrentClauseStack().push( Clause.FROM );
								registerEntityNameUsage( actualTableGroup, EntityNameUse.TREAT,
										entityMappingType.getEntityName() );
								getCurrentClauseStack().pop();
							}
						}
					}
					if ( fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
						final var collectionClassification =
								pluralAttributeMapping.getMappedType()
										.getCollectionSemantics()
										.getCollectionClassification();
						if ( collectionClassification == CollectionClassification.BAG ) {
							final var navigableRole = fetchable.getNavigableRole();
							if ( currentBagRole != null ) {
								throw new MultipleBagFetchException(
										Arrays.asList( currentBagRole,
												navigableRole.getNavigableName() )
								);
							}
							currentBagRole = navigableRole.getNavigableName();
						}
					}
				}
			}
			return fetch;
		}
		finally {
			if ( incrementFetchDepth ) {
				fetchDepth--;
			}
			if ( entityGraphTraversalState != null && traversalResult != null ) {
				entityGraphTraversalState.backtrack( traversalResult );
			}
		}
	}

	@Override
	public <R> R withNestedFetchParent(FetchParent fetchParent, Function<FetchParent, R> action) {
		final var processingState = (SqlAstQueryPartProcessingStateImpl) getCurrentProcessingState();
		final var nestingFetchParent = processingState.getNestingFetchParent();
		processingState.setNestingFetchParent( fetchParent );
		final R result = action.apply( fetchParent );
		processingState.setNestingFetchParent( nestingFetchParent );
		return result;
	}

	@Override
	public ImmutableFetchList visitFetches(FetchParent fetchParent) {
		final var referencedMappingContainer = fetchParent.getReferencedMappingContainer();
		final int keySize = referencedMappingContainer.getNumberOfKeyFetchables();
		final int size = referencedMappingContainer.getNumberOfFetchables();
		final var fetches = new ImmutableFetchList.Builder( referencedMappingContainer );
		for ( int i = 0; i < keySize; i++ ) {
			final var fetch = createFetch( fetchParent,
					referencedMappingContainer.getKeyFetchable( i ), true );
			if ( fetch != null ) {
				fetches.add( fetch );
			}
		}
		for ( int i = 0; i < size; i++ ) {
			final var fetch = createFetch( fetchParent,
					referencedMappingContainer.getFetchable( i ), false );
			if ( fetch != null ) {
				fetches.add( fetch );
			}
		}
		return fetches.build();
	}

	private boolean shouldExplicitFetch(Integer maxDepth, Fetchable fetchable) {
		// Forcing the value of explicitFetch to true will disable the fetch circularity check and
		// for already visited association or collection this will cause a StackOverflow if maxFetchDepth is null, see HHH-15391.
		if ( maxDepth != null ) {
			return true;
		}
		else if ( fetchable instanceof ToOneAttributeMapping toOneAttributeMapping ) {
			return !isAssociationKeyVisited( toOneAttributeMapping.getForeignKeyDescriptor().getAssociationKey() );
		}
		else if ( fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
			return !isAssociationKeyVisited( pluralAttributeMapping.getKeyDescriptor().getAssociationKey() );
		}
		else {
			return true;
		}
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
			return fetchParent.generateFetchableFetch(
					fetchable,
					fetchablePath,
					fetchTiming,
					joined,
					alias,
					this
			);
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
//
//	private void applyOrdering(TableGroup tableGroup, PluralAttributeMapping pluralAttributeMapping) {
//		if ( pluralAttributeMapping.getOrderByFragment() != null ) {
//			applyOrdering( tableGroup, pluralAttributeMapping.getOrderByFragment() );
//		}
//
//		if ( pluralAttributeMapping.getManyToManyOrderByFragment() != null ) {
//			applyOrdering( tableGroup, pluralAttributeMapping.getManyToManyOrderByFragment() );
//		}
//	}

	@Override
	public void applyOrdering(TableGroup tableGroup, OrderByFragment orderByFragment) {
		if ( currentQuerySpec().isRoot() ) {
			if ( orderByFragments == null ) {
				orderByFragments = new ArrayList<>();
			}
			orderByFragments.add( new AbstractMap.SimpleEntry<>( orderByFragment, tableGroup ) );
		}
	}

	@Override
	public <S, M> M resolveMetadata(S source, Function<S, M> producer ) {
		//noinspection unchecked
		return (M) metadata.computeIfAbsent( new MetadataKey<>( source, producer ), k -> producer.apply( source ) );
	}

	static class MetadataKey<S, M> {
		private final S source;
		private final Function<S, M> producer;

		public MetadataKey(S source, Function<S, M> producer) {
			this.source = source;
			this.producer = producer;
		}

		@Override
		public boolean equals(Object object) {
			if ( this == object ) {
				return true;
			}
			else if ( object == null ) {
				return false;
			}
			else if ( !(object instanceof MetadataKey<?, ?> that) ) {
				return false;
			}
			else {
				return source.equals( that.source )
					&& producer.equals( that.producer );
			}
		}

		@Override
		public int hashCode() {
			int result = source.hashCode();
			result = 31 * result + producer.hashCode();
			return result;
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

	protected static class DelegatingSqmAliasedNodeCollector
			implements SqlExpressionResolver, SqmAliasedNodeCollector {

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
		public Expression resolveSqlExpression(ColumnReferenceKey key, Function<SqlAstProcessingState, Expression> creator) {
			return delegate.resolveSqlExpression( key, creator );
		}

		@Override
		public SqlSelection resolveSqlSelection(
				Expression expression,
				JavaType<?> javaType,
				FetchParent fetchParent,
				TypeConfiguration typeConfiguration) {
			return delegate.resolveSqlSelection( expression, javaType, fetchParent, typeConfiguration );
		}

		public void setSqmAliasedNodeCollector(SqmAliasedNodeCollector sqmAliasedNodeCollector) {
			this.sqmAliasedNodeCollector = sqmAliasedNodeCollector;
		}
	}

	protected static class SqmAliasedNodePositionTracker implements SqlExpressionResolver, SqmAliasedNodeCollector {
		private final SqlExpressionResolver delegate;
		private final List<SqlSelection>[] sqlSelectionsForSqmSelection;
		private int index = -1;

		public SqmAliasedNodePositionTracker(SqlExpressionResolver delegate, SqmQuerySpec<?> sqmQuerySpec) {
			this( delegate, countSqmSelections( sqmQuerySpec ) );
		}

		public SqmAliasedNodePositionTracker(SqlExpressionResolver delegate, int sqmSelections) {
			this.delegate = delegate;
			//noinspection unchecked
			this.sqlSelectionsForSqmSelection = new List[sqmSelections];
		}

		private static int countSqmSelections(SqmQuerySpec<?> sqmQuerySpec) {
			final var selections = sqmQuerySpec.getSelectClause().getSelections();
			if ( selections.isEmpty() ) {
				assert sqmQuerySpec.getFromClause().getRoots().size() == 1;
				return 1;
			}
			else {
				return countIndividualSelections( selections );
			}
		}

		private static int countIndividualSelections(List<? extends SqmAliasedNode<?>> selections) {
			int offset = 0;
			for ( int i = 0; i < selections.size(); i++ ) {
				final var selectableNode = selections.get( i ).getSelectableNode();
				if ( selectableNode instanceof SqmDynamicInstantiation<?> dynamicInstantiation ) {
					offset += countIndividualSelections( dynamicInstantiation.getArguments() );
				}
				else if ( selectableNode instanceof SqmJpaCompoundSelection<?> compoundSelection ) {
					for ( var node : compoundSelection.getSelectionItems() ) {
						if ( node instanceof SqmDynamicInstantiation<?> dynamicInstantiation ) {
							offset += countIndividualSelections( dynamicInstantiation.getArguments() );
						}
						else {
							offset += 1;
						}
					}
				}
				else {
					offset += 1;
				}
			}
			return offset;
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
		public Expression resolveSqlExpression(ColumnReferenceKey key, Function<SqlAstProcessingState, Expression> creator) {
			return delegate.resolveSqlExpression( key, creator );
		}

		@Override
		public SqlSelection resolveSqlSelection(
				Expression expression,
				JavaType<?> javaType,
				FetchParent fetchParent,
				TypeConfiguration typeConfiguration) {
			final var selection =
					delegate.resolveSqlSelection( expression, javaType, fetchParent, typeConfiguration );
			var sqlSelectionList = sqlSelectionsForSqmSelection[index];
			if ( sqlSelectionList == null ) {
				sqlSelectionsForSqmSelection[index] = sqlSelectionList = new ArrayList<>();
			}
			sqlSelectionList.add( selection );
			return selection;
		}
	}

	@Override
	public List<Expression> expandSelfRenderingFunctionMultiValueParameter(SqmParameter<?> sqmParameter) {
		assert sqmParameter.allowMultiValuedBinding();
		final var domainParam = domainParameterXref.getQueryParameter( sqmParameter );
		final var domainParamBinding = domainParameterBindings.getBinding( domainParam );

		final var bindValues = domainParamBinding.getBindValues();
		final int bindValuesSize = bindValues.size();
		final List<Expression> result = new ArrayList<>( bindValuesSize );

		boolean first = true;
		for ( int i = 0; i < bindValuesSize; i++ ) {
			final SqmParameter<?> sqmParamToConsume;
			// for each bind value create an "expansion"
			if ( first ) {
				sqmParamToConsume = sqmParameter;
				first = false;
			}
			else {
				sqmParamToConsume = sqmParameter.copy();
				domainParameterXref.addExpansion( domainParam, sqmParameter, sqmParamToConsume );
			}
			result.add( consumeSingleSqmParameter( sqmParamToConsume ) );
		}
		return result;
	}

	private static JdbcMappingContainer highestPrecedence(JdbcMappingContainer type1, JdbcMappingContainer type2) {
		if ( type1 == null ) {
			return type2;
		}
		else if ( type2 == null ) {
			return type1;
		}
		else if ( type1 instanceof ModelPart ) {
			return type1;
		}
		else if ( type2 instanceof ModelPart ) {
			return type2;
		}
		else {
			// todo (6.0) : we probably want a precedence based on generic resolutions such as those based on Serializable
			// todo (6.0) : anything else to consider?
			return type1;
		}
	}

	private static class CteContainerImpl implements CteContainer {
		private final CteContainer parent;
		private final Map<String, CteStatement> cteStatements;
		private final Map<String, CteObject> cteObjects;

		public CteContainerImpl(CteContainer parent) {
			this.parent = parent;
			this.cteStatements = new LinkedHashMap<>();
			this.cteObjects = new LinkedHashMap<>();
		}

		@Override
		public Map<String, CteStatement> getCteStatements() {
			return cteStatements;
		}

		@Override
		public CteStatement getCteStatement(String cteLabel) {
			final var cteStatement = cteStatements.get( cteLabel );
			return cteStatement == null && parent != null
					? parent.getCteStatement( cteLabel )
					: cteStatement;
		}

		@Override
		public void addCteStatement(CteStatement cteStatement) {
			final String tableExpression = cteStatement.getCteTable().getTableExpression();
			if ( cteStatements.putIfAbsent( tableExpression, cteStatement ) != null ) {
				throw new IllegalArgumentException( "A CTE with the label " + tableExpression + " already exists" );
			}
		}

		@Override
		public Map<String, CteObject> getCteObjects() {
			return cteObjects;
		}

		@Override
		public CteObject getCteObject(String cteObjectName) {
			final var cteObject = cteObjects.get( cteObjectName );
			return cteObject == null && parent != null
					? parent.getCteObject( cteObjectName )
					: cteObject;
		}

		@Override
		public void addCteObject(CteObject cteObject) {
			if ( cteObjects.putIfAbsent( cteObject.getName(), cteObject ) != null ) {
				throw new IllegalArgumentException( "A CTE object with the name " + cteObject.getName() + " already exists" );
			}
		}
	}

	@Override
	public boolean registerVisitedAssociationKey(AssociationKey associationKey) {
		return visitedAssociationKeys.add( associationKey );
	}

	@Override
	public void removeVisitedAssociationKey(AssociationKey associationKey) {
		visitedAssociationKeys.remove( associationKey );
	}

	@Override
	public boolean isAssociationKeyVisited(AssociationKey associationKey) {
		return visitedAssociationKeys.contains( associationKey );
	}

	@Override
	public boolean isRegisteringVisitedAssociationKeys() {
		/*
			We need to avoid loops in case of eager self-referencing associations

				E.g.
				@NamedEntityGraphs({
				@NamedEntityGraph(
						name = "User.overview",
						attributeNodes = { @NamedAttributeNode("name") })
				})
				class Sample {

					@ManyToOne
					Sample parent;

					String name;
				}

				TypedQuery<Sample> query = entityManager.createQuery(
							"select s from Sample s",
							Sample.class
					);
				query.setHint( SpecHints.HINT_SPEC_LOAD_GRAPH, entityGraph );

			 */
		return creationContext.getMaximumFetchDepth() == null
			&& ( entityGraphTraversalState != null || getLoadQueryInfluencers().hasEnabledFetchProfiles() );
	}

}
