/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import jakarta.annotation.Nullable;
import jakarta.persistence.TemporalType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import org.hibernate.HibernateException;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.boot.model.process.internal.InferredBasicValueResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.TimestampaddFunction;
import org.hibernate.dialect.function.TimestampdiffFunction;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.graph.spi.AppliedGraph;
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
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.metamodel.mapping.CollectionPart;
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
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
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
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.CompositeSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EntityDiscriminatorSqmPath;
import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityNameUse;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.BindableType;
import org.hibernate.query.QueryLogging;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaSearchOrder;
import org.hibernate.query.derived.AnonymousTupleEntityValuedModelPart;
import org.hibernate.query.derived.AnonymousTupleTableGroupProducer;
import org.hibernate.query.derived.AnonymousTupleType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.BinaryArithmeticOperator;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.InterpretationException;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.UnaryArithmeticOperator;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingFunctionSqlAstExpression;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.mutation.internal.SqmInsertStrategyHelper;
import org.hibernate.query.sqm.produce.function.internal.PatternRenderer;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.sql.internal.AnyDiscriminatorPathInterpretation;
import org.hibernate.query.sqm.sql.internal.AsWrappedExpression;
import org.hibernate.query.sqm.sql.internal.BasicValuedPathInterpretation;
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
import org.hibernate.query.sqm.tree.domain.SqmIndexAggregateFunction;
import org.hibernate.query.sqm.tree.domain.SqmIndexedCollectionAccessPath;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralPartJoin;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmSimplePath;
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
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmOver;
import org.hibernate.query.sqm.tree.expression.SqmOverflow;
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
import org.hibernate.query.sqm.tree.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.from.SqmDerivedJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmConflictClause;
import org.hibernate.query.sqm.tree.insert.SqmConflictUpdateAction;
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
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmJpaCompoundSelection;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmAssignment;
import org.hibernate.query.sqm.tree.update.SqmSetClause;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.SqlTreeCreationLogger;
import org.hibernate.sql.ast.internal.TableGroupJoinHelper;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
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
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.cte.CteColumn;
import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.cte.CteObject;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;
import org.hibernate.sql.ast.tree.cte.CteTableGroup;
import org.hibernate.sql.ast.tree.cte.SearchClauseSpecification;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
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
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.PluralTableGroup;
import org.hibernate.sql.ast.tree.from.QueryPartTableGroup;
import org.hibernate.sql.ast.tree.from.QueryPartTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
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
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.collection.internal.EagerCollectionFetch;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiation;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.StandardEntityGraphTraversalStateImpl;
import org.hibernate.type.BasicType;
import org.hibernate.type.BottomType;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JavaTypeHelper;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserVersionType;
import org.hibernate.usertype.internal.AbstractTimeZoneStorageCompositeUserType;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
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
import static java.util.Collections.singletonList;
import static org.hibernate.boot.model.internal.SoftDeleteHelper.createNonSoftDeletedRestriction;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.ADD;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.MULTIPLY;
import static org.hibernate.query.sqm.BinaryArithmeticOperator.SUBTRACT;
import static org.hibernate.query.sqm.TemporalUnit.EPOCH;
import static org.hibernate.query.sqm.TemporalUnit.NANOSECOND;
import static org.hibernate.query.sqm.TemporalUnit.NATIVE;
import static org.hibernate.query.sqm.TemporalUnit.SECOND;
import static org.hibernate.query.sqm.UnaryArithmeticOperator.UNARY_MINUS;
import static org.hibernate.query.sqm.internal.SqmUtil.isFkOptimizationAllowed;
import static org.hibernate.sql.ast.spi.SqlAstTreeHelper.combinePredicates;
import static org.hibernate.type.spi.TypeConfiguration.isDuration;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmToSqlAstConverter<T extends Statement> extends BaseSemanticQueryWalker
		implements SqmTranslator<T>, DomainResultCreationState, JdbcTypeIndicators {

	private static final Logger log = Logger.getLogger( BaseSqmToSqlAstConverter.class );

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
	private Stack<SqmQueryPart> sqmQueryPartStack = new StandardStack<>( SqmQueryPart.class );
	private CteContainer cteContainer;
	/**
	 * A map from {@link SqmCteTable#getCteName()} to the final SQL name.
	 * We use this global map as most databases don't support shadowing of names.
	 */
	private Map<String, String> cteNameMapping;
	private boolean containsCollectionFetches;
	private boolean trackSelectionsForGroup;
	/*
	 * Captures the list of SqlSelection for a navigable path.
	 * The map will only contain entries for order by elements of a QueryGroup, that refer to an attribute name
	 * i.e. `(select e from Entity e union all ...) order by name` where `name` is an attribute of the type `Entity`
	 */
	private Map<NavigablePath, Map.Entry<Integer, List<SqlSelection>>> trackedFetchSelectionsForGroup = Collections.emptyMap();

	private List<Map.Entry<OrderByFragment, TableGroup>> orderByFragments;

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();
	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>( SqlAstProcessingState.class );
	private final Stack<FromClauseIndex> fromClauseIndexStack = new StandardStack<>( FromClauseIndex.class );
	/*
	 * Captures all entity name uses under which a table group is being used within the current conjunct.
	 * Outside of a top level conjunct, it represents the "global uses" i.e. select, from, group and order by clauses.
	 * Top level conjunct contexts like visitWhereClause, visitHavingClause, visitOrPredicate and visitNestedTopLevelPredicate
	 * stash away the parent entity name uses, consumes the entity name uses of the conjunct by rendering a type restriction,
	 * and then restore the parent entity name uses again.
	 */
	private final Map<TableGroup, Map<String, EntityNameUse>> tableGroupEntityNameUses = new IdentityHashMap<>();
	private SqlAstProcessingState lastPoppedProcessingState;
	private FromClauseIndex lastPoppedFromClauseIndex;
	private SqmJoin<?, ?> currentlyProcessingJoin;
	protected Predicate additionalRestrictions;

	private final Stack<Clause> currentClauseStack = new StandardStack<>( Clause.class );
	private final Stack<Supplier> inferrableTypeAccessStack = new StandardStack<>( Supplier.class );
	private final Stack<List> queryTransformers = new StandardStack<>( List.class );
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
		this.jpaQueryComplianceEnabled = creationContext
				.getSessionFactory()
				.getSessionFactoryOptions()
				.getJpaCompliance()
				.isJpaQueryComplianceEnabled();

		this.statement = statement;
		this.currentSqmStatement = statement;
		this.deduplicateSelectionItems = deduplicateSelectionItems;

		if ( statement instanceof SqmSelectStatement<?> ) {
			final SqmQueryPart<?> queryPart = ( (SqmSelectStatement<?>) statement ).getQueryPart();
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
					queryPart.getFirstQuerySpec()
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
			if ( queryPart instanceof SqmQueryGroup<?> ) {
				this.deduplicateSelectionItems = false;
			}
			final AppliedGraph appliedGraph = queryOptions.getAppliedGraph();
			if ( appliedGraph != null && appliedGraph.getSemantic() != null && appliedGraph.getGraph() != null ) {
				this.entityGraphTraversalState = new StandardEntityGraphTraversalStateImpl(
						appliedGraph.getSemantic(), appliedGraph.getGraph(),
						creationContext.getSessionFactory().getJpaMetamodel()
				);
			}
			else {
				this.entityGraphTraversalState = null;
			}
		}
		else {
			this.domainResults = null;
			this.entityGraphTraversalState = null;
		}

		this.queryOptions = queryOptions;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.domainParameterXref = domainParameterXref;
		this.domainParameterBindings = domainParameterBindings;
		this.jpaCriteriaParamResolutions = domainParameterXref.getParameterResolutions().getJpaCriteriaParamResolutions();
		this.domainModel = creationContext.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel();
	}

	private static Boolean stackMatchHelper(SqlAstProcessingState processingState, SqlAstProcessingState c) {
		if ( !( processingState instanceof SqlAstQueryPartProcessingState ) ) {
			return Boolean.FALSE;
		}
		if ( processingState == c ) {
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

	private static Boolean matchSqlAstWithQueryPart(SqlAstProcessingState state, QueryPart cteQueryPartLocal) {
		if ( state instanceof SqlAstQueryPartProcessingState ) {
			if ( ( (SqlAstQueryPartProcessingState) state ).getInflightQueryPart() == cteQueryPartLocal ) {
				return Boolean.TRUE;
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

	@Override
	public Dialect getDialect() {
		return creationContext.getSessionFactory().getJdbcServices().getDialect();
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

	@Override
	public int getPreferredSqlTypeCodeForDuration() {
		return creationContext.getSessionFactory().getSessionFactoryOptions().getPreferredSqlTypeCodeForDuration();
	}

	@Override
	public int getPreferredSqlTypeCodeForUuid() {
		return creationContext.getSessionFactory().getSessionFactoryOptions().getPreferredSqlTypeCodeForUuid();
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

	@SuppressWarnings("rawtypes")
	@Override
	public Stack<SqmQueryPart> getSqmQueryPartStack() {
		return sqmQueryPartStack;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Statements

	@Override
	public SqmTranslation<T> translate() {
		final SqmStatement<?> sqmStatement = getStatement();
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
		final CteContainer oldCteContainer = cteContainer;
		final CteContainer cteContainer = this.visitCteContainer( sqmStatement );
		final SqmStatement<?> oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = sqmStatement;
		final SqmRoot<?> sqmTarget = sqmStatement.getTarget();
		final String entityName = sqmTarget.getEntityName();

		final EntityPersister entityDescriptor = getCreationContext()
				.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		final FromClause fromClause = new FromClause( 1 );
		final SqlAstQueryNodeProcessingStateImpl queryNodeProcessingState = new SqlAstQueryNodeProcessingStateImpl(
				fromClause,
				getCurrentProcessingState(),
				this,
				getCurrentClauseStack()::getCurrent
		);
		pushProcessingState( queryNodeProcessingState );

		try {
			consumeFromClauseRoot( sqmTarget );
			final TableGroup rootTableGroup = getFromClauseAccess().getTableGroup( sqmTarget.getNavigablePath() );

			final List<Assignment> assignments = visitSetClause( sqmStatement.getSetClause() );
			addVersionedAssignment( assignments::add, sqmStatement );

			Predicate suppliedPredicate = null;
			final SqmWhereClause whereClause = sqmStatement.getWhereClause();
			if ( whereClause != null ) {
				suppliedPredicate = visitWhereClause( whereClause.getPredicate() );
			}

			return new UpdateStatement(
					cteContainer,
					(NamedTableReference) rootTableGroup.getPrimaryTableReference(),
					fromClause,
					assignments,
					combinePredicates(
							suppliedPredicate,
							combinePredicates(
									queryNodeProcessingState.getPredicate(),
									additionalRestrictions
							)
					),
					Collections.emptyList()
			);
		}
		finally {
			popProcessingStateStack();
			this.currentSqmStatement = oldSqmStatement;
			this.cteContainer = oldCteContainer;
		}
	}

	public void addVersionedAssignment(Consumer<Assignment> assignmentConsumer, SqmUpdateStatement<?> sqmStatement) {
		if ( !sqmStatement.isVersioned() ) {
			return;
		}
		final EntityPersister persister = creationContext.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.findEntityDescriptor( sqmStatement.getTarget().getEntityName() );
		if ( !persister.isVersioned() ) {
			throw new SemanticException( "Increment option specified for update of non-versioned entity" );
		}

		final BasicType<?> versionType = persister.getVersionType();
		if ( versionType instanceof UserVersionType ) {
			throw new SemanticException( "User-defined version types not supported for increment option" );
		}

		currentClauseStack.push( Clause.SET );
		final EntityVersionMapping versionMapping = persister.getVersionMapping();
		final List<ColumnReference> targetColumnReferences = BasicValuedPathInterpretation.from(
				(SqmBasicValuedSimplePath<?>) sqmStatement
						.getRoot()
						.get( versionMapping.getPartName() ),
				this,
				jpaQueryComplianceEnabled
		).getColumnReferences();
		currentClauseStack.pop();
		assert targetColumnReferences.size() == 1;

		final ColumnReference versionColumn = targetColumnReferences.get( 0 );
		final Expression value;
		if ( versionMapping.getJdbcMapping().getJdbcType().isTemporal() ) {
			value = new VersionTypeSeedParameterSpecification( versionMapping );
		}
		else {
			value = new BinaryArithmeticExpression(
					versionColumn,
					ADD,
					new QueryLiteral<>(
							persister.getVersionJavaType().next(
									persister.getVersionJavaType().seed(
											versionMapping.getLength(),
											versionMapping.getTemporalPrecision() != null
													? versionMapping.getTemporalPrecision()
													: versionMapping.getPrecision(),
											versionMapping.getScale(),
											null
									),
									versionMapping.getLength(),
									versionMapping.getTemporalPrecision() != null
											? versionMapping.getTemporalPrecision()
											: versionMapping.getPrecision(),
									versionMapping.getScale(),
									null
							),
							versionType
					),
					versionType
			);
		}
		assignmentConsumer.accept( new Assignment( versionColumn, value ) );
	}

	@Override
	public List<Assignment> visitSetClause(SqmSetClause setClause) {
		final ArrayList<Assignment> assignments = new ArrayList<>( setClause.getAssignments().size() );

		final SqmRoot<?> target = ( (SqmDmlStatement<?>) currentSqmStatement ).getTarget();
		final String entityName = target.getEntityName();
		final EntityPersister entityDescriptor = getCreationContext()
				.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );

		// Returns a new instance for collecting assignments if needed
		final AggregateColumnAssignmentHandler aggregateColumnAssignmentHandler = AggregateColumnAssignmentHandler.forEntityDescriptor(
				entityDescriptor,
				setClause.getAssignments().size()
		);

		for ( SqmAssignment<?> sqmAssignment : setClause.getAssignments() ) {
			final SqmPathInterpretation<?> assignedPathInterpretation;
			try {
				currentClauseStack.push( Clause.SET );
				assignedPathInterpretation = (SqmPathInterpretation<?>) sqmAssignment.getTargetPath().accept( this );
			}
			finally {
				currentClauseStack.pop();
			}

			try {
				inferrableTypeAccessStack.push( assignedPathInterpretation::getExpressionType );
				currentClauseStack.push( Clause.SET_EXPRESSION );

				final SqmExpression<?> assignmentValue = sqmAssignment.getValue();
				final SqmParameter<?> assignmentValueParameter = getSqmParameter( assignmentValue );
				final Expression pathSqlExpression = assignedPathInterpretation.getSqlExpression();
				final List<ColumnReference> targetColumnReferences;
				if ( pathSqlExpression instanceof SqlTuple ) {
					//noinspection unchecked
					targetColumnReferences = (List<ColumnReference>) ( (SqlTuple) pathSqlExpression ).getExpressions();
				}
				else {
					targetColumnReferences = pathSqlExpression.getColumnReference().getColumnReferences();
				}
				if ( assignmentValueParameter != null ) {
					consumeSqmParameter(
							assignmentValueParameter,
							assignedPathInterpretation.getExpressionType(),
							(index, jdbcParameter) -> addAssignment(
									assignments,
									aggregateColumnAssignmentHandler,
									targetColumnReferences.get( index ),
									jdbcParameter
							)
					);
				}
				else if ( assignmentValue instanceof SqmLiteralNull<?> ) {
					for ( ColumnReference columnReference : targetColumnReferences ) {
						addAssignment(
								assignments,
								aggregateColumnAssignmentHandler,
								columnReference,
								new QueryLiteral<>( null, (BasicValuedMapping) columnReference.getExpressionType() )
						);
					}
				}
				else {
					final Expression valueExpression = (Expression) assignmentValue.accept( this );

					final int valueExprJdbcCount = getKeyExpressible( valueExpression.getExpressionType() ).getJdbcTypeCount();
					final int assignedPathJdbcCount = getKeyExpressible( assignedPathInterpretation.getExpressionType() )
							.getJdbcTypeCount();

					if ( valueExprJdbcCount != assignedPathJdbcCount ) {
						SqlTreeCreationLogger.LOGGER.debug(
								"JDBC type count does not match in UPDATE assignment between the assigned-path and the assigned-value; " +
										"this will likely lead to problems executing the query"
						);
					}

					assert assignedPathJdbcCount == valueExprJdbcCount;

					if ( valueExpression instanceof SqlTuple ) {
						final List<? extends Expression> expressions = ( (SqlTuple) valueExpression ).getExpressions();
						assert targetColumnReferences.size() == expressions.size();
						for ( int i = 0; i < targetColumnReferences.size(); i++ ) {
							final ColumnReference columnReference = targetColumnReferences.get( i );
							addAssignment( assignments, aggregateColumnAssignmentHandler, columnReference, expressions.get( i ) );
						}
					}
					else if ( valueExpression instanceof EmbeddableValuedPathInterpretation<?> ) {
						final List<? extends Expression> expressions = ( (EmbeddableValuedPathInterpretation<?>) valueExpression ).getSqlTuple().getExpressions();
						assert targetColumnReferences.size() == expressions.size();
						for ( int i = 0; i < targetColumnReferences.size(); i++ ) {
							final ColumnReference columnReference = targetColumnReferences.get( i );
							addAssignment( assignments, aggregateColumnAssignmentHandler, columnReference, expressions.get( i ) );
						}
					}
					else {
						for ( ColumnReference columnReference : targetColumnReferences ) {
							addAssignment( assignments, aggregateColumnAssignmentHandler, columnReference, valueExpression );
						}
					}
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

	private void addAssignment(
			List<Assignment> assignments,
			AggregateColumnAssignmentHandler aggregateColumnAssignmentHandler,
			ColumnReference columnReference,
			Expression valueExpression) {
		if ( aggregateColumnAssignmentHandler != null ) {
			aggregateColumnAssignmentHandler.addAssignment( assignments.size(), columnReference );
		}
		assignments.add( new Assignment( columnReference, valueExpression ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Delete statement

	@Override
	public DeleteStatement visitDeleteStatement(SqmDeleteStatement<?> statement) {
		final CteContainer oldCteContainer = cteContainer;
		final CteContainer cteContainer = this.visitCteContainer( statement );
		final SqmStatement<?> oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = statement;
		final SqmRoot<?> sqmTarget = statement.getTarget();
		final String entityName = sqmTarget.getEntityName();
		final EntityPersister entityDescriptor = creationContext.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		assert entityDescriptor != null;

		final FromClause fromClause = new FromClause( 1 );
		final SqlAstQueryNodeProcessingStateImpl queryNodeProcessingState = new SqlAstQueryNodeProcessingStateImpl(
				fromClause,
				getCurrentProcessingState(),
				this,
				getCurrentClauseStack()::getCurrent
		);
		pushProcessingState( queryNodeProcessingState );

		try {
			consumeFromClauseRoot( sqmTarget );
			final TableGroup rootTableGroup = getFromClauseAccess().getTableGroup( sqmTarget.getNavigablePath() );

			Predicate suppliedPredicate = null;
			final SqmWhereClause whereClause = statement.getWhereClause();
			if ( whereClause != null ) {
				suppliedPredicate = visitWhereClause( whereClause.getPredicate() );
			}

			return new DeleteStatement(
					cteContainer,
					(NamedTableReference) rootTableGroup.getPrimaryTableReference(),
					fromClause,
					combinePredicates(
							suppliedPredicate,
							combinePredicates(
									queryNodeProcessingState.getPredicate(),
									additionalRestrictions
							)
					),
					Collections.emptyList()
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
		final CteContainer oldCteContainer = cteContainer;
		final CteContainer cteContainer = this.visitCteContainer( sqmStatement );
		final SqmStatement<?> oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = sqmStatement;
		final String entityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister entityDescriptor = creationContext.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
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
		final InsertSelectStatement insertStatement;
		final AdditionalInsertValues additionalInsertValues;
		try {
			final NavigablePath rootPath = sqmStatement.getTarget().getNavigablePath();
			final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
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
					Collections.emptyList()
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
					final boolean appliedRowNumber = additionalInsertValues.applySelections(
							querySpec,
							creationContext.getSessionFactory()
					);
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
		if ( !rootTableGroup.getTableReferenceJoins().isEmpty() ) {
			return true;
		}
		return hasJoins( rootTableGroup.getTableGroupJoins() ) || hasJoins( rootTableGroup.getNestedTableGroupJoins() );
	}

	private static boolean hasJoins(List<TableGroupJoin> tableGroupJoins) {
		for ( TableGroupJoin tableGroupJoin : tableGroupJoins ) {
			final TableGroup joinedGroup = tableGroupJoin.getJoinedGroup();
			if ( !joinedGroup.isInitialized() ) {
				continue;
			}
			else if ( joinedGroup.isVirtual() ) {
				return hasJoins( joinedGroup );
			}
			return true;
		}
		return false;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Insert-values statement

	@Override
	public InsertStatement visitInsertValuesStatement(SqmInsertValuesStatement<?> sqmStatement) {
		final CteContainer oldCteContainer = cteContainer;
		final CteContainer cteContainer = this.visitCteContainer( sqmStatement );
		final SqmStatement<?> oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = sqmStatement;
		final String entityName = sqmStatement.getTarget().getEntityName();
		final EntityPersister entityDescriptor = creationContext.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
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
					null,
					() -> predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
					this
			);

			getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );

			final InsertSelectStatement insertStatement = new InsertSelectStatement(
					cteContainer,
					(NamedTableReference) rootTableGroup.getPrimaryTableReference(),
					Collections.emptyList()
			);

			final AdditionalInsertValues additionalInsertValues = visitInsertionTargetPaths(
					(assigable, references) -> insertStatement.addTargetColumnReferences( references ),
					sqmStatement,
					entityDescriptor,
					rootTableGroup
			);

			if ( hasJoins( rootTableGroup ) ) {
				throw new HibernateException( "Not expecting multiple table references for an SQM INSERT-SELECT" );
			}

			for ( SqmValues sqmValues : sqmStatement.getValuesList() ) {
				final Values values = visitValues( sqmValues );
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
		final List<SqmPath<?>> constraintAttributes = sqmConflictClause.getConstraintPaths();
		final List<String> constraintColumnNames = new ArrayList<>( constraintAttributes.size() );
		for ( SqmPath<?> constraintAttribute : constraintAttributes ) {
			final Assignable assignable = ( (Assignable) constraintAttribute.accept( this ) );
			for ( ColumnReference columnReference : assignable.getColumnReferences() ) {
				constraintColumnNames.add( columnReference.getSelectableName() );
			}
		}
		final SqmConflictUpdateAction<?> updateAction = sqmConflictClause.getConflictAction();
		final List<Assignment> assignments;
		final Predicate predicate;
		if ( updateAction == null ) {
			assignments = Collections.emptyList();
			predicate = null;
		}
		else {
			final SqmRoot<?> excludedRoot = sqmConflictClause.getExcludedRoot();
			final EntityPersister entityDescriptor = resolveEntityPersister( excludedRoot.getModel() );
			final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
					true,
					excludedRoot.getNavigablePath(),
					excludedRoot.getExplicitAlias(),
					new SqlAliasBaseConstant( "excluded" ),
					() -> null,
					this
			);
			registerSqmFromTableGroup( excludedRoot, tableGroup );
			assignments = visitSetClause( updateAction.getSetClause() );
			final SqmWhereClause whereClause = updateAction.getWhereClause();
			predicate = whereClause == null ? null : visitWhereClause( whereClause.getPredicate() );
		}

		return new ConflictClause( sqmConflictClause.getConstraintName(), constraintColumnNames, assignments, predicate );
	}

	public AdditionalInsertValues visitInsertionTargetPaths(
			BiConsumer<Assignable, List<ColumnReference>> targetColumnReferenceConsumer,
			SqmInsertStatement<?> sqmStatement,
			EntityPersister entityDescriptor, TableGroup rootTableGroup) {
		final List<SqmPath<?>> targetPaths = sqmStatement.getInsertionTargetPaths();
		final EntityDiscriminatorMapping discriminatorMapping = entityDescriptor.getDiscriminatorMapping();
		Expression versionExpression = null;
		Expression discriminatorExpression = null;
		BasicEntityIdentifierMapping identifierMapping = null;
		// We use the id property name to null the identifier generator variable if the target paths contain the id
		final String identifierPropertyName;
		Generator identifierGenerator = entityDescriptor.getGenerator();
		identifierPropertyName = identifierGenerator != null ? entityDescriptor.getIdentifierPropertyName() : null;
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
			final SqmPath<?> path = targetPaths.get( i );
			final String localName = path.getNavigablePath().getLocalName();
			if ( localName.equals( identifierPropertyName ) ) {
				identifierGenerator = null;
			}
			else if ( localName.equals( versionAttributeName ) ) {
				needsVersionInsert = false;
			}
			final Assignable assignable = (Assignable) path.accept( this );
			targetColumnReferenceConsumer.accept( assignable, assignable.getColumnReferences() );
		}
		if ( needsVersionInsert ) {
			final BasicValuedPathInterpretation<?> versionPath = BasicValuedPathInterpretation.from(
					(SqmBasicValuedSimplePath<?>) sqmStatement.getTarget()
							.get( versionAttributeName ),
					this,
					jpaQueryComplianceEnabled
			);
			final List<ColumnReference> targetColumnReferences = versionPath.getColumnReferences();
			assert targetColumnReferences.size() == 1;

			targetColumnReferenceConsumer.accept( versionPath, targetColumnReferences );
			versionExpression = new VersionTypeSeedParameterSpecification( entityDescriptor.getVersionMapping() );
		}
		if ( discriminatorMapping != null && discriminatorMapping.hasPhysicalColumn() ) {
			final BasicValuedPathInterpretation<?> discriminatorPath = new BasicValuedPathInterpretation<>(
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
					entityDescriptor.getDiscriminatorValue(),
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
			else if ( !( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator ) ) {
				// For non-identity generators that don't implement BulkInsertionCapableIdentifierGenerator, there is nothing we can do
				addIdColumn = false;
			}
			else {
				// Same condition as in AdditionalInsertValues#applySelections
				final Optimizer optimizer;
				if ( identifierGenerator instanceof OptimizableGenerator
						&& ( optimizer = ( (OptimizableGenerator) identifierGenerator ).getOptimizer() ) != null
						&& optimizer.getIncrementSize() > 1
						|| !( (BulkInsertionCapableIdentifierGenerator) identifierGenerator ).supportsBulkInsertionIdentifierGeneration() ) {
					// If the dialect does not support window functions, we don't need the id column in the temporary table insert
					// because we will make use of the special "rn_" column that is auto-incremented and serves as temporary identifier for a row,
					// which is needed to control the generation of proper identifier values with the generator afterwards
					addIdColumn = creationContext.getSessionFactory().getJdbcServices().getDialect().supportsWindowFunctions();
				}
				else {
					// If the generator supports bulk insertion and the optimizer uses an increment size of 1,
					// we can list the column, because we can emit a SQL expression.
					addIdColumn = true;
				}
			}
			identifierMapping = (BasicEntityIdentifierMapping) entityDescriptor.getIdentifierMapping();
			if ( addIdColumn ) {
				final BasicValuedPathInterpretation<?> identifierPath = new BasicValuedPathInterpretation<>(
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
							new IdGeneratorParameter( identifierMapping, (BeforeExecutionGenerator) identifierGenerator );
				}
				expressions.add( identifierGeneratorParameter );
			}
		}

		/**
		 * Returns true if the identifier can't be applied directly and needs to be generated separately.
		 * As a replacement for the identifier, the special row_number column should be filled.
		 */
		public boolean applySelections(QuerySpec querySpec, SessionFactoryImplementor sessionFactory) {
			final SelectClause selectClause = querySpec.getSelectClause();
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
					if ( !( identifierGenerator instanceof BulkInsertionCapableIdentifierGenerator ) ) {
						throw new SemanticException(
								"SQM INSERT-SELECT without bulk insertion capable identifier generator: " + identifierGenerator );
					}
					if ( identifierGenerator instanceof OptimizableGenerator ) {
						final Optimizer optimizer = ( (OptimizableGenerator) identifierGenerator ).getOptimizer();
						if ( optimizer != null && optimizer.getIncrementSize() > 1
								|| !( (BulkInsertionCapableIdentifierGenerator) identifierGenerator ).supportsBulkInsertionIdentifierGeneration() ) {
							// This is a special case where we have a sequence with an optimizer
							// or a table based identifier generator
							if ( !sessionFactory.getJdbcServices().getDialect().supportsWindowFunctions() ) {
								return false;
							}
							identifierSelection = new SqlSelectionImpl(
									SqmInsertStrategyHelper.createRowNumberingExpression( querySpec, sessionFactory )
							);
							selectClause.addSqlSelection( identifierSelection );
							return true;
						}
					}
					final String fragment = ( (BulkInsertionCapableIdentifierGenerator) identifierGenerator )
							.determineBulkInsertionIdentifierGenerationSelectFragment(
									sessionFactory.getSqlStringGenerationContext()
							);
					// The position is irrelevant as this is only needed for insert
					identifierSelection = new SqlSelectionImpl(
							new SelfRenderingSqlFragmentExpression( fragment )
					);
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
		final List<SqmPath<?>> insertionTargetPaths;
		if ( currentSqmStatement instanceof SqmInsertStatement<?> ) {
			insertionTargetPaths = ( (SqmInsertStatement<?>) currentSqmStatement ).getInsertionTargetPaths();
		}
		else {
			insertionTargetPaths = null;
		}
		final List<SqmExpression<?>> expressions = sqmValues.getExpressions();
		final ArrayList<Expression> valuesExpressions = new ArrayList<>( expressions.size() );
		for ( int i = 0; i < expressions.size(); i++ ) {
			// todo: add WriteExpression handling
			valuesExpressions.add(
					insertionTargetPaths == null
							? (Expression) expressions.get( i ).accept( this )
							: visitWithInferredType( expressions.get( i ), insertionTargetPaths.get( i ) )
			);
		}
		return new Values( valuesExpressions );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Select statement

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement<?> statement) {
		final CteContainer oldCteContainer = cteContainer;
		final CteContainer cteContainer = this.visitCteContainer( statement );
		final SqmStatement<?> oldSqmStatement = this.currentSqmStatement;

		this.currentSqmStatement = statement;
		final QueryPart queryPart = visitQueryPart( statement.getQueryPart() );
		final List<DomainResult<?>> domainResults = queryPart.isRoot() ? this.domainResults : Collections.emptyList();
		try {
			return new SelectStatement( cteContainer, queryPart, domainResults );
		}
		finally {
			this.currentSqmStatement = oldSqmStatement;
			this.cteContainer = oldCteContainer;
		}
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
			if ( selectableNode instanceof SqmPath<?> ) {
				prepareForSelection( (SqmPath<?>) selectableNode );
			}
			final DomainResultProducer<?> argumentResultProducer = (DomainResultProducer<?>) sqmArgument.accept( this );

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

		return getCreationContext().getMappingMetamodel()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
				.getDescriptor( targetJavaType );
	}

	@Override
	public CteStatement visitCteStatement(SqmCteStatement<?> sqmCteStatement) {
		final SqmCteTable<?> sqmCteTable = sqmCteStatement.getCteTable();
		final String cteName = getCteName( sqmCteTable );
		final SqmSelectQuery<?> selectStatement = sqmCteStatement.getCteDefinition();
		final SqmQueryPart<?> queryPart = selectStatement.getQueryPart();
		final Literal cycleLiteral = getLiteral( sqmCteStatement.getCycleLiteral() );
		final Literal noCycleLiteral = getLiteral( sqmCteStatement.getNoCycleLiteral() );
		final JdbcMapping cycleMarkType = cycleLiteral == null ? null : cycleLiteral.getJdbcMapping();
		final BasicType<String> stringType = creationContext.getSessionFactory()
				.getTypeConfiguration()
				.getBasicTypeForJavaType( String.class );
		if ( queryPart instanceof SqmQueryGroup<?> && queryPart.getSortSpecifications().isEmpty()
				&& queryPart.getFetchExpression() == null && queryPart.getOffsetExpression() == null ) {
			final SqmQueryGroup<?> queryGroup = (SqmQueryGroup<?>) queryPart;
			switch ( queryGroup.getSetOperator() ) {
				case UNION:
				case UNION_ALL:
					if ( queryGroup.getQueryParts().size() == 2 ) {
						// This could potentially be a recursive CTE,
						// for which we need to visit the non-recursive part first
						// and register a CteStatement before visiting the recursive part.
						// This is important, because the recursive part will refer to the CteStatement,
						// hence we require that it is registered already
						final CteContainer oldCteContainer = cteContainer;
						final CteContainer subCteContainer = this.visitCteContainer( selectStatement );
						// Note that the following is a trimmed down version of what visitQueryGroup does
						try {
							final SqmQueryPart<?> firstPart = queryGroup.getQueryParts().get( 0 );
							final SqmQueryPart<?> secondPart = queryGroup.getQueryParts().get( 1 );
							final List<QueryPart> newQueryParts = new ArrayList<>( 2 );
							final QueryGroup group = new QueryGroup(
									getProcessingStateStack().isEmpty(),
									queryGroup.getSetOperator(),
									newQueryParts
							);

							final SqlAstQueryPartProcessingStateImpl processingState = new SqlAstQueryPartProcessingStateImpl(
									group,
									getCurrentProcessingState(),
									this,
									DelegatingSqmAliasedNodeCollector::new,
									currentClauseStack::getCurrent,
									deduplicateSelectionItems
							);
							final DelegatingSqmAliasedNodeCollector collector = (DelegatingSqmAliasedNodeCollector) processingState
									.getSqlExpressionResolver();
							sqmQueryPartStack.push( queryGroup );
							pushProcessingState( processingState );

							try {
								newQueryParts.add( visitQueryPart( firstPart ) );

								collector.setSqmAliasedNodeCollector(
										(SqmAliasedNodeCollector) lastPoppedProcessingState.getSqlExpressionResolver()
								);

								// Before visiting the second query part, setup the CteStatement and register it
								final CteTable cteTable = new CteTable(
										cteName,
										sqmCteTable.resolveTableGroupProducer(
												cteName,
												newQueryParts.get( 0 )
														.getFirstQuerySpec()
														.getSelectClause()
														.getSqlSelections(),
												lastPoppedFromClauseIndex
										)
								);

								final CteStatement cteStatement = new CteStatement(
										cteTable,
										new SelectStatement( subCteContainer, group, Collections.emptyList() ),
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
		if ( selectStatement instanceof SqmSubQuery<?> ) {
			statement = visitSubQueryExpression( (SqmSubQuery<?>) selectStatement );
		}
		else {
			statement = visitSelectStatement( (SqmSelectStatement<?>) selectStatement );
		}

		final CteTable cteTable = new CteTable(
				cteName,
				sqmCteTable.resolveTableGroupProducer(
						cteName,
						statement.getQuerySpec().getSelectClause().getSqlSelections(),
						lastPoppedFromClauseIndex
				)
		);

		final CteStatement cteStatement = new CteStatement(
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
		final String cteName = name != null
				? generateCteName( name )
				: generateCteName( "cte" + cteNameMapping.size() );
		cteNameMapping.put( key, cteName );
		return cteName;
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
		final int size = searchBySpecifications.size();
		final List<SearchClauseSpecification> searchClauseSpecifications = new ArrayList<>( size );
		for ( int i = 0; i < size; i++ ) {
			final JpaSearchOrder specification = searchBySpecifications.get( i );
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

	protected CteColumn createCteColumn(String cteColumn, JdbcMapping jdbcMapping) {
		if ( cteColumn == null ) {
			return null;
		}
		return new CteColumn( cteColumn, jdbcMapping );
	}

	protected void forEachCteColumn(CteTable cteTable, SqmCteTableColumn cteColumn, Consumer<CteColumn> consumer) {
		final List<CteColumn> cteColumns = cteTable.getCteColumns();
		final int size = cteColumns.size();
		for ( int i = 0; i < size; i++ ) {
			final CteColumn column = cteColumns.get( i );
			final String columnName = column.getColumnExpression();
			final String sqmName = cteColumn.getName();
			if ( columnName.regionMatches( 0, sqmName, 0, sqmName.length() )
					&& ( columnName.length() == sqmName.length()
					|| columnName.charAt( sqmName.length() ) == '_' ) ) {
				consumer.accept( column );
			}
		}
	}

	protected List<CteColumn> visitCycleColumns(CteTable cteTable, List<JpaCteCriteriaAttribute> cycleColumns) {
		if ( cycleColumns == null || cycleColumns.isEmpty() ) {
			return null;
		}
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

	@Override
	public CteContainer visitCteContainer(SqmCteContainer consumer) {
		final Collection<SqmCteStatement<?>> sqmCteStatements = consumer.getCteStatements();
		cteContainer = new CteContainerImpl( cteContainer );
		if ( !sqmCteStatements.isEmpty() ) {
			currentClauseStack.push( Clause.WITH );
			for ( SqmCteStatement<?> sqmCteStatement : sqmCteStatements ) {
				visitCteStatement( sqmCteStatement );
			}
			currentClauseStack.pop();
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
		final List<? extends SqmQueryPart<?>> queryParts = queryGroup.getQueryParts();
		final int size = queryParts.size();
		final List<QueryPart> newQueryParts = new ArrayList<>( size );
		final QueryGroup group = new QueryGroup(
				getProcessingStateStack().isEmpty(),
				queryGroup.getSetOperator(),
				newQueryParts
		);

		final Map<NavigablePath, Map.Entry<Integer, List<SqlSelection>>> originalTrackedFetchSelectionsForGroup = this.trackedFetchSelectionsForGroup;
		if ( queryGroup.getOrderByClause() != null && queryGroup.getOrderByClause().hasPositionalSortItem() ) {
			trackSelectionsForGroup = true;
			// Find the order by elements which refer to attributes of the selections
			// and register the navigable paths so that a list of SqlSelection is tracked for the fetch
			Map<NavigablePath, Map.Entry<Integer, List<SqlSelection>>> trackedFetchSelectionsForGroup = null;
			for ( SqmSortSpecification sortSpecification : queryGroup.getOrderByClause().getSortSpecifications() ) {
				if ( sortSpecification.getExpression() instanceof SqmAliasedNodeRef ) {
					final SqmAliasedNodeRef nodeRef = (SqmAliasedNodeRef) sortSpecification.getExpression();
					if ( nodeRef.getNavigablePath() != null ) {
						if ( trackedFetchSelectionsForGroup == null ) {
							trackedFetchSelectionsForGroup = new HashMap<>();
						}
						trackedFetchSelectionsForGroup.put( nodeRef.getNavigablePath(), new AbstractMap.SimpleEntry<>( nodeRef.getPosition() - 1, new ArrayList<>() ) );
					}
				}
			}

			this.trackedFetchSelectionsForGroup = trackedFetchSelectionsForGroup == null
					? Collections.emptyMap()
					: trackedFetchSelectionsForGroup;
		}

		final SqlAstQueryPartProcessingStateImpl processingState = new SqlAstQueryPartProcessingStateImpl(
				group,
				getCurrentProcessingState(),
				this,
				DelegatingSqmAliasedNodeCollector::new,
				currentClauseStack::getCurrent,
				deduplicateSelectionItems
		);
		final DelegatingSqmAliasedNodeCollector collector = (DelegatingSqmAliasedNodeCollector) processingState
				.getSqlExpressionResolver();
		sqmQueryPartStack.push( queryGroup );
		pushProcessingState( processingState );

		FromClauseIndex firstQueryPartIndex = null;
		SqlAstProcessingState firstPoppedProcessingState = null;
		try {
			newQueryParts.add( visitQueryPart( queryParts.get( 0 ) ) );

			firstQueryPartIndex = lastPoppedFromClauseIndex;
			firstPoppedProcessingState = lastPoppedProcessingState;
			collector.setSqmAliasedNodeCollector(
					(SqmAliasedNodeCollector) lastPoppedProcessingState.getSqlExpressionResolver()
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
		final QuerySpec sqlQuerySpec = new QuerySpec(
				topLevel,
				sqmQuerySpec.getFromClause().getNumberOfRoots()
		);
		final SqmSelectClause selectClause = sqmQuerySpec.getSelectClause();

		final Predicate originalAdditionalRestrictions = additionalRestrictions;
		additionalRestrictions = null;
		final boolean oldInNestedContext = inNestedContext;
		inNestedContext = false;

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

		final SqlAstQueryPartProcessingStateImpl processingState;
		if ( trackAliasedNodePositions ) {
			processingState = new SqlAstQueryPartProcessingStateImpl(
					sqlQuerySpec,
					getCurrentProcessingState(),
					this,
					r -> new SqmAliasedNodePositionTracker(
							r,
							selectClause.getSelections()
					),
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
			// we want to visit the from-clause first
			visitFromClause( sqmQuerySpec.getFromClause() );

			visitSelectClause( selectClause );

			final SqmWhereClause whereClause = sqmQuerySpec.getWhereClause();
			if ( whereClause != null ) {
				sqlQuerySpec.applyPredicate( visitWhereClause( whereClause.getPredicate() ) );
			}

			sqlQuerySpec.setGroupByClauseExpressions( visitGroupByClause( sqmQuerySpec.getGroupByClauseExpressions() ) );
			if ( sqmQuerySpec.getHavingClausePredicate() != null ) {
				sqlQuerySpec.setHavingClauseRestrictions( visitHavingClause( sqmQuerySpec.getHavingClausePredicate() ) );
			}

			visitOrderByOffsetAndFetch( sqmQuerySpec, sqlQuerySpec );

			if ( topLevel && statement instanceof SqmSelectStatement<?> ) {
				if ( orderByFragments != null ) {
					orderByFragments.forEach(
							entry -> entry.getKey().apply(
									sqlQuerySpec,
									entry.getValue(),
									this
							)
					);
					orderByFragments = null;
				}
			}

			// Look for treated SqmFrom registrations that have uses of the untreated SqmFrom.
			// These SqmFrom nodes are then not treat-joined but rather treated only in expressions
			// Consider the following two queries. The latter also uses the untreated SqmFrom
			// and hence has different semantics i.e. the treat is not filtering, but just applies where it is used
			// - select a.id from Root r join treat(r.attribute as Subtype) a where a.id = 1
			// - select a.id from Root r join r.attribute a where treat(a as Subtype).id = 1
			for ( Map.Entry<SqmFrom<?, ?>, Boolean> entry : processingState.getFromRegistrations().entrySet() ) {
				if ( entry.getValue() == Boolean.TRUE ) {
					downgradeTreatUses( getFromClauseIndex().getTableGroup( entry.getKey().getNavigablePath() ) );
				}
			}

			QuerySpec finalQuerySpec = sqlQuerySpec;
			for ( QueryTransformer transformer : (List<QueryTransformer>) queryTransformers.getCurrent() ) {
				finalQuerySpec = transformer.transform(
						cteContainer,
						finalQuerySpec,
						this
				);
			}
			return finalQuerySpec;
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

	private void downgradeTreatUses(TableGroup tableGroup) {
		final Map<String, EntityNameUse> entityNameUses = tableGroupEntityNameUses.get( tableGroup );
		if ( entityNameUses != null ) {
			for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
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
				for ( SqmSortSpecification sortSpecification : sqmQueryPart.getOrderByClause()
						.getSortSpecifications() ) {
					final SortSpecification specification = visitSortSpecification( sortSpecification );
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

		if ( !containsCollectionFetches || !currentClauseStack.isEmpty() ) {
			// Strip off the root offset and limit expressions in case the query contains collection fetches to retain
			// the proper cardinality. We could implement pagination for single select statements differently in this
			// case by using a subquery e.g. `... where alias in (select subAlias from ... limit ...)`
			// or use window functions e.g. `select ... from (select ..., dense_rank() over(order by ..., id) rn from ...) tmp where tmp.rn between ...`
			// but these transformations/translations are non-trivial and can be done later
			inferrableTypeAccessStack.push( () -> getTypeConfiguration().getBasicTypeForJavaType( Integer.class ) );
			sqlQueryPart.setOffsetClauseExpression( visitOffsetExpression( sqmQueryPart.getOffsetExpression() ) );
			if ( sqmQueryPart.getFetchClauseType() == FetchClauseType.PERCENT_ONLY
					|| sqmQueryPart.getFetchClauseType() == FetchClauseType.PERCENT_WITH_TIES ) {
				inferrableTypeAccessStack.pop();
				inferrableTypeAccessStack.push( () -> getTypeConfiguration().getBasicTypeForJavaType( Double.class ) );
			}
			sqlQueryPart.setFetchClauseExpression(
					visitFetchExpression( sqmQueryPart.getFetchExpression() ),
					sqmQueryPart.getFetchClauseType()
			);
			inferrableTypeAccessStack.pop();
		}
	}

	private TableGroup findTableGroupByPath(NavigablePath navigablePath) {
		return getFromClauseAccess().getTableGroup( navigablePath );
	}

	@Override
	public SelectClause visitSelectClause(SqmSelectClause selectClause) {
		currentClauseStack.push( Clause.SELECT );
		try {
			final SelectClause sqlSelectClause = currentQuerySpec().getSelectClause();
			if ( selectClause == null ) {
				final SqmFrom<?, ?> implicitSelection = determineImplicitSelection( (SqmQuerySpec<?>) getCurrentSqmQueryPart() );
				visitSelection( 0, new SqmSelection<>( implicitSelection, implicitSelection.nodeBuilder() ) );
			}
			else {
				final List<SqmSelection<?>> selections = selectClause.getSelections();
				for ( int i = 0; i < selections.size(); i++ ) {
					visitSelection( i, selections.get( i ) );
				}
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
		return visitSelection(
				getCurrentSqmQueryPart().getFirstQuerySpec().getSelectClause().getSelections().indexOf( sqmSelection ),
				sqmSelection
		);
	}

	public Void visitSelection(int index, SqmSelection<?> sqmSelection) {
		final boolean contributesToTopLevelSelectClause = currentClauseStack.depth() == 1 && currentClauseStack.getCurrent() == Clause.SELECT;
		// Only infer the type on the "top level" select clauses
		final boolean inferTargetPath = statement instanceof SqmInsertSelectStatement<?> && contributesToTopLevelSelectClause;
		// todo: add WriteExpression handling
		if ( inferTargetPath ) {
			final SqmPath<?> path = ( (SqmInsertSelectStatement<?>) statement ).getInsertionTargetPaths().get( index );
			inferrableTypeAccessStack.push( () -> determineValueMapping( path ) );
		}
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
			resultProducers = singletonList(
					new AbstractMap.SimpleEntry<>(
						sqmSelection.getAlias(),
						(DomainResultProducer<?>) selectionNode.accept( this )
					)
			);
		}

		final Stack<SqlAstProcessingState> processingStateStack = getProcessingStateStack();
		final boolean needsDomainResults = domainResults != null && contributesToTopLevelSelectClause;
		final boolean collectDomainResults;
		if ( processingStateStack.depth() == 1 ) {
			collectDomainResults = needsDomainResults;
		}
		else {
			final SqlAstProcessingState current = processingStateStack.getCurrent();
			// Since we only want to create domain results for the first/left-most query spec within query groups,
			// we have to check if the current query spec is the left-most.
			// This is the case when all upper level in-flight query groups are still empty
			collectDomainResults = needsDomainResults && processingStateStack.findCurrentFirstWithParameter( current, BaseSqmToSqlAstConverter::stackMatchHelper ) == null;
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
		if ( inferTargetPath ) {
			inferrableTypeAccessStack.pop();
		}
		return null;
	}

	protected Expression resolveGroupOrOrderByExpression(SqmExpression<?> groupByClauseExpression) {
		final int sqmPosition;
		final NavigablePath path;
		if ( groupByClauseExpression instanceof SqmAliasedNodeRef ) {
			final SqmAliasedNodeRef aliasedNodeRef = (SqmAliasedNodeRef) groupByClauseExpression;
			final int aliasedNodeOrdinal = aliasedNodeRef.getPosition();
			sqmPosition = aliasedNodeOrdinal - 1;
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
			final SqmQuerySpec<?> querySpec = getCurrentSqmQueryPart().getFirstQuerySpec();
			sqmPosition = indexOfExpression( querySpec.getSelectClause().getSelections(), groupByClauseExpression );
			path = null;
		}
		else {
			sqmPosition = -1;
			path = null;
		}
		if ( sqmPosition != -1 ) {
			final List<SqlSelection> selections;
			if ( path == null ) {
				selections = currentSqlSelectionCollector().getSelections( sqmPosition );
			}
			else {
				selections = trackedFetchSelectionsForGroup.get( path ).getValue();
			}
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
				if ( getCurrentSqmQueryPart() instanceof SqmQueryGroup<?> ) {
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
											),
											false
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
		if ( result < 0 ) {
			return -1;
		}
		else {
			return result;
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
				if ( subResult >= 0 ) {
					return subResult;
				}
				offset = -subResult - i;
			}
			else if ( selectableNode instanceof SqmJpaCompoundSelection<?> ) {
				final List<SqmSelectableNode<?>> selectionItems = ( (SqmJpaCompoundSelection<?>) selectableNode ).getSelectionItems();
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
				for ( SqmExpression<?> groupByClauseExpression : groupByClauseExpressions ) {
					expressions.add( resolveGroupOrOrderByExpression( groupByClauseExpression ) );
				}
				return expressions;
			}
			finally {
				inferrableTypeAccessStack.pop();
				currentClauseStack.pop();
			}
		}
		return Collections.emptyList();
	}

	@Override
	public Predicate visitWhereClause(SqmWhereClause whereClause) {
		if ( whereClause == null ) {
			return null;
		}
		return visitWhereClause( whereClause.getPredicate() );
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
		final Expression expression = resolveGroupOrOrderByExpression( sortSpecification.getSortExpression() );
		if ( expression == null ) {
			return null;
		}
		return new SortSpecification(
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
		log.tracef( "Resolving SqmRoot [%s] to TableGroup", sqmRoot );
		final FromClauseIndex fromClauseIndex = getFromClauseIndex();
		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
			log.tracef( "Already resolved SqmRoot [%s] to TableGroup", sqmRoot );
		}
		final TableGroup tableGroup;
		if ( !sqmRoot.isCorrelated() ) {
			return;
		}
		final QuerySpec currentQuerySpec = currentQuerySpec();
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
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
			if ( parentTableGroup == null ) {
				throw new InterpretationException( "Access to from node '" + from.getCorrelationParent() + "' is not possible in from-clause subqueries, unless the 'lateral' keyword is used for the subquery!" );
			}
			final SqlAliasBase sqlAliasBase = sqlAliasBaseManager.createSqlAliasBase( parentTableGroup.getGroupAlias() );
			if ( parentTableGroup instanceof PluralTableGroup ) {
				final PluralTableGroup pluralTableGroup = (PluralTableGroup) parentTableGroup;
				final CorrelatedPluralTableGroup correlatedPluralTableGroup = new CorrelatedPluralTableGroup(
						parentTableGroup,
						sqlAliasBase,
						currentQuerySpec,
						predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
						sessionFactory
				);
				final TableGroup elementTableGroup = pluralTableGroup.getElementTableGroup();
				if ( elementTableGroup != null ) {
					final TableGroup correlatedElementTableGroup = new CorrelatedTableGroup(
							elementTableGroup,
							sqlAliasBase,
							currentQuerySpec,
							predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
							sessionFactory
					);
					final TableGroupJoin tableGroupJoin = new TableGroupJoin(
							elementTableGroup.getNavigablePath(),
							SqlAstJoinType.INNER,
							correlatedElementTableGroup
					);
					correlatedPluralTableGroup.registerElementTableGroup( tableGroupJoin );
				}
				final TableGroup indexTableGroup = pluralTableGroup.getIndexTableGroup();
				if ( indexTableGroup != null ) {
					final TableGroup correlatedIndexTableGroup = new CorrelatedTableGroup(
							indexTableGroup,
							sqlAliasBase,
							currentQuerySpec,
							predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
							sessionFactory
					);
					final TableGroupJoin tableGroupJoin = new TableGroupJoin(
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

			log.tracef( "Resolved SqmRoot [%s] to correlated TableGroup [%s]", sqmRoot, tableGroup );
			consumeExplicitJoins( from, tableGroup );
			return;
		}
		else {
			final EntityPersister entityDescriptor = resolveEntityPersister( sqmRoot.getModel() );
			final TableGroup parentTableGroup = fromClauseIndex.findTableGroup(
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
			final EntityIdentifierMapping identifierMapping = entityDescriptor.getIdentifierMapping();
			final NavigablePath navigablePath = sqmRoot.getNavigablePath().append( identifierMapping.getNavigableRole().getNavigableName() );
			final int jdbcTypeCount = identifierMapping.getJdbcTypeCount();
			if ( jdbcTypeCount == 1 ) {
				identifierMapping.forEachSelectable(
						(index, selectable) -> additionalRestrictions = combinePredicates(
								additionalRestrictions,
								new ComparisonPredicate(
										new ColumnReference(
												parentTableGroup.resolveTableReference( navigablePath, selectable.getContainingTableExpression() ),
												selectable
										),
										ComparisonOperator.EQUAL,
										new ColumnReference(
												tableGroup.resolveTableReference( navigablePath, selectable.getContainingTableExpression() ),
												selectable
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
											parentTableGroup.resolveTableReference( navigablePath, selectable.getContainingTableExpression() ),
											selectable
									)
							);
							rhs.add(
									new ColumnReference(
											tableGroup.resolveTableReference( navigablePath, selectable.getContainingTableExpression() ),
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

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, tableGroup );

		fromClauseIndex.register( sqmRoot, tableGroup );
		currentQuerySpec.getFromClause().addRoot( tableGroup );

		consumeJoins( sqmRoot, fromClauseIndex, tableGroup );
	}

	protected void consumeFromClauseRoot(SqmRoot<?> sqmRoot) {
		log.tracef( "Resolving SqmRoot [%s] to TableGroup", sqmRoot );
		final FromClauseIndex fromClauseIndex = getFromClauseIndex();
		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
			log.tracef( "Already resolved SqmRoot [%s] to TableGroup", sqmRoot );
		}
		if ( sqmRoot.isCorrelated() ) {
			return;
		}
		final SqlAstQueryNodeProcessingState currentQueryNodeProcessingState = currentQueryNodeProcessingState();
		final TableGroup tableGroup;
		if ( sqmRoot instanceof SqmDerivedRoot<?> ) {
			final SqmDerivedRoot<?> derivedRoot = (SqmDerivedRoot<?>) sqmRoot;
			// Temporarily push an empty FromClauseIndex to disallow access to aliases from the top query
			// Only lateral subqueries are allowed to see the aliases
			fromClauseIndexStack.push( new FromClauseIndex( null ) );
			final SelectStatement statement = (SelectStatement) derivedRoot.getQueryPart().accept( this );
			fromClauseIndexStack.pop();
			final AnonymousTupleType<?> tupleType = (AnonymousTupleType<?>) sqmRoot.getNodeType();
			final List<SqlSelection> sqlSelections = statement.getQueryPart().getFirstQuerySpec().getSelectClause().getSqlSelections();
			final AnonymousTupleTableGroupProducer tableGroupProducer = tupleType.resolveTableGroupProducer(
					derivedRoot.getExplicitAlias(),
					sqlSelections,
					lastPoppedFromClauseIndex
			);
			final List<String> columnNames = tupleType.determineColumnNames();
			final SqlAliasBase sqlAliasBase = getSqlAliasBaseGenerator().createSqlAliasBase(
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
		else if ( sqmRoot instanceof SqmCteRoot<?> ) {
			final SqmCteRoot<?> cteRoot = (SqmCteRoot<?>) sqmRoot;
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

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, tableGroup );

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

	private TableGroup createCteTableGroup(
			String cteName,
			NavigablePath navigablePath,
			String explicitAlias,
			boolean canUseInnerJoins) {
		final SqlAliasBase sqlAliasBase = getSqlAliasBaseGenerator().createSqlAliasBase(
				explicitAlias == null ? cteName : explicitAlias
		);
		final String identifierVariable = sqlAliasBase.generateNewAlias();
		final CteStatement cteStatement = cteContainer.getCteStatement( cteName );
		if ( cteStatement == null ) {
			throw new InterpretationException( "Could not find CTE for name '" + cteName + "'!" );
		}
		final QueryPart cteQueryPart = ( (SelectStatement) cteStatement.getCteDefinition() ).getQueryPart();
		// If the query part of the CTE is one which we are currently processing, then this is a recursive CTE
		if ( cteQueryPart instanceof QueryGroup && Boolean.TRUE == processingStateStack.findCurrentFirstWithParameter( cteQueryPart, BaseSqmToSqlAstConverter::matchSqlAstWithQueryPart ) ) {
			cteStatement.setRecursive();
		}
		final AnonymousTupleTableGroupProducer tableGroupProducer = cteStatement.getCteTable().getTableGroupProducer();
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
				final TableGroup actualTableGroup = getActualTableGroup( ownerTableGroup, join );
				lastTableGroup = consumeExplicitJoin( join, lastTableGroup, actualTableGroup, false );
			}
		}
	}

	private EntityPersister resolveEntityPersister(EntityDomainType<?> entityDomainType) {
		return creationContext.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityDomainType.getHibernateEntityName() );
	}

	/**
	 * Registers {@link EntityNameUse#PROJECTION} entity name uses for all entity valued path subtypes.
	 * If the path is a treat, registers {@link EntityNameUse#TREAT} for all treated subtypes instead.
	 */
	private void registerEntityNameProjectionUsage(SqmPath<?> projectedPath, TableGroup tableGroup) {
		final ManagedDomainType<?> treatedType;
		if ( projectedPath instanceof SqmTreatedPath<?, ?> ) {
			treatedType = ( (SqmTreatedPath<?, ?>) projectedPath ).getTreatTarget();
			registerEntityNameUsage( tableGroup, EntityNameUse.TREAT, treatedType.getTypeName(), true );

			if ( projectedPath instanceof SqmFrom<?, ?> ) {
				// Register that the TREAT uses for the SqmFrom node may not be downgraded
				( (SqlAstQueryPartProcessingState) getCurrentProcessingState() ).registerFromUsage(
						(SqmFrom<?, ?>) ( (SqmTreatedPath<?, ?>) projectedPath ).getWrappedPath(),
						false
				);
			}
		}
		else if ( projectedPath.getNodeType().getSqmPathType() instanceof EntityDomainType<?> ) {
			treatedType = (EntityDomainType<?>) projectedPath.getNodeType().getSqmPathType();
			registerEntityNameUsage( tableGroup, EntityNameUse.PROJECTION, treatedType.getTypeName(), true );

			if ( projectedPath instanceof SqmFrom<?, ?> ) {
				// Register that the TREAT uses for the SqmFrom node may not be downgraded
				( (SqlAstQueryPartProcessingState) getCurrentProcessingState() ).registerFromUsage(
						(SqmFrom<?, ?>) projectedPath,
						true
				);
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
		final SqmPath<?> parentPath = sqmPath.getLhs();
		final SqlAstProcessingState processingState = getCurrentProcessingState();
		if ( processingState instanceof SqlAstQueryPartProcessingState ) {
			if ( parentPath instanceof SqmFrom<?, ?> ) {
				( (SqlAstQueryPartProcessingState) processingState ).registerFromUsage(
						(SqmFrom<?, ?>) parentPath,
						true
				);
			}
			if ( sqmPath instanceof SqmFrom<?, ?> ) {
				( (SqlAstQueryPartProcessingState) processingState ).registerFromUsage(
						(SqmFrom<?, ?>) sqmPath,
						true
				);
			}
		}
		final SqmPathSource<?> resolvedModel;
		if ( !( sqmPath instanceof SqmTreatedPath<?, ?> )
				&& tableGroup.getModelPart().getPartMappingType() instanceof EntityMappingType
				&& ( resolvedModel = sqmPath.getResolvedModel() ) instanceof PersistentAttribute<?, ?> ) {
			final String attributeName = resolvedModel.getPathName();
			final EntityMappingType entityType = (EntityMappingType) tableGroup.getModelPart().getPartMappingType();
			final EntityMappingType parentType;
			if ( parentPath instanceof SqmTreatedPath<?, ?> ) {
				// A treated attribute usage i.e. `treat(alias as Subtype).attribute = 1`
				final ManagedDomainType<?> treatTarget = ( (SqmTreatedPath<?, ?>) parentPath ).getTreatTarget();
				if ( treatTarget.getPersistenceType() == ENTITY ) {
					parentType = creationContext.getMappingMetamodel().getEntityDescriptor( treatTarget.getTypeName() );

					// The following is an optimization to avoid rendering treat conditions into predicates.
					// Imagine an HQL predicate like `treat(alias as Subtype).attribute is null or alias.name = '...'`.
					// If the `attribute` is basic, we will render a case wrapper around the column expression
					// and hence we can safely skip adding the `type(alias) = Subtype and ...` condition to the SQL.

					final ModelPart subPart = parentType.findSubPart( attributeName );
					final EntityNameUse entityNameUse;
					// We only apply this optimization for basic valued model parts for now
					if ( subPart.asBasicValuedModelPart() != null ) {
						entityNameUse = EntityNameUse.OPTIONAL_TREAT;
					}
					else {
						entityNameUse = EntityNameUse.BASE_TREAT;
					}
					registerEntityNameUsage(
							tableGroup,
							entityNameUse,
							treatTarget.getTypeName()
					);
				}
				else {
					parentType = entityType;
				}
			}
			else {
				// A simple attribute usage e.g. `alias.attribute = 1`
				parentType = entityType;
			}
			final AttributeMapping attributeMapping = parentType.findAttributeMapping( attributeName );
			if ( attributeMapping == null ) {
				if ( attributeName.equals( parentType.getIdentifierMapping().getAttributeName() ) ) {
					if ( parentType.getIdentifierMapping() instanceof EmbeddableValuedModelPart ) {
						// Until HHH-16571 is fixed, we must also register an entity name use for the root entity descriptor name
						registerEntityNameUsage(
								tableGroup,
								EntityNameUse.EXPRESSION,
								parentType.getRootEntityDescriptor().getEntityName()
						);
					}
					final EntityDiscriminatorMapping discriminator = parentType.getDiscriminatorMapping();
					final String entityName;
					if ( discriminator != null && discriminator.hasPhysicalColumn() && !parentType.getSubMappingTypes().isEmpty() ) {
						// This is needed to preserve optimization for joined + discriminator inheritance
						// see JoinedSubclassEntityPersister#getIdentifierMappingForJoin
						entityName = parentType.getRootEntityDescriptor().getEntityName();
					}
					else {
						entityName = parentType.getEntityName();
					}
					registerEntityNameUsage(
							tableGroup,
							EntityNameUse.EXPRESSION,
							entityName
					);
				}
				else {
					// If the attribute mapping can't be found on the declaring type and it is not the identifier,
					// this signals that we are working with an arbitrarily chosen attribute from a subclass.
					// Register entity name usages for all subtypes that declare the attribute with the same name then
					for ( EntityMappingType subMappingType : parentType.getSubMappingTypes() ) {
						if ( subMappingType.findDeclaredAttributeMapping( attributeName ) != null ) {
							registerEntityNameUsage( tableGroup, EntityNameUse.EXPRESSION, subMappingType.getEntityName() );
						}
					}
				}
			}
			else {
				registerEntityNameUsage(
						tableGroup,
						EntityNameUse.EXPRESSION,
						attributeMapping.findContainingEntityMapping().getEntityName()
				);
			}
		}
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
		final AbstractEntityPersister persister;
		if ( tableGroup.getModelPart() instanceof EmbeddableValuedModelPart ) {
			persister = null;
			final EmbeddableDomainType<?> embeddableDomainType = creationContext.getSessionFactory()
					.getRuntimeMetamodels()
					.getJpaMetamodel()
					.embeddable( treatTargetTypeName );
			if ( embeddableDomainType == null || !embeddableDomainType.isPolymorphic() ) {
				return;
			}
		}
		else {
			persister = (AbstractEntityPersister) creationContext.getSessionFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.findEntityDescriptor( treatTargetTypeName );
			if ( persister == null || !persister.isPolymorphic() ) {
				return;
			}
		}
		final TableGroup actualTableGroup;
		final EntityNameUse finalEntityNameUse;
		if ( tableGroup instanceof CorrelatedTableGroup ) {
			actualTableGroup = ( (CorrelatedTableGroup) tableGroup ).getCorrelatedTableGroup();
			// For correlated table groups we can't apply filters,
			// as the context is in which the use happens may only affect the result of the subquery
			finalEntityNameUse = entityNameUse == EntityNameUse.EXPRESSION ? entityNameUse : EntityNameUse.PROJECTION;
		}
		else {
			if ( tableGroup instanceof PluralTableGroup ) {
				actualTableGroup = ( (PluralTableGroup) tableGroup ).getElementTableGroup();
			}
			else {
				actualTableGroup = tableGroup;
			}
			finalEntityNameUse = entityNameUse == EntityNameUse.EXPRESSION
					|| entityNameUse == EntityNameUse.PROJECTION
					|| contextAllowsTreatOrFilterEntityNameUse()
				? entityNameUse
				: EntityNameUse.EXPRESSION;
		}
		final Map<String, EntityNameUse> entityNameUses = tableGroupEntityNameUses.computeIfAbsent(
				actualTableGroup,
				tg -> new HashMap<>( 1 )
		);
		entityNameUses.compute(
				treatTargetTypeName,
				(s, existingUse) -> finalEntityNameUse.stronger( existingUse )
		);

		if ( persister == null ) {
			// No need to do anything else for embeddables
			return;
		}

		// Resolve the table reference for all types which we register an entity name use for.
		// Also, force table group initialization for treats when needed to ensure correct cardinality
		final EntityNameUse.UseKind useKind = finalEntityNameUse.getKind();
		if ( actualTableGroup.isInitialized() || ( useKind == EntityNameUse.UseKind.TREAT && actualTableGroup.canUseInnerJoins()
				&& !( (EntityMappingType) actualTableGroup.getModelPart().getPartMappingType() ).isTypeOrSuperType( persister ) ) ) {
			actualTableGroup.resolveTableReference( null, persister.getTableName() );
		}

		if ( projection ) {
			EntityMappingType superMappingType = persister;
			while ( ( superMappingType = superMappingType.getSuperMappingType() ) != null ) {
				entityNameUses.putIfAbsent( superMappingType.getEntityName(), EntityNameUse.PROJECTION );
				actualTableGroup.resolveTableReference(
						null,
						( (AbstractEntityPersister) superMappingType.getEntityPersister() ).getTableName()
				);
			}
		}

		// If we encounter a treat or projection use, we also want register the use for all subtypes.
		// We do this here to not have to expand entity name uses during pruning later on
		if ( useKind == EntityNameUse.UseKind.TREAT ) {
			for ( EntityMappingType subType : persister.getSubMappingTypes() ) {
				entityNameUses.compute(
						subType.getEntityName(),
						(s, existingUse) -> finalEntityNameUse.stronger( existingUse )
				);
			}
		}
		else if ( useKind == EntityNameUse.UseKind.PROJECTION ) {
			for ( EntityMappingType subType : persister.getSubMappingTypes() ) {
				entityNameUses.compute(
						subType.getEntityName(),
						(s, existingUse) -> finalEntityNameUse.stronger( existingUse )
				);
				actualTableGroup.resolveTableReference(
						null,
						subType.getEntityPersister().getMappedTableDetails().getTableName()
				);
			}
		}
	}

	private boolean contextAllowsTreatOrFilterEntityNameUse() {
		final Clause currentClause = getCurrentClauseStack().getCurrent();
		switch ( currentClause ) {
			case SET:
			case FROM:
			case GROUP:
			case HAVING:
			case WHERE:
				// A TREAT or FILTER EntityNameUse is only allowed in these clauses,
				// but only if it's not in a nested context
				return !inNestedContext;
		}
		return false;
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
		final MappingType partMappingType = tableGroup.getModelPart().getPartMappingType();
		if ( partMappingType instanceof EntityMappingType ) {
			final EntityMappingType mappingType = (EntityMappingType) partMappingType;
			final AbstractEntityPersister persister = (AbstractEntityPersister) mappingType.getEntityPersister();
			// Avoid resolving subclass tables for persisters with physical discriminators as we won't need them
			if ( persister.getDiscriminatorMapping().hasPhysicalColumn() ) {
				return;
			}
			if ( getCurrentClauseStack().getCurrent() != Clause.WHERE && getCurrentClauseStack().getCurrent() != Clause.HAVING ) {
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

	protected void pruneTableGroupJoins() {
		for ( Map.Entry<TableGroup, Map<String, EntityNameUse>> entry : tableGroupEntityNameUses.entrySet() ) {
			final TableGroup tableGroup = entry.getKey();
			if ( tableGroup.isInitialized() ) {
				final Map<String, EntityNameUse> entityNameUses = entry.getValue();
				final ModelPartContainer modelPart = tableGroup.getModelPart();
				final MappingType partMappingType;
				if ( modelPart instanceof PluralAttributeMapping ) {
					partMappingType = ( (PluralAttributeMapping) modelPart )
							.getElementDescriptor()
							.getPartMappingType();
				}
				else {
					partMappingType = modelPart.getPartMappingType();
				}

				if ( partMappingType instanceof EntityPersister ) {
					( (EntityPersister) partMappingType ).pruneForSubclasses( tableGroup, entityNameUses );
				}
			}
		}
	}

	protected void consumeExplicitJoins(SqmFrom<?, ?> sqmFrom, TableGroup lhsTableGroup) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Visiting explicit joins for `%s`", sqmFrom.getNavigablePath() );
		}
		sqmFrom.visitSqmJoins(
				sqmJoin -> {
					final TableGroup actualTableGroup = getActualTableGroup( lhsTableGroup, sqmJoin );
					registerPathAttributeEntityNameUsage( sqmJoin, actualTableGroup );
					consumeExplicitJoin( sqmJoin, actualTableGroup, actualTableGroup, true );
				}
		);
		final List<SqmFrom<?, ?>> sqmTreats = sqmFrom.getSqmTreats();
		if ( !sqmTreats.isEmpty() ) {
			final SqlAstQueryPartProcessingState queryPartProcessingState = (SqlAstQueryPartProcessingState) getCurrentProcessingState();
			queryPartProcessingState.registerTreatedFrom( sqmFrom );
			// If a SqmFrom is used anywhere even though treats exists,
			// the treats are context dependent and hence we need to downgrade TREAT entity uses to EXPRESSION.
			// Treat expressions will be protected via predicates or case when expressions,
			// but we may not filter rows based on the TREAT entity uses.
			if ( lhsTableGroup.hasRealJoins() ) {//|| sqmFrom instanceof SqmRoot<?> ) {
				queryPartProcessingState.registerFromUsage( sqmFrom, true );
			}
			for ( SqmFrom<?, ?> sqmTreat : sqmTreats ) {
				final TableGroup actualTableGroup = getActualTableGroup( lhsTableGroup, sqmTreat );
				// We don't know the context yet in which a treat is used, so we have to register base treats and track the usage
				registerEntityNameUsage( actualTableGroup, EntityNameUse.BASE_TREAT, ( (SqmTreatedPath<?, ?>) sqmTreat ).getTreatTarget().getTypeName() );
				consumeExplicitJoins( sqmTreat, actualTableGroup );
			}
		}
	}

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
		else if ( sqmJoin instanceof SqmDerivedJoin<?> ) {
			return consumeDerivedJoin( ( (SqmDerivedJoin<?>) sqmJoin ), lhsTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmCteJoin<?> ) {
			return consumeCteJoin( ( (SqmCteJoin<?>) sqmJoin ), lhsTableGroup, transitive );
		}
		else if ( sqmJoin instanceof SqmPluralPartJoin<?, ?> ) {
			return consumePluralPartJoin( ( (SqmPluralPartJoin<?, ?>) sqmJoin ), ownerTableGroup, transitive );
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
		if ( lhsTableGroup instanceof PluralTableGroup
				&& !( path instanceof SqmPluralPartJoin<?, ?> )
				&& CollectionPart.Nature.fromNameExact( path.getNavigablePath().getLocalName() ) == null ) {
			final TableGroup elementTableGroup = ( (PluralTableGroup) lhsTableGroup ).getElementTableGroup();
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

		final SqmPathSource<?> pathSource = sqmJoin.getReferencedPathSource();
		final SqmJoinType sqmJoinType = sqmJoin.getSqmJoinType();

		final TableGroupJoin joinedTableGroupJoin;
		final TableGroup joinedTableGroup;

		final NavigablePath sqmJoinNavigablePath = sqmJoin.getNavigablePath();

		final ModelPart modelPart = ownerTableGroup.getModelPart().findSubPart(
				pathSource.getPathName(),
				SqmMappingModelHelper.resolveExplicitTreatTarget( sqmJoin, this )
		);

		if ( pathSource instanceof PluralPersistentAttribute ) {
			assert modelPart instanceof PluralAttributeMapping;

			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) modelPart;

			if ( sqmJoin.isFetched() ) {
				containsCollectionFetches = true;
			}

			joinedTableGroupJoin = pluralAttributeMapping.createTableGroupJoin(
					sqmJoinNavigablePath,
					ownerTableGroup,
					sqmJoin.getExplicitAlias(),
					null,
					sqmJoinType.getCorrespondingSqlJoinType(),
					sqmJoin.isFetched(),
					sqmJoin.getJoinPredicate() != null,
					this
			);

			joinedTableGroup = joinedTableGroupJoin.getJoinedGroup();
		}
		else {
			assert modelPart instanceof TableGroupJoinProducer;

			joinedTableGroupJoin = ( (TableGroupJoinProducer) modelPart ).createTableGroupJoin(
					sqmJoinNavigablePath,
					ownerTableGroup,
					sqmJoin.getExplicitAlias(),
					null,
					sqmJoinType.getCorrespondingSqlJoinType(),
					sqmJoin.isFetched(),
					sqmJoin.getJoinPredicate() != null,
					this
			);

			joinedTableGroup = joinedTableGroupJoin.getJoinedGroup();

			// Since this is an explicit join, we force the initialization of a possible lazy table group
			// to retain the cardinality, but only if this is a non-trivial attribute join.
			// Left or inner singular attribute joins without a predicate can be safely optimized away
			if ( sqmJoin.getJoinPredicate() != null || sqmJoinType != SqmJoinType.INNER && sqmJoinType != SqmJoinType.LEFT ) {
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
		if ( !sqmJoin.hasTreats() && sqmJoin.getNodeType().getSqmPathType() instanceof EntityDomainType ) {
			final EntityDomainType<?> entityDomainType = (EntityDomainType<?>) sqmJoin.getNodeType().getSqmPathType();
			final TableGroup elementTableGroup = joinedTableGroup instanceof PluralTableGroup ?
					( (PluralTableGroup) joinedTableGroup ).getElementTableGroup() :
					joinedTableGroup;
			final EntityValuedModelPart entityModelPart = (EntityValuedModelPart) elementTableGroup.getModelPart();
			final EntityPersister entityDescriptor = entityModelPart.getEntityMappingType().getEntityPersister();
			if ( entityDescriptor.getSuperMappingType() != null ) {
				// This is a non-treated join with an entity which is an inheritance subtype,
				// register a TREAT entity name use to filter only the entities of the correct type.
				registerEntityNameUsage(
						elementTableGroup,
						EntityNameUse.TREAT,
						entityDomainType.getHibernateEntityName()
				);
			}
		}

		// Implicit joins in the predicate might alter the nested table group joins,
		// so defer determination of the join for predicate until after the predicate was visited
		final TableGroupJoin joinForPredicate;

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			if ( sqmJoin.isFetched() ) {
				QueryLogging.QUERY_MESSAGE_LOGGER.debugf( "Join fetch [%s] is restricted", sqmJoinNavigablePath );
			}

			final SqmJoin<?, ?> oldJoin = currentlyProcessingJoin;
			currentlyProcessingJoin = sqmJoin;
			final Predicate predicate = visitNestedTopLevelPredicate( sqmJoin.getJoinPredicate() );
			joinForPredicate = TableGroupJoinHelper.determineJoinForPredicateApply( joinedTableGroupJoin );
			// If translating the join predicate didn't initialize the table group,
			// we can safely apply it on the collection table group instead
			if ( joinForPredicate.getJoinedGroup().isInitialized() ) {
				joinForPredicate.applyPredicate( predicate );
			}
			else {
				joinedTableGroupJoin.applyPredicate( predicate );
			}
			currentlyProcessingJoin = oldJoin;
		}
		else {
			joinForPredicate = TableGroupJoinHelper.determineJoinForPredicateApply( joinedTableGroupJoin );
		}
		// Since joins on treated paths will never cause table pruning, we need to add a join condition for the treat
		if ( sqmJoin.getLhs() instanceof SqmTreatedPath<?, ?> ) {
			final SqmTreatedPath<?, ?> treatedPath = (SqmTreatedPath<?, ?>) sqmJoin.getLhs();
			final ManagedDomainType<?> treatTarget = treatedPath.getTreatTarget();
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

	private TableGroup consumeCrossJoin(SqmCrossJoin<?> sqmJoin, TableGroup lhsTableGroup, boolean transitive) {
		final EntityPersister entityDescriptor = resolveEntityPersister( sqmJoin.getReferencedPathSource() );

		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
				true,
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				null,
				() -> predicate -> additionalRestrictions = combinePredicates( additionalRestrictions, predicate ),
				this
		);

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
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

	private TableGroup consumeEntityJoin(SqmEntityJoin<?> sqmJoin, TableGroup lhsTableGroup, boolean transitive) {
		final EntityPersister entityDescriptor = resolveEntityPersister( sqmJoin.getReferencedPathSource() );

		final SqlAstJoinType correspondingSqlJoinType = sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType();
		final TableGroup tableGroup = entityDescriptor.createRootTableGroup(
				correspondingSqlJoinType == SqlAstJoinType.INNER || correspondingSqlJoinType == SqlAstJoinType.CROSS,
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

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				sqmJoin.getNavigablePath(),
				correspondingSqlJoinType,
				tableGroup,
				null
		);
		lhsTableGroup.addTableGroupJoin( tableGroupJoin );

		entityDescriptor.applyBaseRestrictions(
				tableGroupJoin::applyPredicate,
				tableGroup,
				true,
				getLoadQueryInfluencers().getEnabledFilters(),
				false,
				null,
				this
		);

		final SoftDeleteMapping softDeleteMapping = entityDescriptor.getSoftDeleteMapping();
		if ( softDeleteMapping != null ) {
			final Predicate softDeleteRestriction = createNonSoftDeletedRestriction(
					tableGroup.resolveTableReference( softDeleteMapping.getTableName() ),
					softDeleteMapping
			);
			tableGroupJoin.applyPredicate( softDeleteRestriction );
		}

		if ( sqmJoin.getJoinPredicate() != null ) {
			final SqmJoin<?, ?> oldJoin = currentlyProcessingJoin;
			currentlyProcessingJoin = sqmJoin;
			tableGroupJoin.applyPredicate( visitNestedTopLevelPredicate( sqmJoin.getJoinPredicate() ) );
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
		final SelectStatement statement = (SelectStatement) sqmJoin.getQueryPart().accept( this );
		if ( !sqmJoin.isLateral() ) {
			fromClauseIndexStack.pop();
		}
		final AnonymousTupleType<?> tupleType = (AnonymousTupleType<?>) sqmJoin.getNodeType();
		final List<SqlSelection> sqlSelections = statement.getQueryPart().getFirstQuerySpec().getSelectClause().getSqlSelections();
		final AnonymousTupleTableGroupProducer tableGroupProducer = tupleType.resolveTableGroupProducer(
				sqmJoin.getExplicitAlias(),
				sqlSelections,
				lastPoppedFromClauseIndex
		);
		final List<String> columnNames = tupleType.determineColumnNames();
		final SqlAliasBase sqlAliasBase = getSqlAliasBaseGenerator().createSqlAliasBase(
				sqmJoin.getExplicitAlias() == null ? "derived" : sqmJoin.getExplicitAlias()
		);
		final String identifierVariable = sqlAliasBase.generateNewAlias();
		final QueryPartTableGroup queryPartTableGroup = new QueryPartTableGroup(
				sqmJoin.getNavigablePath(),
				tableGroupProducer,
				statement,
				identifierVariable,
				columnNames,
				tableGroupProducer.getCompatibleTableExpressions(),
				sqmJoin.isLateral(),
				false,
				creationContext.getSessionFactory()
		);
		getFromClauseIndex().register( sqmJoin, queryPartTableGroup );

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				queryPartTableGroup.getNavigablePath(),
				sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
				queryPartTableGroup,
				null
		);
		parentTableGroup.addTableGroupJoin( tableGroupJoin );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			final SqmJoin<?, ?> oldJoin = currentlyProcessingJoin;
			currentlyProcessingJoin = sqmJoin;
			tableGroupJoin.applyPredicate( visitNestedTopLevelPredicate( sqmJoin.getJoinPredicate() ) );
			currentlyProcessingJoin = oldJoin;
		}

		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, queryPartTableGroup );
		}
		return queryPartTableGroup;
	}

	private TableGroup consumeCteJoin(SqmCteJoin<?> sqmJoin, TableGroup parentTableGroup, boolean transitive) {
		final SqlAstJoinType correspondingSqlJoinType = sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType();
		final TableGroup tableGroup = createCteTableGroup(
				getCteName( sqmJoin.getCte().getCteTable() ),
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				correspondingSqlJoinType == SqlAstJoinType.INNER || correspondingSqlJoinType == SqlAstJoinType.CROSS
		);
		getFromClauseIndex().register( sqmJoin, tableGroup );

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				tableGroup.getNavigablePath(),
				correspondingSqlJoinType,
				tableGroup,
				null
		);
		parentTableGroup.addTableGroupJoin( tableGroupJoin );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			final SqmJoin<?, ?> oldJoin = currentlyProcessingJoin;
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
		final PluralTableGroup pluralTableGroup = (PluralTableGroup) lhsTableGroup;
		final TableGroup tableGroup = getPluralPartTableGroup( pluralTableGroup, sqmJoin.getReferencedPathSource() );
		getFromClauseIndex().register( sqmJoin, tableGroup );

		assert sqmJoin.getJoinPredicate() == null;
		if ( transitive ) {
			consumeExplicitJoins( sqmJoin, tableGroup );
		}
		return tableGroup;
	}

	private TableGroup getPluralPartTableGroup(PluralTableGroup pluralTableGroup, SqmPathSource<?> pathSource) {
		final CollectionPart.Nature nature = CollectionPart.Nature.fromNameExact( pathSource.getPathName() );
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
		final Consumer<TableGroup> implicitJoinChecker;
		implicitJoinChecker = tg -> {};
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
			final TableGroup existingTableGroup = fromClauseIndex.findTableGroupForGetOrCreate(
					sqmPath.getNavigablePath(),
					allowLeftJoins
			);
			if ( existingTableGroup == null ) {
				final TableGroup createdTableGroup = createTableGroup(
						getActualTableGroup(
							fromClauseIndex.getTableGroup( sqmPath.getLhs().getNavigablePath() ),
							sqmPath
						),
						sqmPath,
						allowLeftJoins
				);
				if ( createdTableGroup != null ) {
					if ( sqmPath instanceof SqmTreatedPath<?, ?> ) {
						fromClauseIndex.register( sqmPath, createdTableGroup );
					}
				}
			}
		}
		return supplier.get();
	}

	private TableGroup prepareReusablePath(
			FromClauseIndex fromClauseIndex,
			JpaPath<?> path,
			Consumer<TableGroup> implicitJoinChecker) {
		final SqmPath<?> sqmPath = (SqmPath<?>) path;
		final SqmPath<?> parentPath;
		final boolean treated;
		if ( sqmPath instanceof SqmTreatedPath<?, ?> ) {
			parentPath = ( (SqmTreatedPath<?, ?>) sqmPath ).getWrappedPath();
			treated = true;
		}
		else {
			parentPath = sqmPath.getLhs();
			treated = false;
		}
		if ( parentPath == null ) {
			if ( sqmPath instanceof SqmFunctionPath<?> ) {
				final SqmFunctionPath<?> functionPath = (SqmFunctionPath<?>) sqmPath;
				if ( functionPath.getReferencedPathSource() instanceof CompositeSqmPathSource<?> ) {
					return (TableGroup) visitFunctionPath( functionPath );
				}
			}
			return null;
		}
		final TableGroup parentTableGroup = getActualTableGroup(
				fromClauseIndex.findTableGroupForGetOrCreate( parentPath.getNavigablePath() ),
				sqmPath
		);
		if ( parentTableGroup == null ) {
			final TableGroup createdParentTableGroup = prepareReusablePath(
					fromClauseIndex,
					parentPath,
					implicitJoinChecker
			);
			if ( createdParentTableGroup == null ) {
				throw new SqlTreeCreationException( "Could not locate TableGroup - " + parentPath.getNavigablePath() );
			}
			final TableGroup newTableGroup;
			if ( parentPath instanceof SqmTreatedPath<?, ?> ) {
				fromClauseIndex.register( parentPath, createdParentTableGroup );
				newTableGroup = createdParentTableGroup;
			}
			else if ( createdParentTableGroup instanceof PluralTableGroup ) {
				final CollectionPart.Nature nature = CollectionPart.Nature.fromName(
						parentPath.getNavigablePath().getLocalName()
				);
				assert nature != null;
				newTableGroup = ( (PluralTableGroup) createdParentTableGroup ).getTableGroup( nature );
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
				&& parentTableGroup.getModelPart() instanceof ToOneAttributeMapping ) {
			// we need to handle the case of an implicit path involving a to-one
			// association that path has been previously joined using left.
			// typically, this indicates that the to-one is being
			// fetched - the fetch would use a left-join.  however, since the path is
			// used outside the select-clause also, we need to force the join to be inner
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) parentTableGroup.getModelPart();
			final String partName = sqmPath.getResolvedModel().getPathName();
			final ModelPart pathPart;
			if ( !toOneAttributeMapping.isFkOptimizationAllowed()
					|| !( ( pathPart = toOneAttributeMapping.findSubPart( partName ) ) instanceof ValuedModelPart )
					|| !toOneAttributeMapping.getForeignKeyDescriptor().isKeyPart( (ValuedModelPart) pathPart ) ) {
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
		final SqmPath<?> path;
		// Don't create joins for plural part paths as that will be handled
		// through a cardinality preserving mechanism in visitIndexAggregateFunction/visitElementAggregateFunction
		if ( selectionPath instanceof AbstractSqmSpecificPluralPartPath<?> ) {
			path = selectionPath.getLhs().getLhs();
		}
		else {
			path = selectionPath;
		}
		final FromClauseIndex fromClauseIndex = getFromClauseIndex();
		final TableGroup tableGroup = fromClauseIndex.findTableGroupForGetOrCreate( path.getNavigablePath() );
		if ( tableGroup == null ) {
			prepareReusablePath( path, () -> null );

			if ( path.getLhs() != null && !( path instanceof SqmEntityValuedSimplePath<?>
					|| path instanceof SqmEmbeddedValuedSimplePath<?>
					|| path instanceof SqmAnyValuedSimplePath<?>
					|| path instanceof SqmTreatedPath<?, ?> ) ) {
				// Since this is a selection, we must create a table group for the path as a DomainResult will be created
				// But only create it for paths that are not handled by #prepareReusablePath anyway
				final TableGroup createdTableGroup = createTableGroup(
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
		final SqmPath<?> lhsPath = joinedPath.getLhs();
		final FromClauseIndex fromClauseIndex = getFromClauseIndex();
		final ModelPart subPart = parentTableGroup.getModelPart().findSubPart(
				joinedPath.getReferencedPathSource().getPathName(),
				lhsPath instanceof SqmTreatedPath<?, ?> && ( (SqmTreatedPath<?, ?>) lhsPath ).getTreatTarget().getPersistenceType() == ENTITY
						? resolveEntityPersister( (EntityDomainType<?>) ( (SqmTreatedPath<?, ?>) lhsPath ).getTreatTarget() )
						: null
		);

		final TableGroup tableGroup;
		if ( subPart instanceof TableGroupJoinProducer ) {
			final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) subPart;
			if ( fromClauseIndex.findTableGroupOnCurrentFromClause( parentTableGroup.getNavigablePath() ) == null
					&& !isRecursiveCte( parentTableGroup ) ) {
				final SqlAstQueryNodeProcessingState queryNodeProcessingState = currentQueryNodeProcessingState();
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
				final TableGroupJoin compatibleLeftJoin;
				final SqlAstJoinType sqlAstJoinType;
				if ( isMappedByOrNotFoundToOne( joinProducer ) ) {
					compatibleLeftJoin = parentTableGroup.findCompatibleJoin(
							joinProducer,
							SqlAstJoinType.LEFT
					);
					sqlAstJoinType = SqlAstJoinType.LEFT;
				}
				else {
					compatibleLeftJoin = null;
					sqlAstJoinType = null;
				}

				final TableGroup compatibleTableGroup = compatibleLeftJoin != null ?
						compatibleLeftJoin.getJoinedGroup() :
						parentTableGroup.findCompatibleJoinedGroup( joinProducer, SqlAstJoinType.INNER );
				if ( compatibleTableGroup == null ) {
					final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
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
		if ( joinProducer instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOne = (ToOneAttributeMapping) joinProducer;
			return toOne.hasNotFoundAction() ||
					// ToOne( mappedBy = "..." ) always has a referenced property name and is the target side
					( toOne.getReferencedPropertyName() != null && toOne.getSideNature() == ForeignKeyDescriptor.Nature.TARGET );
		}
		return false;
	}

	private boolean isRecursiveCte(TableGroup tableGroup) {
		if ( tableGroup instanceof CteTableGroup ) {
			final CteTableGroup cteTableGroup = (CteTableGroup) tableGroup;
			return cteContainer.getCteStatement( cteTableGroup.getPrimaryTableReference().getTableId() ).isRecursive();
		}
		return false;
	}

	private void registerPluralTableGroupParts(TableGroup tableGroup) {
		registerPluralTableGroupParts( null, tableGroup );
	}

	private void registerPluralTableGroupParts(NavigablePath navigablePath, TableGroup tableGroup) {
		if ( tableGroup instanceof PluralTableGroup ) {
			final PluralTableGroup pluralTableGroup = (PluralTableGroup) tableGroup;
			if ( pluralTableGroup.getElementTableGroup() != null ) {
				getFromClauseAccess().registerTableGroup(
						navigablePath == null || navigablePath == tableGroup.getNavigablePath()
								? pluralTableGroup.getElementTableGroup().getNavigablePath()
								: navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
						pluralTableGroup.getElementTableGroup()
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
		final TableGroup resolved = getFromClauseAccess().findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolved != null ) {
			log.tracef( "SqmRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolved );
			return visitTableGroup( resolved, sqmRoot );
		}

		throw new InterpretationException( "SqmRoot not yet resolved to TableGroup" );
	}

	@Override
	public Object visitRootDerived(SqmDerivedRoot<?> sqmRoot) {
		final TableGroup resolved = getFromClauseAccess().findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolved != null ) {
			log.tracef( "SqmDerivedRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolved );
			return visitTableGroup( resolved, sqmRoot );
		}

		throw new InterpretationException( "SqmDerivedRoot not yet resolved to TableGroup" );
	}

	@Override
	public Object visitRootCte(SqmCteRoot<?> sqmRoot) {
		final TableGroup resolved = getFromClauseAccess().findTableGroup( sqmRoot.getNavigablePath() );
		if ( resolved != null ) {
			log.tracef( "SqmCteRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolved );
			return visitTableGroup( resolved, sqmRoot );
		}

		throw new InterpretationException( "SqmCteRoot not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> sqmJoin) {
		final TableGroup existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmAttributeJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmAttributeJoin not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitQualifiedDerivedJoin(SqmDerivedJoin<?> sqmJoin) {
		final TableGroup existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmDerivedJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmDerivedJoin not yet resolved to TableGroup" );
	}

	@Override
	public Object visitQualifiedCteJoin(SqmCteJoin<?> sqmJoin) {
		final TableGroup existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmCteJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmCteJoin not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitCrossJoin(SqmCrossJoin<?> sqmJoin) {
		final TableGroup existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmCrossJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmCrossJoin not yet resolved to TableGroup" );
	}

	@Override
	public Object visitPluralPartJoin(SqmPluralPartJoin<?, ?> sqmJoin) {
		final TableGroup existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmPluralPartJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmPluralPartJoin not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitQualifiedEntityJoin(SqmEntityJoin<?> sqmJoin) {
		final TableGroup existing = getFromClauseAccess().findTableGroup( sqmJoin.getNavigablePath() );
		if ( existing != null ) {
			log.tracef( "SqmEntityJoin [%s] resolved to existing TableGroup [%s]", sqmJoin, existing );
			return visitTableGroup( existing, sqmJoin );
		}

		throw new InterpretationException( "SqmEntityJoin not yet resolved to TableGroup" );
	}

	private Expression visitTableGroup(TableGroup tableGroup, SqmPath<?> path) {
		final ModelPartContainer tableGroupModelPart = tableGroup.getModelPart();

		final ModelPart actualModelPart;
		final NavigablePath navigablePath;
		if ( tableGroupModelPart instanceof PluralAttributeMapping ) {
			actualModelPart = ( (PluralAttributeMapping) tableGroupModelPart ).getElementDescriptor();
			navigablePath = tableGroup.getNavigablePath().append( actualModelPart.getPartName() );
		}
		else {
			actualModelPart = tableGroupModelPart;
			navigablePath = tableGroup.getNavigablePath();
		}

		final Expression result;
		if ( actualModelPart instanceof EntityValuedModelPart ) {
			final EntityValuedModelPart entityValuedModelPart = (EntityValuedModelPart) actualModelPart;
			final EntityValuedModelPart inferredEntityMapping = (EntityValuedModelPart) getInferredValueMapping();
			final ModelPart resultModelPart;
			final EntityValuedModelPart interpretationModelPart;
			final TableGroup tableGroupToUse;
			if ( inferredEntityMapping == null ) {
				// When the inferred mapping is null, we try to resolve to the FK by default, which is fine because
				// expansion to all target columns for select and group by clauses is handled in EntityValuedPathInterpretation
				if ( entityValuedModelPart instanceof EntityAssociationMapping
						&& isFkOptimizationAllowed( path, (EntityAssociationMapping) entityValuedModelPart ) ) {
					// If the table group uses an association mapping that is not a one-to-many,
					// we make use of the FK model part - unless the path is a non-optimizable join,
					// for which we should always use the target's identifier to preserve semantics
					final EntityAssociationMapping associationMapping = (EntityAssociationMapping) entityValuedModelPart;
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
			else if ( inferredEntityMapping instanceof ToOneAttributeMapping ) {
				// If the inferred mapping is a to-one association,
				// we use the FK target part, which must be located on the entity mapping
				final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) inferredEntityMapping;
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
			else if ( inferredEntityMapping instanceof EntityCollectionPart ) {
				// If the inferred mapping is a collection part, we try to make use of the FK again to avoid joins
				final EntityCollectionPart collectionPart = (EntityCollectionPart) inferredEntityMapping;

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
					final ManyToManyCollectionPart manyToManyPart = (ManyToManyCollectionPart) collectionPart;

					if ( entityValuedModelPart == collectionPart ) {
						// When we compare the same collection parts, we can just use the FK part
						resultModelPart = manyToManyPart.getForeignKeyDescriptor().getKeyPart();
					}
					else if ( entityValuedModelPart instanceof EntityAssociationMapping ) {
						// If the table group model part is an association, we check if the FK targets are compatible
						final EntityAssociationMapping tableGroupAssociation = (EntityAssociationMapping) entityValuedModelPart;
						final ModelPart pathTargetPart = tableGroupAssociation
								.getForeignKeyDescriptor()
								.getPart( tableGroupAssociation.getSideNature().inverse() );
						final ModelPart inferredTargetPart = manyToManyPart
								.getForeignKeyDescriptor()
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
					else if ( entityValuedModelPart instanceof AnonymousTupleEntityValuedModelPart ) {
					resultModelPart = ( (AnonymousTupleEntityValuedModelPart) entityValuedModelPart ).getForeignKeyPart();
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
							resultModelPart = manyToManyPart
									.getForeignKeyDescriptor()
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
			else {
				// Render the identifier mapping if the inferred mapping is an EntityMappingType
				assert inferredEntityMapping instanceof EntityMappingType;
				resultModelPart = ( (EntityMappingType) inferredEntityMapping ).getIdentifierMapping();
				interpretationModelPart = inferredEntityMapping;
				tableGroupToUse = null;
			}

			final EntityMappingType treatedMapping;
			if ( path instanceof SqmTreatedPath<?, ?> && ( (SqmTreatedPath<?, ?>) path ).getTreatTarget().getPersistenceType() == ENTITY ) {
				final ManagedDomainType<?> treatTarget = ( (SqmTreatedPath<?, ?>) path ).getTreatTarget();
				treatedMapping = creationContext.getSessionFactory()
						.getRuntimeMetamodels()
						.getMappingMetamodel()
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
		else if ( actualModelPart instanceof EmbeddableValuedModelPart ) {
			final EmbeddableValuedModelPart mapping = (EmbeddableValuedModelPart) actualModelPart;
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
			final BasicValuedModelPart mapping = actualModelPart.asBasicValuedModelPart();
			if ( mapping != null ) {
				final TableReference tableReference = tableGroup.resolveTableReference(
						navigablePath.append( actualModelPart.getPartName() ),
						mapping,
						mapping.getContainingTableExpression()
				);

				final Expression expression = getSqlExpressionResolver().resolveSqlExpression(
						tableReference,
						mapping
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
						mapping,
						tableGroup
				);
			}
			else if ( actualModelPart instanceof AnonymousTupleTableGroupProducer ) {
				throw new SemanticException(
						"The derived SqmFrom" + ( (AnonymousTupleType<?>) path.getReferencedPathSource() ).getComponentNames() + " can not be used in a context where the expression needs to " +
								"be expanded to identifying parts, because a derived model part does not have identifying parts. " +
								"Replace uses of the root with paths instead e.g. `derivedRoot.get(\"alias1\")` or `derivedRoot.alias1`"
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
		final BasicValuedPathInterpretation<?> path = prepareReusablePath(
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

				JdbcMappingContainer durationType = scaledExpression.getExpressionType();
				Duration duration;
				if ( durationType.getSingleJdbcMapping().getJdbcType().isInterval() ) {
					// For interval types, we need to extract the epoch for integer arithmetic for the 'by unit' operator
					duration = new Duration(
							extractEpoch( scaledExpression ),
							SECOND,
							(BasicValuedMapping) durationType
					);
				}
				else {
					// Durations are stored as nanoseconds (see DurationJavaType)
					duration = new Duration( scaledExpression, NANOSECOND, (BasicValuedMapping) durationType );
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
		final PatternRenderer patternRenderer = new PatternRenderer(
				creationContext.getSessionFactory()
						.getJdbcServices()
						.getDialect()
						.extractPattern( EPOCH )
		);
		return new SelfRenderingFunctionSqlAstExpression(
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
		final SqmPath<?> lhs = fkExpression.getToOnePath().getLhs();
		prepareReusablePath( lhs, () -> null );
		final TableGroup tableGroup = getFromClauseIndex().findTableGroup( lhs.getNavigablePath() );
		final ModelPart subPart = tableGroup.getModelPart()
				.findSubPart( fkExpression.getToOnePath().getModel().getPathName(), null );
		assert subPart instanceof ToOneAttributeMapping;

		final ToOneAttributeMapping toOneMapping = (ToOneAttributeMapping) subPart;
		final ForeignKeyDescriptor fkDescriptor = toOneMapping.getForeignKeyDescriptor();
		final TableReference tableReference = tableGroup.resolveTableReference( toOneMapping.getContainingTableExpression() );

		final ModelPart fkKeyPart = fkDescriptor.getPart( toOneMapping.getSideNature() );
		final BasicValuedModelPart basicFkPart = fkKeyPart.asBasicValuedModelPart();
		if ( basicFkPart != null ) {
			return getSqlExpressionResolver().resolveSqlExpression(
					tableReference,
					basicFkPart
			);
		}
		else {
			assert fkKeyPart instanceof EmbeddableValuedModelPart;
			final EmbeddableValuedModelPart compositeFkPart = (EmbeddableValuedModelPart) fkKeyPart;
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
		if ( creationContext.getSessionFactory().getJdbcServices().getDialect().supportsLateral() ) {
			return createLateralJoinExpression( pluralPartPath, index, functionName );
		}
		else {
			return createCorrelatedAggregateSubQuery( pluralPartPath, index, functionName );
		}
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
		final NavigablePath navigablePath = functionPath.getNavigablePath();
		TableGroup tableGroup = getFromClauseAccess().findTableGroup( navigablePath );
		if ( tableGroup == null ) {
			final Expression functionExpression = (Expression) functionPath.getFunction().accept( this );
			final JdbcType jdbcType = functionExpression.getExpressionType()
					.getSingleJdbcMapping()
					.getJdbcType();
			if ( jdbcType instanceof AggregateJdbcType ) {
				tableGroup = new EmbeddableFunctionTableGroup(
						navigablePath,
						( (AggregateJdbcType) jdbcType ).getEmbeddableMappingType(),
						functionExpression
				);
				getFromClauseAccess().registerTableGroup( navigablePath, tableGroup );
			}
			else {
				return functionExpression;
			}
		}
		return tableGroup;
	}

	@Override
	public Expression visitCorrelation(SqmCorrelation<?, ?> correlation) {
		final TableGroup resolved = getFromClauseAccess().findTableGroup( correlation.getNavigablePath() );
		if ( resolved != null ) {
			log.tracef( "SqmCorrelation [%s] resolved to existing TableGroup [%s]", correlation, resolved );
			return visitTableGroup( resolved, correlation );
		}
		throw new InterpretationException( "SqmCorrelation not yet resolved to TableGroup" );
	}

	@Override
	public Expression visitTreatedPath(SqmTreatedPath<?, ?> sqmTreatedPath) {
		prepareReusablePath( sqmTreatedPath, () -> null );
		final TableGroup resolved = getFromClauseAccess().findTableGroup( sqmTreatedPath.getNavigablePath() );
		if ( resolved != null ) {
			log.tracef( "SqmTreatedPath [%s] resolved to existing TableGroup [%s]", sqmTreatedPath, resolved );
			return visitTableGroup( resolved, sqmTreatedPath );
		}

		throw new InterpretationException( "SqmTreatedPath not yet resolved to TableGroup" );
	}


	@Override
	public Expression visitPluralAttributeSizeFunction(SqmCollectionSize function) {
		final SqmPath<?> pluralPath = function.getPluralPath();

		prepareReusablePath( pluralPath, () -> null );
		final TableGroup parentTableGroup = getFromClauseAccess().getTableGroup( pluralPath.getNavigablePath().getParent() );
		assert parentTableGroup != null;

		final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) parentTableGroup.getModelPart().findSubPart(
				pluralPath.getNavigablePath().getLocalName(),
				null
		);
		assert pluralAttributeMapping != null;

		final QuerySpec subQuerySpec = new QuerySpec( false );
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
			final TableGroup tableGroup = pluralAttributeMapping.createRootTableGroup(
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

			final AbstractSqmSelfRenderingFunctionDescriptor functionDescriptor = resolveFunction( "count" );
			final BasicType<Integer> integerType = creationContext.getMappingMetamodel()
					.getTypeConfiguration()
					.getBasicTypeForJavaType( Integer.class );
			final Expression expression = new SelfRenderingAggregateFunctionSqlAstExpression(
					functionDescriptor.getName(),
					functionDescriptor,
					singletonList( Star.INSTANCE ),
					null,
					integerType,
					integerType
			);
			subQuerySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( expression ) );

			subQuerySpec.applyPredicate(
					pluralAttributeMapping.getKeyDescriptor().generateJoinPredicate(
							parentTableGroup,
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

		final PluralAttributeMapping mapDescriptor = (PluralAttributeMapping) tableGroup.getModelPart();

		final CollectionPart indexDescriptor = mapDescriptor.getIndexDescriptor();
		final NavigablePath indexNavigablePath = mapNavigablePath.append( indexDescriptor.getPartName() );
		final DomainResult<Object> indexResult = indexDescriptor.createDomainResult(
				indexNavigablePath,
				tableGroup,
				null,
				this
		);
		registerProjectionUsageFromDescriptor( tableGroup, indexDescriptor );

		final CollectionPart valueDescriptor = mapDescriptor.getElementDescriptor();
		final NavigablePath valueNavigablePath = mapNavigablePath.append( valueDescriptor.getPartName() );
		final DomainResult<Object> valueResult = valueDescriptor.createDomainResult(
				valueNavigablePath,
				tableGroup,
				null,
				this
		);
		registerProjectionUsageFromDescriptor( tableGroup, valueDescriptor );

		return new DomainResultProducer<Map.Entry<Object, Object>>() {
			@Override
			public DomainResult<Map.Entry<Object, Object>> createDomainResult(
					String resultVariable,
					DomainResultCreationState creationState) {
				final JavaType<Map.Entry<Object, Object>> mapEntryDescriptor = getTypeConfiguration()
						.getJavaTypeRegistry()
						.resolveDescriptor( Map.Entry.class );
				return new SqmMapEntryResult<>( indexResult, valueResult, resultVariable, mapEntryDescriptor );
			}

			@Override
			public void applySqlSelections(DomainResultCreationState creationState) {
				throw new UnsupportedOperationException();
			}
		};
	}

	private void registerProjectionUsageFromDescriptor(TableGroup tableGroup, CollectionPart descriptor) {
		if ( descriptor instanceof EntityCollectionPart ) {
			final EntityCollectionPart entityCollectionPart = (EntityCollectionPart) descriptor;
			final EntityMappingType entityMappingType = entityCollectionPart.getEntityMappingType();
			registerEntityNameUsage( tableGroup, EntityNameUse.PROJECTION, entityMappingType.getEntityName(), true );
		}
	}

	protected Expression createCorrelatedAggregateSubQuery(
			AbstractSqmSpecificPluralPartPath<?> pluralPartPath,
			boolean index,
			String function) {
		prepareReusablePath( pluralPartPath.getLhs(), () -> null );

		final FromClauseAccess parentFromClauseAccess = getFromClauseAccess();
		final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) determineValueMapping(
				pluralPartPath.getPluralDomainPath() );

		final QuerySpec subQuerySpec = new QuerySpec( false );

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
			final TableGroup tableGroup = pluralAttributeMapping.createRootTableGroup(
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

			final AbstractSqmSelfRenderingFunctionDescriptor functionDescriptor = resolveFunction( function );
			final CollectionPart collectionPart = index
					? pluralAttributeMapping.getIndexDescriptor()
					: pluralAttributeMapping.getElementDescriptor();
			final ModelPart modelPart;
			if ( collectionPart instanceof OneToManyCollectionPart ) {
				final OneToManyCollectionPart toManyPart = (OneToManyCollectionPart) collectionPart;
				modelPart = toManyPart.getAssociatedEntityMappingType().getIdentifierMapping();
//				modelPart = pluralAttributeMapping.getKeyDescriptor().getTargetPart();
			}
			else if ( collectionPart instanceof ManyToManyCollectionPart ) {
				modelPart = ( (ManyToManyCollectionPart) collectionPart ).getKeyTargetMatchPart();
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
			final Expression expression = new SelfRenderingAggregateFunctionSqlAstExpression(
					functionDescriptor.getName(),
					functionDescriptor,
					arguments,
					null,
					(ReturnableType<?>) functionDescriptor.getReturnTypeResolver().resolveFunctionReturnType(
							() -> null,
							arguments
					).getJdbcMapping(),
					modelPart
			);
			subQuerySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( expression ) );

			NavigablePath parent = pluralPartPath.getPluralDomainPath().getNavigablePath().getParent();
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
		return (AbstractSqmSelfRenderingFunctionDescriptor) creationContext
				.getSessionFactory()
				.getQueryEngine()
				.getSqmFunctionRegistry()
				.findFunctionDescriptor( function );
	}

	protected Expression createLateralJoinExpression(
			AbstractSqmSpecificPluralPartPath<?> pluralPartPath,
			boolean index,
			String functionName) {
		prepareReusablePath( pluralPartPath.getLhs(), () -> null );

		final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) determineValueMapping(
				pluralPartPath.getPluralDomainPath() );
		final FromClauseAccess parentFromClauseAccess = getFromClauseAccess();
		final TableGroup parentTableGroup = parentFromClauseAccess.findTableGroup(
				pluralPartPath.getNavigablePath().getParent()
		);
		final CollectionPart collectionPart = index
				? pluralAttributeMapping.getIndexDescriptor()
				: pluralAttributeMapping.getElementDescriptor();
		final ModelPart modelPart;
		if ( collectionPart instanceof OneToManyCollectionPart ) {
			final OneToManyCollectionPart toManyPart = (OneToManyCollectionPart) collectionPart;
			modelPart = toManyPart.getAssociatedEntityMappingType().getIdentifierMapping();
//				modelPart = pluralAttributeMapping.getKeyDescriptor().getTargetPart();
		}
		else if ( collectionPart instanceof ManyToManyCollectionPart ) {
			modelPart = ( (ManyToManyCollectionPart) collectionPart ).getKeyTargetMatchPart();
		}
		else {
			modelPart = collectionPart;
		}
		final int jdbcTypeCount = modelPart.getJdbcTypeCount();
		final String pathName = functionName + ( index ? "_index" : "_element" );
		final String identifierVariable = parentTableGroup.getPrimaryTableReference().getIdentificationVariable()
				+ "_" + pathName;
		final NavigablePath queryPath = new NavigablePath( parentTableGroup.getNavigablePath(), pathName, identifierVariable );
		TableGroup lateralTableGroup = parentFromClauseAccess.findTableGroup( queryPath );
		if ( lateralTableGroup == null ) {
			final QuerySpec subQuerySpec = new QuerySpec( false );
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
				final TableGroup tableGroup = pluralAttributeMapping.createRootTableGroup(
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
				final NavigablePath navigablePath = pluralPartPath.getNavigablePath();
				final Boolean max = functionName.equalsIgnoreCase( "max" ) ? Boolean.TRUE
						: ( functionName.equalsIgnoreCase( "min" ) ? Boolean.FALSE : null );
				final AbstractSqmSelfRenderingFunctionDescriptor functionDescriptor =
						resolveFunction( functionName );
				final List<ColumnReference> subQueryColumns = new ArrayList<>( jdbcTypeCount );
				modelPart.forEachSelectable(
						(selectionIndex, selectionMapping) -> {
							final ColumnReference columnReference = new ColumnReference(
									tableGroup.resolveTableReference(
											navigablePath,
											(ValuedModelPart) modelPart,
											selectionMapping.getContainingTableExpression()
									),
									selectionMapping
							);
							final String columnName;
							if ( selectionMapping.isFormula() ) {
								columnName = "col" + columnNames.size();
							}
							else {
								columnName = selectionMapping.getSelectionExpression();
							}
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
								new SqlSelectionImpl(
										i,
										subQueryColumns.get( i )
								)
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
					final List<? extends SqlAstNode> arguments;
					if ( jdbcTypeCount == 1 ) {
						arguments = subQueryColumns;
					}
					else {
						arguments = singletonList( new SqlTuple( subQueryColumns, modelPart ) );
					}
					final Expression expression = new SelfRenderingAggregateFunctionSqlAstExpression(
							functionDescriptor.getName(),
							functionDescriptor,
							arguments,
							null,
							(ReturnableType<?>) functionDescriptor.getReturnTypeResolver().resolveFunctionReturnType(
									() -> null,
									arguments
							).getJdbcMapping(),
							modelPart
					);

					subQuerySpec.getSelectClause().addSqlSelection(
							new SqlSelectionImpl( expression )
					);
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
				final Set<String> compatibleTableExpressions;
				final BasicValuedModelPart basicPart = modelPart.asBasicValuedModelPart();
				if ( basicPart != null ) {
					compatibleTableExpressions = Collections.singleton( basicPart.getContainingTableExpression() );
				}
				else if ( modelPart instanceof EmbeddableValuedModelPart ) {
					compatibleTableExpressions = Collections.singleton( ( (EmbeddableValuedModelPart) modelPart ).getContainingTableExpression() );
				}
				else {
					compatibleTableExpressions = Collections.emptySet();
				}
				lateralTableGroup = new QueryPartTableGroup(
						queryPath,
						null,
						new SelectStatement( subQuerySpec ),
						identifierVariable,
						columnNames,
						compatibleTableExpressions,
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
					return new SelfRenderingFunctionSqlAstExpression(
							pathName,
							(sqlAppender, sqlAstArguments, returnType, walker) -> sqlAstArguments.get( 0 ).accept( walker ),
							resultColumnReferences,
							(ReturnableType<?>) resultColumnReferences.get( 0 ).getJdbcMapping(),
							resultColumnReferences.get( 0 ).getJdbcMapping()
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
		final QueryPartTableReference tableReference = (QueryPartTableReference) lateralTableGroup.getPrimaryTableReference();
		if ( jdbcTypeCount == 1 ) {
			final List<SqlSelection> sqlSelections = tableReference.getQueryPart()
					.getFirstQuerySpec()
					.getSelectClause()
					.getSqlSelections();
			return new SelfRenderingFunctionSqlAstExpression(
					pathName,
					(sqlAppender, sqlAstArguments, returnType, walker) -> sqlAstArguments.get( 0 ).accept( walker ),
					singletonList(
							new ColumnReference(
									identifierVariable,
									tableReference.getColumnNames().get( 0 ),
									false,
									null,
									sqlSelections.get( 0 ).getExpressionType().getSingleJdbcMapping()
							)
					),
					(ReturnableType<?>) sqlSelections.get( 0 ).getExpressionType().getSingleJdbcMapping(),
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

	private Expression withTreatRestriction(Expression expression, SqmPath<?> path) {
		final SqmPath<?> lhs;
		if ( path instanceof SqmTreatedPath<?, ?> ) {
			lhs = path;
		}
		else {
			lhs = path.getLhs();
		}
		if ( lhs instanceof SqmTreatedPath<?, ?> ) {
			final SqmTreatedPath<?, ?> treatedPath = (SqmTreatedPath<?, ?>) lhs;
			final ManagedDomainType<?> treatTarget = treatedPath.getTreatTarget();
			final Class<?> treatTargetJavaType = treatTarget.getJavaType();
			final SqmPath<?> wrappedPath = treatedPath.getWrappedPath();
			final Class<?> originalJavaType = wrappedPath.getJavaType();
			if ( treatTargetJavaType.isAssignableFrom( originalJavaType ) ) {
				// Treating a node to a super type can be ignored
				return expression;
			}
			if ( treatTarget instanceof EmbeddableDomainType<?> ) {
				// For embedded treats we simply register a TREAT use
				final TableGroup tableGroup = getFromClauseIndex().findTableGroup( wrappedPath.getNavigablePath() );
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
					final TableGroup tableGroup = getFromClauseIndex().findTableGroup( wrappedPath.getNavigablePath() );
					registerEntityNameUsage( tableGroup, EntityNameUse.TREAT, treatTarget.getTypeName(), false );
				}
				return expression;
			}
			final BasicValuedPathInterpretation<?> basicPath = (BasicValuedPathInterpretation<?>) expression;
			final TableGroup tableGroup = basicPath.getTableGroup();
			final TableGroup elementTableGroup = tableGroup instanceof PluralTableGroup
					? ( (PluralTableGroup) tableGroup ).getElementTableGroup()
					: tableGroup;
			final AbstractEntityPersister persister = (AbstractEntityPersister) elementTableGroup.getModelPart().getPartMappingType();
			// Only need a case expression around the basic valued path for the parent treat expression
			// if the column of the basic valued path is shared between subclasses
			if ( persister.isSharedColumn( basicPath.getColumnReference().getColumnExpression() ) ) {
				return createCaseExpression( wrappedPath, (EntityDomainType<?>) treatTarget, expression );
			}
		}
		return expression;
	}

	private Expression createCaseExpression(SqmPath<?> lhs, EntityDomainType<?> treatTarget, Expression expression) {
		final Predicate treatTypeRestriction = createTreatTypeRestriction( lhs, treatTarget );
		if ( treatTypeRestriction == null ) {
			return expression;
		}
		final BasicValuedMapping mappingModelExpressible = (BasicValuedMapping) expression.getExpressionType();
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

	private Predicate consumeConjunctTreatTypeRestrictions() {
		return consumeConjunctTreatTypeRestrictions( tableGroupEntityNameUses );
	}

	private Predicate consumeConjunctTreatTypeRestrictions(Map<TableGroup, Map<String, EntityNameUse>> conjunctTreatUsages) {
		if ( conjunctTreatUsages == null || conjunctTreatUsages.isEmpty() ) {
			return null;
		}
		Predicate predicate = null;
		for ( Map.Entry<TableGroup, Map<String, EntityNameUse>> entry : conjunctTreatUsages.entrySet() ) {
			final TableGroup tableGroup = entry.getKey();
			final ModelPartContainer modelPart = tableGroup.getModelPart();

			final Set<String> typeNames;
			final EntityMappingType entityMapping;
			final EmbeddableMappingType embeddableMapping;
			if ( modelPart instanceof PluralAttributeMapping ) {
				entityMapping = (EntityMappingType) ( (PluralAttributeMapping) modelPart ).getElementDescriptor()
						.getPartMappingType();
				embeddableMapping = null;
			}
			else if ( modelPart instanceof EntityValuedModelPart ) {
				entityMapping = ( (EntityValuedModelPart) modelPart ).getEntityMappingType();
				embeddableMapping = null;
			}
			else if ( modelPart instanceof EmbeddableValuedModelPart ) {
				embeddableMapping = ( (EmbeddableValuedModelPart) modelPart ).getEmbeddableTypeDescriptor();
				entityMapping = null;
			}
			else {
				throw new IllegalStateException( "Unrecognized model part for treated table group: " + tableGroup );
			}

			final DiscriminatorPathInterpretation<?> typeExpression;
			if ( entityMapping != null ) {
				typeNames = determineEntityNamesForTreatTypeRestriction( entityMapping, entry.getValue() );
				if ( typeNames.isEmpty() ) {
					continue;
				}
				typeExpression = new DiscriminatorPathInterpretation<>(
						tableGroup.getNavigablePath().append( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME ),
						entityMapping,
						tableGroup,
						this
				);
				registerTypeUsage( tableGroup );
			}
			else {
				assert embeddableMapping != null;
				typeNames = determineEmbeddableNamesForTreatTypeRestriction( embeddableMapping, entry.getValue() );
				if ( typeNames.isEmpty() ) {
					continue;
				}
				typeExpression = new DiscriminatorPathInterpretation<>(
						tableGroup.getNavigablePath().append( EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME ),
						embeddableMapping.getDiscriminatorMapping(),
						tableGroup,
						this
				);
			}

			// We need to check if this is a treated left or full join, which case we should
			// allow null discriminator values to maintain correct semantics
			final TableGroupJoin join = getParentTableGroupJoin( tableGroup );
			final boolean allowNulls = join != null && ( join.getJoinType() == SqlAstJoinType.LEFT || join.getJoinType() == SqlAstJoinType.FULL );
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
		final NavigablePath parentNavigablePath = tableGroup.getNavigablePath().getParent();
		if ( parentNavigablePath != null ) {
			final TableGroup parentTableGroup = getFromClauseIndex().findTableGroup( parentNavigablePath );
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
		for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
			if ( entry.getValue() == EntityNameUse.PROJECTION ) {
				continue;
			}
			entityNameUsesSet.add( entry.getKey() );
		}

		if ( entityNameUsesSet.containsAll( partMappingType.getSubclassEntityNames() ) ) {
			// No need to create a restriction if all subclasses are used
			return Collections.emptySet();
		}
		if ( entityNameUses.containsValue( EntityNameUse.FILTER ) ) {
			// If the conjunct contains FILTER uses we can omit the treat type restriction
			return Collections.emptySet();
		}
		final String baseEntityNameToAdd;
		if ( entityNameUsesSet.contains( partMappingType.getEntityName() ) ) {
			if ( !partMappingType.isAbstract() ) {
				baseEntityNameToAdd = partMappingType.getEntityName();
			}
			else {
				baseEntityNameToAdd = null;
			}
			if ( entityNameUses.size() == 1 ) {
				return Collections.emptySet();
			}
		}
		else {
			baseEntityNameToAdd = null;
		}
		final Set<String> entityNames = new HashSet<>( entityNameUsesSet.size() );
		for ( Map.Entry<String, EntityNameUse> entityNameUse : entityNameUses.entrySet() ) {
			if ( entityNameUse.getValue() == EntityNameUse.TREAT ) {
				final String entityName = entityNameUse.getKey();
				final EntityPersister entityDescriptor = creationContext.getMappingMetamodel()
						.findEntityDescriptor( entityName );
				if ( !entityDescriptor.isAbstract() ) {
					entityNames.add( entityDescriptor.getEntityName() );
				}
				for ( EntityMappingType subMappingType : entityDescriptor.getSubMappingTypes() ) {
					if ( !subMappingType.isAbstract() ) {
						entityNames.add( subMappingType.getEntityName() );
					}
				}
			}
		}
		do {
			entityNames.remove( partMappingType.getEntityName() );
			partMappingType = partMappingType.getSuperMappingType();
		} while ( partMappingType != null );
		if ( !entityNames.isEmpty() && baseEntityNameToAdd != null ) {
			entityNames.add( baseEntityNameToAdd );
		}
		return entityNames;
	}

	private Set<String> determineEmbeddableNamesForTreatTypeRestriction(
			EmbeddableMappingType embeddableMappingType,
			Map<String, EntityNameUse> entityNameUses) {
		final EmbeddableDomainType<?> embeddableDomainType = creationContext.getSessionFactory()
				.getRuntimeMetamodels()
				.getJpaMetamodel()
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
		final AbstractEntityPersister entityDescriptor = (AbstractEntityPersister) domainModel.findEntityDescriptor( treatTarget.getHibernateEntityName() );
		if ( entityDescriptor.isPolymorphic() && lhs.getNodeType() != treatTarget ) {
			final Set<String> subclassEntityNames = entityDescriptor.getSubclassEntityNames();
			return createTreatTypeRestriction( lhs, subclassEntityNames );
		}
		return null;
	}

	private Predicate createTreatTypeRestriction(SqmPath<?> lhs, Set<String> subclassEntityNames) {
		// Do what visitSelfInterpretingSqmPath does, except for calling preparingReusablePath
		// as that would register a type usage for the table group that we don't want here
		final EntityDiscriminatorSqmPath discriminatorSqmPath = (EntityDiscriminatorSqmPath) lhs.type();
		registerTypeUsage( discriminatorSqmPath );
		return createTreatTypeRestriction(
				DiscriminatorPathInterpretation.from( discriminatorSqmPath, this ),
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
			return new EntityTypeLiteral( domainModel.findEntityDescriptor( typeName ) );
		}
		else {
			final EmbeddableDomainType<?> embeddable = creationContext.getSessionFactory()
					.getRuntimeMetamodels()
					.getJpaMetamodel()
					.embeddable( typeName );
			return new EmbeddableTypeLiteral(
					embeddable,
					(BasicType<?>) typeExpression.getExpressionType().getSingleJdbcMapping()
			);
		}
	}

	private MappingModelExpressible<?> resolveInferredType() {
		final Supplier<MappingModelExpressible<?>> inferableTypeAccess = inferrableTypeAccessStack.getCurrent();
		if ( inTypeInference || inferableTypeAccess == null ) {
			return null;
		}
		inTypeInference = true;
		final MappingModelExpressible<?> inferredType = inferableTypeAccess.get();
		inTypeInference = false;
		return inferredType;
	}

	@Override
	public boolean isInTypeInference() {
		return inImpliedResultTypeInference || inTypeInference;
	}

	@Override
	public MappingModelExpressible<?> resolveFunctionImpliedReturnType() {
		if ( inImpliedResultTypeInference || inTypeInference || functionImpliedResultTypeAccess == null ) {
			return null;
		}
		inImpliedResultTypeInference = true;
		final MappingModelExpressible<?> inferredType = functionImpliedResultTypeAccess.get();
		inImpliedResultTypeInference = false;
		return inferredType;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// General expressions

	@Override
	public Expression visitLiteral(SqmLiteral<?> literal) {
		if ( literal instanceof SqmLiteralNull ) {
			MappingModelExpressible<?> mappingModelExpressible = resolveInferredType();
			if ( mappingModelExpressible == null ) {
				mappingModelExpressible = determineCurrentExpressible( literal );
			}
			if ( mappingModelExpressible instanceof BasicValuedMapping ) {
				return new QueryLiteral<>( null, (BasicValuedMapping) mappingModelExpressible );
			}
			else if ( mappingModelExpressible instanceof EntityMappingType ) {
				// When selecting a literal null entity type, we should simply return a single null object
				mappingModelExpressible = null;
			}
			final MappingModelExpressible<?> keyExpressible = getKeyExpressible( mappingModelExpressible );
			if ( keyExpressible == null ) {
				// treat Void as the bottom type, the class of null
				return new QueryLiteral<>( null, BottomType.INSTANCE );
			}

			final List<Expression> expressions = new ArrayList<>( keyExpressible.getJdbcTypeCount() );

			if ( keyExpressible instanceof ModelPart ) {
				// Use the selectable mappings as BasicValuedMapping if possible to retain column definition info for possible casts
				( (ModelPart) keyExpressible ).forEachSelectable(
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
			return new SqlTuple( expressions, mappingModelExpressible);
		}

		final MappingModelExpressible<?> inferableExpressible = getInferredValueMapping();

		if ( inferableExpressible instanceof DiscriminatorMapping ) {
			final MappingMetamodelImplementor mappingMetamodel = creationContext.getSessionFactory().getMappingMetamodel();
			final Object literalValue = literal.getLiteralValue();
			final EntityPersister entityDescriptor;
			if ( literalValue instanceof Class<?> ) {
				entityDescriptor = mappingMetamodel.findEntityDescriptor( (Class<?>) literalValue );
			}
			else {
				final DiscriminatorMapping discriminatorMapping = (DiscriminatorMapping) inferableExpressible;
				//noinspection unchecked
				final DiscriminatorConverter<?, Object> valueConverter = (DiscriminatorConverter<?, Object>) discriminatorMapping.getValueConverter();
				final DiscriminatorValueDetails discriminatorDetails;
				if ( valueConverter.getDomainJavaType().isInstance( literalValue ) ) {
					discriminatorDetails = valueConverter.getDetailsForDiscriminatorValue( literalValue );
				}
				else if ( valueConverter.getRelationalJavaType().isInstance( literalValue ) ) {
					discriminatorDetails = valueConverter.getDetailsForRelationalForm( literalValue );
				}
				else {
					// Special case when passing the discriminator value as e.g. string literal,
					// but the expected relational type is Character.
					// In this case, we use wrap to transform the value to the correct type
					final Object relationalForm = valueConverter.getRelationalJavaType().wrap(
							literalValue,
							creationContext.getSessionFactory().getWrapperOptions()
					);
					discriminatorDetails = valueConverter.getDetailsForRelationalForm( relationalForm );
				}
				entityDescriptor = discriminatorDetails.getIndicatedEntity().getEntityPersister();
			}
			return new EntityTypeLiteral( entityDescriptor );
		}
		else if ( inferableExpressible instanceof BasicValuedMapping ) {
			final BasicValuedMapping basicValuedMapping = (BasicValuedMapping) inferableExpressible;
			final BasicValueConverter valueConverter = basicValuedMapping.getJdbcMapping().getValueConverter();
			if ( valueConverter != null ) {
				return new QueryLiteral<>( sqlLiteralValue( valueConverter, literal.getLiteralValue() ), basicValuedMapping );
			}
		}


		final MappingModelExpressible<?> expressible;
		final MappingModelExpressible<?> localExpressible = SqmMappingModelHelper.resolveMappingModelExpressible(
				literal,
				creationContext.getSessionFactory().getRuntimeMetamodels().getMappingMetamodel(),
				getFromClauseAccess() == null ? null : getFromClauseAccess()::findTableGroup
		);
		if ( localExpressible == null ) {
			expressible = getElementExpressible( inferableExpressible );
		}
		else {
			final MappingModelExpressible<?> elementExpressible = getElementExpressible( localExpressible );
			if ( elementExpressible instanceof BasicType ) {
				expressible = InferredBasicValueResolver.resolveSqlTypeIndicators(
						this,
						(BasicType) elementExpressible,
						literal.getJavaTypeDescriptor()
				);
			}
			else {
				expressible = elementExpressible;
			}
		}

		if ( expressible instanceof EntityIdentifierMapping && literal.getNodeType() instanceof EntityTypeImpl ) {
			return new QueryLiteral<>(
					( (EntityIdentifierMapping) expressible ).getIdentifier( literal.getLiteralValue() ),
					(BasicValuedMapping) expressible
			);
		}

		if ( expressible instanceof BasicValuedMapping ) {
			return new QueryLiteral<>(
					literal.getLiteralValue(),
					(BasicValuedMapping) expressible
			);
		}
		// Handling other values might seem unnecessary, but with JPA Criteria it is totally possible to have such literals
		if ( expressible instanceof EmbeddableValuedModelPart ) {
			final EmbeddableValuedModelPart embeddableValuedModelPart = (EmbeddableValuedModelPart) expressible;
			final List<Expression> list = new ArrayList<>( embeddableValuedModelPart.getJdbcTypeCount() );
			embeddableValuedModelPart.forEachJdbcValue(
					literal.getLiteralValue(),
					list,
					null,
					(selectionIndex, expressions, noop, value, jdbcMapping)
							-> expressions.add( new QueryLiteral<>( value, (BasicValuedMapping) jdbcMapping ) ),
					null
			);
			return new SqlTuple( list, expressible );
		}
		else if ( expressible instanceof EntityValuedModelPart ) {
			final EntityValuedModelPart entityValuedModelPart = (EntityValuedModelPart) expressible;
			final Object associationKey;
			final ModelPart associationKeyPart;
			if ( entityValuedModelPart instanceof EntityAssociationMapping ) {
				final EntityAssociationMapping association = (EntityAssociationMapping) entityValuedModelPart;
				final ForeignKeyDescriptor foreignKeyDescriptor = association.getForeignKeyDescriptor();
				if ( association.getSideNature() == ForeignKeyDescriptor.Nature.TARGET ) {
					// If the association is the target, we must use the identifier of the EntityMappingType
					associationKey = association.getAssociatedEntityMappingType().getIdentifierMapping()
							.getIdentifier( literal.getLiteralValue() );
					associationKeyPart = association.getAssociatedEntityMappingType().getIdentifierMapping();
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
				final EntityIdentifierMapping identifierMapping = entityValuedModelPart.getEntityMappingType()
						.getIdentifierMapping();
				associationKeyPart = identifierMapping;
				associationKey = identifierMapping.getIdentifier( literal.getLiteralValue() );
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
						list,
						null,
						(selectionIndex, expressions, noop, value, jdbcMapping)
								-> expressions.add( new QueryLiteral<>( value, (BasicValuedMapping) jdbcMapping ) ),
						null
				);
				return new SqlTuple( list, associationKeyPart );
			}
		}
		else {
			return new QueryLiteral<>(
					literal.getLiteralValue(),
					creationContext.getSessionFactory()
							.getTypeConfiguration()
							.getBasicTypeRegistry()
							.getRegisteredType(
									( (BasicSqmPathSource<?>) literal.getNodeType() ).getSqmPathType()
											.getJavaType()
											.getName()
							)
			);
		}
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
				&& value instanceof CharSequence && ( (CharSequence) value ).length() == 1 ) {
			return ( (CharSequence) value ).charAt( 0 );
		}
		// In HQL, number literals might not match the relational java type exactly,
		// so we allow coercion between the number types
		else if ( Number.class.isAssignableFrom( valueConverter.getRelationalJavaType().getJavaTypeClass() )
				&& value instanceof Number ) {
			return valueConverter.getRelationalJavaType().coerce(
					value,
					creationContext.getSessionFactory()::getTypeConfiguration
			);
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

	@Override
	public <N extends Number> Expression visitHqlNumericLiteral(SqmHqlNumericLiteral<N> numericLiteral) {
		final BasicValuedMapping inferredExpressible = (BasicValuedMapping) getInferredValueMapping();
		final BasicValuedMapping expressible;
		if ( inferredExpressible == null ) {
			expressible = (BasicValuedMapping) determineCurrentExpressible( numericLiteral );
		}
		else {
			expressible = inferredExpressible;
		}

		final JdbcMapping jdbcMapping = expressible.getJdbcMapping();
		if ( jdbcMapping.getValueConverter() != null ) {
			// special case where we need to parse the value in order to apply the conversion
			return handleConvertedUnparsedNumericLiteral( numericLiteral, expressible );
		}

		return new UnparsedNumericLiteral<>(
				numericLiteral.getUnparsedLiteralValue(),
				numericLiteral.getTypeCategory(),
				jdbcMapping
		);
	}

	private <N extends Number> Expression handleConvertedUnparsedNumericLiteral(
			SqmHqlNumericLiteral<N> numericLiteral,
			BasicValuedMapping expressible) {
		//noinspection rawtypes
		final BasicValueConverter valueConverter = expressible.getJdbcMapping().getValueConverter();
		assert valueConverter != null;

		final Number parsedValue = numericLiteral.getTypeCategory().parseLiteralValue( numericLiteral.getUnparsedLiteralValue() );
		final Object sqlLiteralValue;
		if ( valueConverter.getDomainJavaType().isInstance( parsedValue ) ) {
			//noinspection unchecked
			sqlLiteralValue = valueConverter.toRelationalValue( parsedValue );
		}
		else if ( valueConverter.getRelationalJavaType().isInstance( parsedValue ) ) {
			sqlLiteralValue = parsedValue;
		}
		else if ( Number.class.isAssignableFrom( valueConverter.getRelationalJavaType().getJavaTypeClass() ) ) {
			//noinspection unchecked
			sqlLiteralValue = valueConverter.getRelationalJavaType().coerce(
					parsedValue,
					creationContext.getSessionFactory()::getTypeConfiguration
			);
		}
		else {
			throw new SemanticException(
					String.format(
							Locale.ROOT,
							"Literal type '%s' did not match domain type '%s' nor converted type '%s'",
							parsedValue.getClass(),
							valueConverter.getDomainJavaType().getJavaTypeClass().getName(),
							valueConverter.getRelationalJavaType().getJavaTypeClass().getName()
					)
			);
		}

		return new QueryLiteral<>( sqlLiteralValue, expressible );
	}

	private MappingModelExpressible<?> getKeyExpressible(JdbcMappingContainer mappingModelExpressible) {
		if ( mappingModelExpressible instanceof EntityAssociationMapping ) {
			return ( (EntityAssociationMapping) mappingModelExpressible ).getKeyTargetMatchPart();
		}
		else {
			return (MappingModelExpressible<?>) mappingModelExpressible;
		}
	}

	private MappingModelExpressible<?> getElementExpressible(MappingModelExpressible<?> mappingModelExpressible) {
		if ( mappingModelExpressible instanceof PluralAttributeMapping ) {
			return ( (PluralAttributeMapping) mappingModelExpressible).getElementDescriptor();
		}
		else {
			return mappingModelExpressible;
		}
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

		final QueryParameterImplementor<?> queryParameter = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding binding = domainParameterBindings.getBinding( queryParameter );
		if ( binding.setType( valueMapping ) ) {
			replaceJdbcParametersType(
					sqmParameter,
					domainParameterXref.getSqmParameters( queryParameter ),
					valueMapping
			);
		}
		return new SqmParameterInterpretation(
				jdbcParametersForSqm,
				valueMapping
		);
	}

	private void replaceJdbcParametersType(
			SqmParameter<?> sourceSqmParameter,
			List<SqmParameter<?>> sqmParameters,
			MappingModelExpressible<?> valueMapping) {
		final JdbcMapping jdbcMapping = valueMapping.getSingleJdbcMapping();
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
					if ( ( (SqlExpressible) jdbcParameter ).getJdbcMapping() != jdbcMapping ) {
						final JdbcParameter newJdbcParameter = new JdbcParameterImpl( jdbcMapping );
						parameters.set( 0, newJdbcParameter );
						jdbcParameters.getJdbcParameters().remove( jdbcParameter );
						jdbcParameters.getJdbcParameters().add( newJdbcParameter );
					}
				}
			}
		}
	}

	protected Expression consumeSqmParameter(SqmParameter<?> sqmParameter) {
		if ( sqmParameter.allowMultiValuedBinding() ) {
			final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter( sqmParameter );
			final QueryParameterBinding<?> domainParamBinding = domainParameterBindings.getBinding( domainParam );

			if ( !domainParamBinding.isMultiValued() ) {
				return consumeSingleSqmParameter( sqmParameter );
			}

			final Collection<?> bindValues = domainParamBinding.getBindValues();
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
		else {
			return consumeSingleSqmParameter( sqmParameter );
		}
	}

	protected Expression consumeSingleSqmParameter(SqmParameter<?> sqmParameter) {
		return consumeSqmParameter( sqmParameter, determineValueMapping( sqmParameter ), (integer, jdbcParameter) -> {} );
	}

	@Override
	public MappingModelExpressible<?> determineValueMapping(SqmExpression<?> sqmExpression) {
		return determineValueMapping( sqmExpression, fromClauseIndexStack.getCurrent() );
	}

	private MappingModelExpressible<?> determineValueMapping(SqmExpression<?> sqmExpression, FromClauseIndex fromClauseIndex) {
		if ( sqmExpression instanceof SqmParameter ) {
			return determineValueMapping( (SqmParameter<?>) sqmExpression );
		}

		if ( sqmExpression instanceof SqmPath ) {
			log.debugf( "Determining mapping-model type for SqmPath : %s ", sqmExpression );

			final MappingModelExpressible<?> mappingModelExpressible = SqmMappingModelHelper.resolveMappingModelExpressible(
					sqmExpression,
					domainModel,
					fromClauseIndex::findTableGroup
			);
			if ( mappingModelExpressible != null ) {
				return mappingModelExpressible;
			}
		}

		if ( sqmExpression instanceof SqmBooleanExpressionPredicate ) {
			final SqmBooleanExpressionPredicate expressionPredicate = (SqmBooleanExpressionPredicate) sqmExpression;
			return determineValueMapping( expressionPredicate.getBooleanExpression(), fromClauseIndex );
		}

		// The model type of an enum literal is always inferred
		if ( sqmExpression instanceof SqmEnumLiteral<?> ) {
			final MappingModelExpressible<?> mappingModelExpressible = resolveInferredType();
			if ( mappingModelExpressible != null ) {
				return mappingModelExpressible;
			}
		}

		if ( sqmExpression instanceof SqmSubQuery<?> ) {
			final SqmSubQuery<?> subQuery = (SqmSubQuery<?>) sqmExpression;
			final SqmSelectClause selectClause = subQuery.getQuerySpec().getSelectClause();
			if ( selectClause.getSelections().size() == 1 ) {
				final SqmSelection<?> subQuerySelection = selectClause.getSelections().get( 0 );
				final SqmSelectableNode<?> selectableNode = subQuerySelection.getSelectableNode();
				if ( selectableNode instanceof SqmExpression<?> ) {
					return determineValueMapping( (SqmExpression<?>) selectableNode, fromClauseIndex );
				}
				final SqmExpressible<?> selectionNodeType = subQuerySelection.getNodeType();
				if ( selectionNodeType != null ) {
					final MappingModelExpressible<?> expressible = domainModel.resolveMappingExpressible(selectionNodeType, this::findTableGroupByPath );

					if ( expressible != null ) {
						return expressible;
					}

					try {
						final MappingModelExpressible<?> mappingModelExpressible = resolveInferredType();
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

		if ( sqmExpression instanceof SelfRenderingSqmFunction<?> ) {
			return domainModel.resolveMappingExpressible(
					( (SelfRenderingSqmFunction<?>) sqmExpression ).resolveResultType( this ),
					this::findTableGroupByPath
			);
		}

		if ( sqmExpression instanceof SqmBinaryArithmetic<?> ) {
			final SqmBinaryArithmetic<?> binaryArithmetic = (SqmBinaryArithmetic<?>) sqmExpression;
			if ( binaryArithmetic.getNodeType() == null ) {
				final MappingModelExpressible<?> lhs = determineValueMapping( binaryArithmetic.getLeftHandOperand() );
				final MappingModelExpressible<?> rhs = determineValueMapping( binaryArithmetic.getRightHandOperand() );
				// This cast should be fine since the result of this will be a BasicType
				return (MappingModelExpressible<?>) getTypeConfiguration().resolveArithmeticType(
						// These casts should be safe, since the only JdbcMapping is BasicType
						// which also implements SqmExpressible
						lhs == null ? null : (SqmExpressible<?>) lhs.getSingleJdbcMapping(),
						rhs == null ? null : (SqmExpressible<?>) rhs.getSingleJdbcMapping()
				);
			}
		}

		log.debugf( "Determining mapping-model type for generalized SqmExpression : %s", sqmExpression );
		final SqmExpressible<?> nodeType = sqmExpression.getNodeType();
		if ( nodeType == null ) {
			// We can't determine the type of the expression
			return null;
		}

		if ( nodeType instanceof EmbeddedSqmPathSource<?> ) {
			if ( sqmExpression instanceof SqmBinaryArithmetic<?> ) {
				final SqmBinaryArithmetic<?> binaryArithmetic = (SqmBinaryArithmetic<?>) sqmExpression;
				if ( binaryArithmetic.getLeftHandOperand().getNodeType() == nodeType ) {
					return determineValueMapping( binaryArithmetic.getLeftHandOperand(), fromClauseIndex );
				}
				else if ( binaryArithmetic.getRightHandOperand().getNodeType() == nodeType ) {
					return determineValueMapping( binaryArithmetic.getRightHandOperand(), fromClauseIndex );
				}
			}
		}


		final MappingModelExpressible<?> valueMapping = domainModel.resolveMappingExpressible(
				nodeType,
				fromClauseIndex::getTableGroup
		);

		if ( valueMapping == null ) {
			final MappingModelExpressible<?> mappingModelExpressible = resolveInferredType();
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
		final MappingModelExpressible<?> inferredMapping = resolveInferredType();
		if ( inferredMapping != null ) {
			if ( inferredMapping instanceof PluralAttributeMapping ) {
				return ( (PluralAttributeMapping) inferredMapping ).getElementDescriptor();
			}
			else if ( !( inferredMapping instanceof JavaObjectType ) ) {
				// Never report back the "object type" as inferred type and instead rely on the value type
				return inferredMapping;
			}
		}
		return null;
	}

	protected MappingModelExpressible<?> determineValueMapping(SqmParameter<?> sqmParameter) {
		log.debugf( "Determining mapping-model type for SqmParameter : %s", sqmParameter );

		final QueryParameterImplementor<?> queryParameter = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding<?> binding = domainParameterBindings.getBinding( queryParameter );

		BindableType<?> paramType = binding.getBindType();
		final boolean bindingTypeExplicit;
		bindingTypeExplicit = binding.getExplicitTemporalPrecision() != null;
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

		if ( paramType == null ) {
			final MappingModelExpressible<?> inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return resolveInferredValueMappingForParameter( inferredValueMapping );
			}
			// Default to the Object type
			return basicType( Object.class );
		}
		else if ( paramType instanceof MappingModelExpressible<?> ) {
			final MappingModelExpressible<?> paramModelType = (MappingModelExpressible<?>) paramType;
			final MappingModelExpressible<?> inferredValueMapping = getInferredValueMapping();
			// Prefer the model part type instead of the bind type if possible as the model part type contains size information
			if ( inferredValueMapping instanceof ModelPart ) {
				final JdbcMapping paramJdbcMapping = paramModelType.getSingleJdbcMapping();
				final JdbcMapping inferredJdbcMapping = inferredValueMapping.getSingleJdbcMapping();
				// Only use the inferred mapping as parameter type when the JavaType accepts values of the bind type
				if ( ( inferredJdbcMapping.getMappedJavaType() == paramJdbcMapping.getMappedJavaType()
						|| inferredJdbcMapping.getMappedJavaType().isWider( paramJdbcMapping.getMappedJavaType() ) )
						// and the bind type is not explicit or the bind type has the same JDBC type
						&& ( !bindingTypeExplicit || canUseInferredType( paramJdbcMapping, inferredJdbcMapping ) ) ) {
					return resolveInferredValueMappingForParameter( inferredValueMapping );
				}
			}
			return paramModelType;
		}
		else if ( sqmParameter.getAnticipatedType() == null ) {
			// this should indicate the condition that the user query did not define an
			// explicit type in regard to this parameter.  Here we should prefer the
			// inferrable type and fallback to resolving the binding type
			final MappingModelExpressible<?> inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return resolveInferredValueMappingForParameter( inferredValueMapping );
			}
		}
		else if ( paramType instanceof EntityDomainType ) {
			// In JPA Criteria, it is possible to define a parameter of an entity type,
			// but that should infer the mapping type from context,
			// otherwise this would default to binding the PK which might be wrong
			final MappingModelExpressible<?> inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return resolveInferredValueMappingForParameter( inferredValueMapping );
			}
		}

		final SqmExpressible<?> paramSqmType = paramType.resolveExpressible( creationContext.getSessionFactory() );

		if ( paramSqmType instanceof SqmPath ) {
			final MappingModelExpressible<?> modelPart = determineValueMapping( (SqmPath<?>) paramSqmType );
			if ( modelPart instanceof PluralAttributeMapping ) {
				return resolveInferredValueMappingForParameter( ( (PluralAttributeMapping) modelPart ).getElementDescriptor() );
			}
			return modelPart;
		}

		if ( paramSqmType instanceof BasicValuedMapping ) {
			return (BasicValuedMapping) paramSqmType;
		}

		if ( paramSqmType instanceof CompositeSqmPathSource || paramSqmType instanceof EmbeddableDomainType<?> ) {
			// Try to infer the value mapping since the other side apparently is a path source
			final MappingModelExpressible<?> inferredValueMapping = getInferredValueMapping();
			if ( inferredValueMapping != null ) {
				return resolveInferredValueMappingForParameter( inferredValueMapping );
			}
			throw new UnsupportedOperationException( "Support for embedded-valued parameters not yet implemented" );
		}

		if ( paramSqmType instanceof AnyDiscriminatorSqmPathSource ) {
			return (MappingModelExpressible<?>) ((AnyDiscriminatorSqmPathSource<?>) paramSqmType).getSqmPathType();
		}

		if ( paramSqmType instanceof SqmPathSource<?> || paramSqmType instanceof BasicDomainType<?> ) {
			// Try to infer the value mapping since the other side apparently is a path source
			final MappingModelExpressible<?> inferredMapping = resolveInferredType();
			if ( inferredMapping != null ) {
				if ( inferredMapping instanceof PluralAttributeMapping ) {
					return resolveInferredValueMappingForParameter( ( (PluralAttributeMapping) inferredMapping ).getElementDescriptor() );
				}
				else if ( !( inferredMapping instanceof JavaObjectType ) ) {
					// Do not report back the "object type" as inferred type and instead try to rely on the paramSqmType.getExpressibleJavaType()
					return resolveInferredValueMappingForParameter( inferredMapping );
				}
			}

			final Class<?> parameterJavaType = paramSqmType.getExpressibleJavaType().getJavaTypeClass();

			final BasicType<?> basicTypeForJavaType = getTypeConfiguration().getBasicTypeForJavaType(
					parameterJavaType
			);

			if ( basicTypeForJavaType == null ) {
				if ( paramSqmType instanceof EntityDomainType ) {
					return resolveEntityPersister( (EntityDomainType<?>) paramSqmType );
				}
				else if ( paramSqmType instanceof SingularAttribute ) {
					final Type<?> type = ( (SingularAttribute<?, ?>) paramSqmType ).getType();
					if ( type instanceof EntityDomainType ) {
						return resolveEntityPersister( (EntityDomainType<?>) type );
					}
				}
				if ( inferredMapping != null ) {
					// inferredMapping is JavaObjectType and we cannot deduct the type
					return inferredMapping;
				}
			}
			return basicTypeForJavaType;
		}
		throw new ConversionException( "Could not determine ValueMapping for SqmParameter: " + sqmParameter );
	}

	private static boolean canUseInferredType(JdbcMapping bindJdbcMapping, JdbcMapping inferredJdbcMapping) {
		final JdbcType bindJdbcType = bindJdbcMapping.getJdbcType();
		final JdbcType inferredJdbcType = inferredJdbcMapping.getJdbcType();
		// If the bind type has a different JDBC type, we prefer that over the inferred type.
		return bindJdbcType == inferredJdbcType
				|| bindJdbcType instanceof ArrayJdbcType && inferredJdbcType instanceof ArrayJdbcType
				&& ( (ArrayJdbcType) bindJdbcType ).getElementJdbcType() == ( (ArrayJdbcType) inferredJdbcType ).getElementJdbcType();
	}

	private MappingModelExpressible<?> resolveInferredValueMappingForParameter(MappingModelExpressible<?> inferredValueMapping) {
		if ( inferredValueMapping instanceof PluralAttributeMapping ) {
			// For parameters, we resolve to the element descriptor
			inferredValueMapping = ( (PluralAttributeMapping) inferredValueMapping ).getElementDescriptor();
		}
		if ( inferredValueMapping instanceof EntityCollectionPart ) {
			// For parameters, we resolve to the entity mapping type to bind the primary key
			return ( (EntityCollectionPart) inferredValueMapping ).getAssociatedEntityMappingType();
		}
		return inferredValueMapping;
	}

	private void resolveSqmParameter(
			SqmParameter<?> expression,
			MappingModelExpressible<?> valueMapping,
			BiConsumer<Integer,JdbcParameter> jdbcParameterConsumer) {
		sqmParameterMappingModelTypes.put( expression, valueMapping );
		final Bindable bindable;
		if ( valueMapping instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping mapping = (EntityAssociationMapping) valueMapping;
			bindable = mapping.getForeignKeyDescriptor().getPart( mapping.getSideNature() );
		}
		else if ( valueMapping instanceof EntityMappingType ) {
			bindable = ( (EntityMappingType) valueMapping ).getIdentifierMapping();
		}
		else {
			bindable = valueMapping;
		}
		if ( bindable instanceof SelectableMappings ) {
			( (SelectableMappings) bindable ).forEachSelectable(
					(index, selectableMapping) -> jdbcParameterConsumer.accept( index, new SqlTypedMappingJdbcParameter( selectableMapping ) )
			);
		}
		else if ( bindable instanceof SelectableMapping ) {
			jdbcParameterConsumer.accept( 0, new SqlTypedMappingJdbcParameter( (SelectableMapping) bindable ) );
		}
		else {
			SqlTypedMapping sqlTypedMapping = null;
			if ( bindable instanceof BasicType<?> ) {
				final int sqlTypeCode = ( (BasicType<?>) bindable ).getJdbcType().getDdlTypeCode();
				if ( sqlTypeCode == SqlTypes.NUMERIC || sqlTypeCode == SqlTypes.DECIMAL ) {
					// For numeric and decimal parameter types we must determine the precision/scale of the value.
					// When we need to cast the parameter later, it is necessary to know the size to avoid truncation.
					final QueryParameterBinding<?> binding = domainParameterBindings.getBinding(
							domainParameterXref.getQueryParameter( expression )
					);
					final Object bindValue;
					if ( binding.isMultiValued() ) {
						final Collection<?> bindValues = binding.getBindValues();
						bindValue = !bindValues.isEmpty() ? bindValues.iterator().next() : null;
					}
					else {
						bindValue = binding.getBindValue();
					}
					if ( bindValue != null ) {
						if ( bindValue instanceof BigInteger ) {
							int precision = bindValue.toString().length() - ( ( (BigInteger) bindValue ).signum() < 0 ? 1 : 0 );
							sqlTypedMapping = new SqlTypedMappingImpl(
									null,
									null,
									precision,
									0,
									null,
									( (BasicType<?>) bindable ).getJdbcMapping()
							);
						}
						else if ( bindValue instanceof BigDecimal ) {
							final BigDecimal bigDecimal = (BigDecimal) bindValue;
							sqlTypedMapping = new SqlTypedMappingImpl(
									null,
									null,
									bigDecimal.precision(),
									bigDecimal.scale(),
									null,
									( (BasicType<?>) bindable ).getJdbcMapping()
							);
						}
					}
				}
			}
			if ( sqlTypedMapping == null ) {
				if ( bindable == null ) {
					throw new ConversionException(
							"Could not determine neither the SqlTypedMapping nor the Bindable value for SqmParameter: " + expression );
				}
				bindable.forEachJdbcType(
						(index, jdbcMapping) -> jdbcParameterConsumer.accept(
								index,
								new JdbcParameterImpl( jdbcMapping )
						)
				);
			}
			else {
				jdbcParameterConsumer.accept( 0, new SqlTypedMappingJdbcParameter( sqlTypedMapping ) );
			}
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

		final SqmJpaCriteriaParameterWrapper<?> supplier = jpaCriteriaParamResolutions.get( expression );
		if ( supplier == null ) {
			throw new IllegalStateException( "Criteria parameter [" + expression + "] not known to be a parameter of the processing tree" );
		}
		return supplier;
	}

	@Override
	public Object visitTuple(SqmTuple<?> sqmTuple) {
		final List<SqmExpression<?>> groupedExpressions = sqmTuple.getGroupedExpressions();
		final int size = groupedExpressions.size();
		final List<Expression> expressions = new ArrayList<>( size );
		final MappingModelExpressible<?> mappingModelExpressible = resolveInferredType();
		final EmbeddableMappingType embeddableMappingType;
		if ( mappingModelExpressible instanceof ValueMapping ) {
			embeddableMappingType = (EmbeddableMappingType) ( (ValueMapping) mappingModelExpressible).getMappedType();
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
				final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMapping( i );
				inferrableTypeAccessStack.push( () -> attributeMapping );
				try {
					expressions.add( (Expression) groupedExpressions.get( i ).accept( this ) );
				}
				finally {
					inferrableTypeAccessStack.pop();
				}
			}
		}
		final MappingModelExpressible<?> valueMapping;
		if ( mappingModelExpressible != null ) {
			valueMapping = mappingModelExpressible;
		}
		else {
			final SqmExpressible<?> expressible = sqmTuple.getExpressible();
			if ( expressible instanceof MappingModelExpressible<?>) {
				valueMapping = (MappingModelExpressible<?>) expressible;
			}
			else {
				valueMapping = null;
			}
		}
		return new SqlTuple( expressions, valueMapping );
	}

	@Override
	public Object visitCollation(SqmCollation sqmCollation) {
		return new Collation( sqmCollation.getLiteralValue() );
	}

	@Override
	public Expression visitFunction(SqmFunction<?> sqmFunction) {
		final boolean oldInNestedContext = inNestedContext;
		inNestedContext = true;
		final Supplier<MappingModelExpressible<?>> oldFunctionImpliedResultTypeAccess = functionImpliedResultTypeAccess;
		functionImpliedResultTypeAccess = inferrableTypeAccessStack.getCurrent();
		inferrableTypeAccessStack.push( () -> null );
		try {
			return sqmFunction.convertToSqlAst( this );
		}
		finally {
			inferrableTypeAccessStack.pop();
			functionImpliedResultTypeAccess = oldFunctionImpliedResultTypeAccess;
			inNestedContext = oldInNestedContext;
		}
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
		final Expression expression = (Expression) over.getExpression().accept( this );

		final List<Expression> partitions = new ArrayList<>(over.getWindow().getPartitions().size());
		for ( SqmExpression<?> partition : over.getWindow().getPartitions() ) {
			partitions.add( (Expression) partition.accept( this ) );
		}
		final List<SortSpecification> orderList = new ArrayList<>( over.getWindow().getOrderList().size() );
		for ( SqmSortSpecification sortSpecification : over.getWindow().getOrderList() ) {
			orderList.add( visitSortSpecification( sortSpecification ) );
		}
		final Over<Object> overExpression = new Over<>(
				expression,
				partitions,
				orderList,
				over.getWindow().getMode(),
				over.getWindow().getStartKind(),
				over.getWindow().getStartExpression() == null ?
						null :
						(Expression) over.getWindow().getStartExpression().accept( this ),
				over.getWindow().getEndKind(),
				over.getWindow().getEndExpression() == null ?
						null :
						(Expression) over.getWindow().getEndExpression().accept( this ),
				over.getWindow().getExclusion()
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
		return new Overflow(
				(Expression) sqmOverflow.getSeparatorExpression().accept( this ),
				sqmOverflow.getFillerExpression() == null
						? null
						: (Expression) sqmOverflow.getFillerExpression().accept( this ),
				sqmOverflow.isWithCount()
		);
	}

	@Override
	public Object visitTrimSpecification(SqmTrimSpecification specification) {
		return new TrimSpecification( specification.getSpecification() );
	}

	@Override
	public Object visitCastTarget(SqmCastTarget<?> target) {
		BasicValuedMapping targetType = (BasicValuedMapping) target.getType();
		if ( targetType instanceof BasicType ) {
			targetType = InferredBasicValueResolver.resolveSqlTypeIndicators(
					this,
					(BasicType) targetType,
					target.getNodeJavaType()
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
		return new Format( sqmFormat.getLiteralValue() );
	}

	@Override
	public Object visitCoalesce(SqmCoalesce<?> sqmCoalesce) {
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final QueryEngine queryEngine = sessionFactory.getQueryEngine();
		return queryEngine.getSqmFunctionRegistry().findFunctionDescriptor( "coalesce" )
				.generateSqmExpression(
						sqmCoalesce.getArguments(),
						null,
						queryEngine
				)
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
		SqmExpression<?> leftOperand = expression.getLeftHandOperand();
		SqmExpression<?> rightOperand = expression.getRightHandOperand();

		// Need to infer the operand types here first to decide how to transform the expression
		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();
		inferrableTypeAccessStack.push( () -> determineValueMapping( rightOperand, fromClauseIndex ) );
		final MappingModelExpressible<?> leftOperandType = determineValueMapping( leftOperand );
		inferrableTypeAccessStack.pop();
		inferrableTypeAccessStack.push( () -> determineValueMapping( leftOperand, fromClauseIndex ) );
		final MappingModelExpressible<?> rightOperandType = determineValueMapping( rightOperand );
		inferrableTypeAccessStack.pop();

		final boolean durationToRight = isDuration( rightOperand.getNodeType() );
		final TypeConfiguration typeConfiguration = getCreationContext().getMappingMetamodel().getTypeConfiguration();
		final TemporalType temporalTypeToLeft = typeConfiguration.getSqlTemporalType( leftOperandType );
		final TemporalType temporalTypeToRight = typeConfiguration.getSqlTemporalType( rightOperandType );
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
			final Expression lhs = toSqlExpression( leftOperand.accept( this ) );
			inferrableTypeAccessStack.pop();
			inferrableTypeAccessStack.push( () -> determineValueMapping( leftOperand, fromClauseIndex ) );
			final Expression rhs = toSqlExpression( rightOperand.accept( this ) );
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
		final SqmExpressible<?> nodeType = expression.getNodeType();
		if ( nodeType != null ) {
			if ( nodeType instanceof BasicValuedMapping ) {
				return (BasicValuedMapping) nodeType;
			}
			else if ( nodeType.getSqmType() instanceof BasicValuedMapping ) {
				return (BasicValuedMapping) nodeType.getSqmType();
			}
			else {
				return getTypeConfiguration().getBasicTypeForJavaType(
						nodeType.getExpressibleJavaType().getJavaTypeClass()
				);
			}
		}
		return JavaObjectType.INSTANCE;
	}

	private Expression toSqlExpression(Object value) {
		return (Expression) value;
	}

	private Object transformDurationArithmetic(SqmBinaryArithmetic<?> expression) {
		final BinaryArithmeticOperator operator = expression.getOperator();
		final SqmExpression<?> lhs = SqmExpressionHelper.getActualExpression( expression.getLeftHandOperand() );
		final SqmExpression<?> rhs = SqmExpressionHelper.getActualExpression( expression.getRightHandOperand() );
		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();

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
				SqmExpressible<?> timestampType = adjustedTimestampType;
				inferrableTypeAccessStack.push( () -> determineValueMapping( rhs, fromClauseIndex ) );
				adjustedTimestamp = toSqlExpression( lhs.accept( this ) );
				inferrableTypeAccessStack.pop();
				JdbcMappingContainer type = adjustedTimestamp.getExpressionType();
				if ( type instanceof SqmExpressible) {
					adjustedTimestampType = (SqmExpressible<?>) type;
				}
				else if (type instanceof ValueMapping ) {
					adjustedTimestampType = (SqmExpressible<?>) ( (ValueMapping) type ).getMappedType();
				}
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
					if ( lhs != expression.getLeftHandOperand() ) {
						final SqmPath<?> temporalPath = (SqmPath<?>) expression.getLeftHandOperand();
						baseNavigablePath = temporalPath.getNavigablePath().getParent();
						offset = (temporalPath).get(
								AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME
						).accept( this );
					}
					else if ( rhs != expression.getRightHandOperand() ) {
						final SqmPath<?> temporalPath = (SqmPath<?>) expression.getRightHandOperand();
						baseNavigablePath = temporalPath.getNavigablePath().getParent();
						offset = ( temporalPath ).get(
								AbstractTimeZoneStorageCompositeUserType.ZONE_OFFSET_NAME
						).accept( this );
					}
					else {
						return result;
					}
					final EmbeddableValuedModelPart valueMapping = (EmbeddableValuedModelPart) determineValueMapping( expression );
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
				Expression duration = toSqlExpression( lhs.accept( this ) );
				inferrableTypeAccessStack.pop();
				Expression scale = adjustmentScale;
				boolean negate = negativeAdjustment;
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
		BinaryArithmeticOperator operator = expression.getOperator();

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

		final SqmExpression<?> lhs = SqmExpressionHelper.getActualExpression( expression.getLeftHandOperand() );
		final SqmExpression<?> rhs = SqmExpressionHelper.getActualExpression( expression.getRightHandOperand() );

		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();
		inferrableTypeAccessStack.push( () -> determineValueMapping( rhs, fromClauseIndex ) );
		final Expression left = getActualExpression( cleanly( () -> toSqlExpression( lhs.accept( this ) ) ) );
		inferrableTypeAccessStack.pop();
		inferrableTypeAccessStack.push( () -> determineValueMapping( lhs, fromClauseIndex ) );
		final Expression right = getActualExpression( cleanly( () -> toSqlExpression( rhs.accept( this ) ) ) );
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

			DurationUnit unit = new DurationUnit( baseUnit, diffResultType );
			BasicValuedMapping durationType = (BasicValuedMapping) expression.getNodeType();
			Expression scaledMagnitude = applyScale(
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

			DurationUnit unit = (DurationUnit) appliedByUnit.getUnit().accept( this );
			return applyScale( timestampdiff().expression( null, unit, right, left ) );
		}
		else {
			// a plain "bare" Duration
			DurationUnit unit = new DurationUnit( baseUnit, diffResultType );
			BasicValuedMapping durationType = (BasicValuedMapping) expression.getNodeType();
			Expression scaledMagnitude = applyScale(
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
		if ( expression.getExpressionType() instanceof EmbeddableValuedModelPart ) {
			final EmbeddableValuedModelPart embeddableValuedModelPart = (EmbeddableValuedModelPart) expression.getExpressionType();
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

	private <X> X cleanly(Supplier<X> supplier) {
		SqmByUnit byUnit = appliedByUnit;
		Expression timestamp = adjustedTimestamp;
		SqmExpressible<?> timestampType = adjustedTimestampType;
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
					if ( magnitudeType.getJdbcMapping().getJdbcType().isInterval() ) {
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
		inferrableTypeAccessStack.push( () -> null );
		Expression magnitude = toSqlExpression( toDuration.getMagnitude().accept( this ) );
		inferrableTypeAccessStack.pop();
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
					(ReturnableType<?>) adjustedTimestampType, //TODO should be adjustedTimestamp.getType()
					unit, scaledMagnitude, adjustedTimestamp
			);
		}
		else {
			BasicValuedMapping durationType = (BasicValuedMapping) toDuration.getNodeType();
			Duration duration;
			if ( scaledMagnitude.getExpressionType().getSingleJdbcMapping().getJdbcType().isInterval() ) {
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
		final CastType lhsCastType = lhs.getJdbcMapping().getJdbcType().getCastType();
		final CastType rhsCastType = rhs.getJdbcMapping().getJdbcType().getCastType();
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
	public Object visitModifiedSubQueryExpression(SqmModifiedSubQueryExpression<?> expr) {
		return new ModifiedSubQueryExpression(
				visitSubQueryExpression( expr.getSubQuery() ),
				convert( expr.getModifier() )
		);
	}

	private ModifiedSubQueryExpression.Modifier convert(SqmModifiedSubQueryExpression.Modifier modifier) {
		if ( modifier == SqmModifiedSubQueryExpression.Modifier.ALL ) {
			return ModifiedSubQueryExpression.Modifier.ALL;
		}
		if ( modifier == SqmModifiedSubQueryExpression.Modifier.ANY ) {
			return ModifiedSubQueryExpression.Modifier.ANY;
		}
		if ( modifier == SqmModifiedSubQueryExpression.Modifier.SOME ) {
			return ModifiedSubQueryExpression.Modifier.SOME;
		}
		throw new IllegalStateException( "Unrecognized SqmModifiedSubQueryExpression.Modifier : " + modifier );
	}

	@Override
	public SelectStatement visitSubQueryExpression(SqmSubQuery<?> sqmSubQuery) {
		// The only purpose for tracking the current join is to
		// Reset the current join for subqueries because in there, we won't add nested joins
		final SqmJoin<?, ?> oldJoin = currentlyProcessingJoin;
		final CteContainer oldCteContainer = cteContainer;
		currentlyProcessingJoin = null;
		final CteContainer cteContainer = this.visitCteContainer( sqmSubQuery );
		final QueryPart queryPart = visitQueryPart( sqmSubQuery.getQueryPart() );
		currentlyProcessingJoin = oldJoin;
		this.cteContainer = oldCteContainer;
		return new SelectStatement( cteContainer, queryPart, Collections.emptyList() );
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(SqmCaseSimple<?, ?> expression) {
		final List<CaseSimpleExpression.WhenFragment> whenFragments = new ArrayList<>( expression.getWhenFragments().size() );
		final Supplier<MappingModelExpressible<?>> inferenceSupplier = inferrableTypeAccessStack.getCurrent();
		final boolean oldInNestedContext = inNestedContext;

		inNestedContext = true;
		inferrableTypeAccessStack.push(
				() -> {
					for ( SqmCaseSimple.WhenFragment<?, ?> whenFragment : expression.getWhenFragments() ) {
						final MappingModelExpressible<?> resolved = determineCurrentExpressible( whenFragment.getCheckValue() );
						if ( resolved != null ) {
							return resolved;
						}
					}
					return null;
				}
		);
		final Expression fixture = (Expression) expression.getFixture().accept( this );
		final MappingModelExpressible<?> fixtureType = (MappingModelExpressible<?>) fixture.getExpressionType();
		inferrableTypeAccessStack.pop();
		MappingModelExpressible<?> resolved = determineCurrentExpressible( expression );
		Expression otherwise = null;
		for ( SqmCaseSimple.WhenFragment<?, ?> whenFragment : expression.getWhenFragments() ) {
			inferrableTypeAccessStack.push( () -> fixtureType );
			final Expression checkValue = (Expression) whenFragment.getCheckValue().accept( this );
			inferrableTypeAccessStack.pop();
			final MappingModelExpressible<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			final Expression resultExpression = (Expression) whenFragment.getResult().accept( this );
			inferrableTypeAccessStack.pop();
			resolved = (MappingModelExpressible<?>) highestPrecedence( resolved, resultExpression.getExpressionType() );

			whenFragments.add(
					new CaseSimpleExpression.WhenFragment(
							checkValue,
							resultExpression
					)
			);
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
		final Supplier<MappingModelExpressible<?>> inferenceSupplier = inferrableTypeAccessStack.getCurrent();
		final boolean oldInNestedContext = inNestedContext;

		inNestedContext = true;
		MappingModelExpressible<?> resolved = determineCurrentExpressible( expression );

		Expression otherwise = null;
		for ( SqmCaseSearched.WhenFragment<?> whenFragment : expression.getWhenFragments() ) {
			final Predicate whenPredicate = visitNestedTopLevelPredicate( whenFragment.getPredicate() );
			final MappingModelExpressible<?> alreadyKnown = resolved;
			inferrableTypeAccessStack.push(
					() -> alreadyKnown == null && inferenceSupplier != null ? inferenceSupplier.get() : alreadyKnown
			);
			final Expression resultExpression = (Expression) whenFragment.getResult().accept( this );
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
		return creationContext.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.resolveMappingExpressible( expression.getNodeType(), getFromClauseIndex()::findTableGroup );
	}

	private <X> X visitWithInferredType(SqmExpression<?> expression, SqmExpression<?> inferred) {
		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();
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
		final EntityPersister mappingDescriptor = creationContext.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( nodeType.getHibernateEntityName() );

		return new EntityTypeLiteral( mappingDescriptor );
	}

	@Override
	public Object visitEmbeddableTypeLiteralExpression(SqmLiteralEmbeddableType<?> expression) {
		// The node type of literal embeddable types will either be the discriminator
		// type for polymorphic embeddables or the Class standard basic type
		final BasicType<?> basicType = (BasicType<?>) expression.getNodeType();
		return new EmbeddableTypeLiteral( expression.getEmbeddableDomainType(), basicType );
	}

	@Override
	public Expression visitAnyDiscriminatorTypeValueExpression(SqmAnyDiscriminatorValue<?> expression) {
		final BasicType<?> domainType = expression.getDomainType();
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
		final MappingModelExpressible<?> inferred = resolveInferredType();
		final SqlExpressible inferredType;
		if ( inferred instanceof PluralAttributeMapping ) {
			final CollectionPart elementDescriptor = ((PluralAttributeMapping) inferred).getElementDescriptor();
			if ( elementDescriptor instanceof BasicValuedCollectionPart) {
				inferredType = (BasicValuedCollectionPart) elementDescriptor;
			}
			else {
				inferredType = null;
			}
		}
		else if ( inferred instanceof BasicValuedMapping ) {
			inferredType = (BasicValuedMapping) inferred;
		}
		else {
			inferredType = null;
		}
		if ( inferredType != null ) {
			return new QueryLiteral<>(
					inferredType.getJdbcMapping().convertToRelationalValue( sqmEnumLiteral.getEnumValue() ),
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
						typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.SMALLINT )
				)
		);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Object visitFieldLiteral(SqmFieldLiteral<?> sqmFieldLiteral) {
		final BasicValuedMapping valueMapping = (BasicValuedMapping) determineValueMapping( sqmFieldLiteral );
		final Object value = sqmFieldLiteral.getValue();
		final BasicValueConverter converter = valueMapping.getJdbcMapping().getValueConverter();
		return new QueryLiteral<>( converter != null ? sqlLiteralValue( converter, value ) : value, valueMapping );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates

	@Override
	public Predicate visitNestedTopLevelPredicate(SqmPredicate predicate) {
		final Map<TableGroup, Map<String, EntityNameUse>> originalConjunctTableGroupTreatUsages;
		if ( tableGroupEntityNameUses.isEmpty() ) {
			originalConjunctTableGroupTreatUsages = null;
		}
		else {
			originalConjunctTableGroupTreatUsages = new IdentityHashMap<>( tableGroupEntityNameUses );
		}
		tableGroupEntityNameUses.clear();
		inferrableTypeAccessStack.push( this::getBooleanType );
		final Predicate result = (Predicate) predicate.accept( this );
		inferrableTypeAccessStack.pop();
		final Predicate finalPredicate = combinePredicates(
				result,
				consumeConjunctTreatTypeRestrictions()
		);
		if ( originalConjunctTableGroupTreatUsages != null ) {
			for ( Map.Entry<TableGroup, Map<String, EntityNameUse>> entry : originalConjunctTableGroupTreatUsages.entrySet() ) {
				final Map<String, EntityNameUse> entityNameUses = tableGroupEntityNameUses.putIfAbsent(
						entry.getKey(),
						entry.getValue()
				);
				if ( entityNameUses != null && entityNameUses != entry.getValue() ) {
					for ( Map.Entry<String, EntityNameUse> useEntry : entry.getValue().entrySet() ) {
						final EntityNameUse currentUseKind = entityNameUses.get( useEntry.getKey() );
						if ( currentUseKind == null ) {
							entityNameUses.put( useEntry.getKey(), useEntry.getValue() );
						}
						else {
							entityNameUses.put( useEntry.getKey(), useEntry.getValue().stronger( currentUseKind ) );
						}
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
		if ( predicate.getOperator() == jakarta.persistence.criteria.Predicate.BooleanOperator.AND ) {
			final List<Predicate> predicates = new ArrayList<>( predicate.getPredicates().size() );
			for ( SqmPredicate subPredicate : predicate.getPredicates() ) {
				predicates.add( (Predicate) subPredicate.accept( this ) );
			}
			return new Junction( Junction.Nature.CONJUNCTION, predicates, getBooleanType() );
		}
		final Junction disjunction = new Junction(
				Junction.Nature.DISJUNCTION,
				new ArrayList<>( predicate.getPredicates().size() ),
				getBooleanType()
		);
		final Map<TableGroup, Map<String, EntityNameUse>> previousTableGroupEntityNameUses = new IdentityHashMap<>( tableGroupEntityNameUses );
		Map<TableGroup, Map<String, EntityNameUse>>[] disjunctEntityNameUsesArray = null;
		Map<TableGroup, Map<String, EntityNameUse>> entityNameUsesToPropagate = null;
		List<TableGroup> treatedTableGroups = null;
		List<TableGroup> filteredTableGroups = null;
		List<SqmPredicate> predicates = predicate.getPredicates();
		for ( int i = 0; i < predicates.size(); i++ ) {
			tableGroupEntityNameUses.clear();
			disjunction.add( (Predicate) predicates.get( i ).accept( this ) );
			if ( !tableGroupEntityNameUses.isEmpty() ) {
				if ( disjunctEntityNameUsesArray == null ) {
					disjunctEntityNameUsesArray = new Map[predicate.getPredicates().size()];
					entityNameUsesToPropagate = new IdentityHashMap<>( previousTableGroupEntityNameUses );
				}
				if ( i == 0 ) {
					// Collect the table groups for which filters are registered
					for ( Map.Entry<TableGroup, Map<String, EntityNameUse>> entry : tableGroupEntityNameUses.entrySet() ) {
						if ( entry.getValue().containsValue( EntityNameUse.TREAT ) || entry.getValue().containsValue( EntityNameUse.OPTIONAL_TREAT ) ) {
							if ( treatedTableGroups == null ) {
								treatedTableGroups = new ArrayList<>( 1 );
							}
							treatedTableGroups.add( entry.getKey() );
						}
						if ( entry.getValue().containsValue( EntityNameUse.FILTER ) ) {
							if ( filteredTableGroups == null ) {
								filteredTableGroups = new ArrayList<>( 1 );
							}
							filteredTableGroups.add( entry.getKey() );
						}
					}
				}
				// Create a copy of the filtered table groups from which we remove
				final List<TableGroup> missingTableGroupFilters;
				if ( filteredTableGroups == null || i == 0 ) {
					missingTableGroupFilters = Collections.emptyList();
				}
				else {
					missingTableGroupFilters = new ArrayList<>( filteredTableGroups );
				}
				final List<TableGroup> missingTableGroupTreats;
				if ( treatedTableGroups == null || i == 0 ) {
					missingTableGroupTreats = Collections.emptyList();
				}
				else {
					missingTableGroupTreats = new ArrayList<>( treatedTableGroups );
				}
				// Compute the entity name uses to propagate to the parent context
				// If every disjunct contains a FILTER, we can merge the filters
				// If every disjunct contains a TREAT, we can merge the treats
				// Otherwise, we downgrade the entity name uses to expression uses
				final Iterator<Map.Entry<TableGroup, Map<String, EntityNameUse>>> iterator = tableGroupEntityNameUses.entrySet().iterator();
				while ( iterator.hasNext() ) {
					final Map.Entry<TableGroup, Map<String, EntityNameUse>> entry = iterator.next();
					final TableGroup tableGroup = entry.getKey();
					final Map<String, EntityNameUse> entityNameUses = entityNameUsesToPropagate.computeIfAbsent(
							tableGroup,
							k -> new HashMap<>()
					);
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
					for ( Map.Entry<String, EntityNameUse> useEntry : entry.getValue().entrySet() ) {
						final EntityNameUse.UseKind useKind = useEntry.getValue().getKind();
						final EntityNameUse currentUseKind = entityNameUses.get( useEntry.getKey() );
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
						if ( currentUseKind == null ) {
							entityNameUses.put( useEntry.getKey(), unionEntityNameUse );
						}
						else {
							entityNameUses.put( useEntry.getKey(), unionEntityNameUse.stronger( currentUseKind ) );
						}
					}
				}
				// Downgrade entity name uses for table groups that haven't been filtered in this disjunct
				for ( TableGroup missingTableGroupTreat : missingTableGroupTreats ) {
					treatedTableGroups.remove( missingTableGroupTreat );
					final Map<String, EntityNameUse> entityNameUses = entityNameUsesToPropagate.get( missingTableGroupTreat );
					for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
						if ( entry.getValue().getKind() == EntityNameUse.UseKind.TREAT ) {
							entry.setValue( EntityNameUse.EXPRESSION );
						}
					}
				}
				for ( TableGroup missingTableGroupFilter : missingTableGroupFilters ) {
					filteredTableGroups.remove( missingTableGroupFilter );
					final Map<String, EntityNameUse> entityNameUses = entityNameUsesToPropagate.get( missingTableGroupFilter );
					for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
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
					for ( Map<String, EntityNameUse> entityNameUses : entityNameUsesToPropagate.values() ) {
						for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
							if ( entry.getValue().getKind() == EntityNameUse.UseKind.TREAT ) {
								entry.setValue( EntityNameUse.EXPRESSION );
							}
						}
					}
				}
				if ( filteredTableGroups != null ) {
					filteredTableGroups = null;
					for ( Map<String, EntityNameUse> entityNameUses : entityNameUsesToPropagate.values() ) {
						for ( Map.Entry<String, EntityNameUse> entry : entityNameUses.entrySet() ) {
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
			final Iterator<Map.Entry<TableGroup, Map<String, EntityNameUse>>> iterator = tableGroupEntityNameUses.entrySet().iterator();
			while ( iterator.hasNext() ) {
				final Map.Entry<TableGroup, Map<String, EntityNameUse>> entry = iterator.next();
				final Map<String, EntityNameUse> intersected = new HashMap<>( entry.getValue() );
				entry.setValue( intersected );
				boolean remove = false;
				for ( Map<TableGroup, Map<String, EntityNameUse>> conjunctTreatUsages : disjunctEntityNameUsesArray ) {
					final Map<String, EntityNameUse> entityNames;
					if ( conjunctTreatUsages == null || ( entityNames = conjunctTreatUsages.get( entry.getKey() ) ) == null ) {
						remove = true;
						break;
					}
					// Intersect the two sets and transfer the common elements to the intersection
					final Iterator<Map.Entry<String, EntityNameUse>> intersectedIter = intersected.entrySet()
							.iterator();
					while ( intersectedIter.hasNext() ) {
						final Map.Entry<String, EntityNameUse> intersectedEntry = intersectedIter.next();
						final EntityNameUse intersectedUseKind = intersectedEntry.getValue();
						final EntityNameUse useKind = entityNames.get( intersectedEntry.getKey() );
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
			final Map<TableGroup, Map<String, EntityNameUse>> conjunctTreatUsages = disjunctEntityNameUsesArray[i];
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
		for ( Map.Entry<TableGroup, Map<String, EntityNameUse>> entry : entityNameUsesToPropagate.entrySet() ) {
			final Map<String, EntityNameUse> entityNameUses = tableGroupEntityNameUses.putIfAbsent(
					entry.getKey(),
					entry.getValue()
			);
			if ( entityNameUses != null && entityNameUses != entry.getValue() ) {
				for ( Map.Entry<String, EntityNameUse> useEntry : entry.getValue().entrySet() ) {
					final EntityNameUse currentEntityNameUse = entityNameUses.get( useEntry.getKey() );
					if ( currentEntityNameUse == null ) {
						entityNameUses.put( useEntry.getKey(), useEntry.getValue() );
					}
					else {
						entityNameUses.put( useEntry.getKey(), useEntry.getValue().stronger( currentEntityNameUse ) );
					}
				}
			}
		}
		return disjunction;
	}

	@Override
	public Predicate visitMemberOfPredicate(SqmMemberOfPredicate predicate) {
		final SqmPath<?> pluralPath = predicate.getPluralPath();
		prepareReusablePath( pluralPath, () -> null );

		final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) determineValueMapping(
				pluralPath );

		inferrableTypeAccessStack.push( () -> pluralAttributeMapping );

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
						currentClauseStack::getCurrent,
						false
				)
		);
		try {
			final TableGroup tableGroup = pluralAttributeMapping.createRootTableGroup(
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
		}
		finally {
			popProcessingStateStack();
		}

		return new InSubQueryPredicate(
				lhs,
				new SelectStatement( subQuerySpec ),
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
		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();
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

		final ComparisonOperator sqmOperator = predicate.isNegated()
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
		final TableGroup tableGroup = getFromClauseIndex().getTableGroup( typeExpression.getNavigablePath().getParent() );
		final MappingType partMappingType = tableGroup.getModelPart().getPartMappingType();
		if ( !( partMappingType instanceof EntityMappingType ) ) {
			return;
		}
		final EntityMappingType entityMappingType = (EntityMappingType) partMappingType;
		if ( entityMappingType.getDiscriminatorMapping().hasPhysicalColumn() ) {
			// If the entity has a physical discriminator column we don't need to register any FILTER usages.
			// Register only an EXPRESSION usage to prevent pruning of the root type's table reference which
			// contains the physical discriminator column
			registerEntityNameUsage(
					tableGroup,
					EntityNameUse.EXPRESSION,
					entityMappingType.getRootEntityDescriptor().getEntityName()
			);
		}
		else {
			if ( literalExpressions == null ) {
				// We have to assume all types are possible and can't do optimizations
				registerEntityNameUsage( tableGroup, EntityNameUse.FILTER, entityMappingType.getEntityName() );
				for ( EntityMappingType subMappingType : entityMappingType.getSubMappingTypes() ) {
					registerEntityNameUsage( tableGroup, EntityNameUse.FILTER, subMappingType.getEntityName() );
				}
			}
			else {
				if ( inclusive ) {
					for ( EntityTypeLiteral literalExpr : literalExpressions ) {
						registerEntityNameUsage(
								tableGroup,
								EntityNameUse.FILTER,
								literalExpr.getEntityTypeDescriptor().getEntityName()
						);
					}
				}
				else {
					final Set<String> excludedEntityNames = new HashSet<>( entityMappingType.getSubMappingTypes().size() );
					for ( EntityTypeLiteral literalExpr : literalExpressions ) {
						excludedEntityNames.add( literalExpr.getEntityTypeDescriptor().getEntityName() );
					}
					if ( !excludedEntityNames.contains( entityMappingType.getEntityName() ) ) {
						registerEntityNameUsage( tableGroup, EntityNameUse.FILTER, entityMappingType.getEntityName() );
					}
					for ( EntityMappingType subMappingType : entityMappingType.getSubMappingTypes() ) {
						if ( !excludedEntityNames.contains( subMappingType.getEntityName() ) ) {
							registerEntityNameUsage( tableGroup, EntityNameUse.FILTER, subMappingType.getEntityName() );
						}
					}
				}
			}
		}
	}

	@Override
	public Object visitIsEmptyPredicate(SqmEmptinessPredicate predicate) {
		prepareReusablePath( predicate.getPluralPath(), () -> null );

		final QuerySpec subQuerySpec = new QuerySpec( false, 1 );

		final FromClauseAccess parentFromClauseAccess = getFromClauseAccess();
		final SqlAstQueryPartProcessingStateImpl subQueryState = new SqlAstQueryPartProcessingStateImpl(
				subQuerySpec,
				getCurrentProcessingState(),
				this,
				currentClauseStack::getCurrent,
				true
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
			registerPluralTableGroupParts( tableGroup );

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
							null,
							SqlAstJoinType.INNER,
							false,
							false,
							this
					)
			);

			final ForeignKeyDescriptor collectionKeyDescriptor = pluralAttributeMapping.getKeyDescriptor();
			final int jdbcTypeCount = collectionKeyDescriptor.getJdbcTypeCount();
			assert jdbcTypeCount > 0;

			final JdbcLiteral<Integer> jdbcLiteral = new JdbcLiteral<>( 1, basicType( Integer.class ) );
			subQuerySpec.getSelectClause().addSqlSelection( new SqlSelectionImpl( jdbcLiteral ) );

			return new ExistsPredicate( subQuerySpec, !predicate.isNegated(), getBooleanType() );
		}
		finally {
			popProcessingStateStack();
		}
	}

	@Override
	public BetweenPredicate visitBetweenPredicate(SqmBetweenPredicate predicate) {
		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();
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
		return new LikePredicate(
				visitWithInferredType( predicate.getMatchExpression(), predicate.getPattern() ),
				visitWithInferredType( predicate.getPattern(), predicate.getMatchExpression() ),
				predicate.getEscapeCharacter() == null ?
						null :
						(Expression) visitWithInferredType( predicate.getEscapeCharacter(), () -> basicType( Character.class ) ),
				predicate.isNegated(),
				predicate.isCaseSensitive(),
				getBooleanType()
		);
	}

	@Override
	public NullnessPredicate visitIsNullPredicate(SqmNullnessPredicate predicate) {
		final SqmExpression<?> sqmExpression = predicate.getExpression();
		final Expression expression;
		if ( sqmExpression instanceof SqmEntityValuedSimplePath<?> ) {
			final SqmEntityValuedSimplePath<?> entityValuedPath = (SqmEntityValuedSimplePath<?>) sqmExpression;
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
			expression = (Expression) visitWithInferredType(
					predicate.getExpression(),
					() -> basicType( Object.class )
			);
		}
		return new NullnessPredicate( expression, predicate.isNegated(), getBooleanType() );
	}

	@Override
	public Object visitIsTruePredicate(SqmTruthnessPredicate predicate) {
		return new ThruthnessPredicate(
				(Expression) visitWithInferredType( predicate.getExpression(), () -> basicType( Boolean.class )),
				predicate.getBooleanValue(),
				predicate.isNegated(),
				getBooleanType()
		);
	}

	@Override
	public Predicate visitInListPredicate(SqmInListPredicate<?> predicate) {
		// special case: if there is just a single "value" element and it is a parameter
		//		and the binding for that parameter is multi-valued we need special
		//		handling for "expansion"
		if ( predicate.getListExpressions().size() == 1 ) {
			final SqmExpression<?> sqmExpression = predicate.getListExpressions().get( 0 );
			if ( sqmExpression instanceof SqmParameter ) {
				final SqmParameter<?> sqmParameter = (SqmParameter<?>) sqmExpression;

				if ( sqmParameter.allowMultiValuedBinding() ) {
					final InListPredicate specialCase = processInListWithSingleParameter( predicate, sqmParameter );
					if ( specialCase != null ) {
						handleTypeComparison( specialCase );
						return specialCase;
					}
				}
			}
		}

		// otherwise - no special case...
		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();
		inferrableTypeAccessStack.push(
				() -> {
					for ( SqmExpression<?> listExpression : predicate.getListExpressions() ) {
						final MappingModelExpressible<?> mapping = determineValueMapping( listExpression, fromClauseIndex );
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

		final InListPredicate inPredicate = new InListPredicate(
				testExpression,
				predicate.isNegated(),
				getBooleanType()
		);

		inferrableTypeAccessStack.push( () -> determineValueMapping( predicate.getTestExpression(), fromClauseIndex ) );

		try {
			for ( SqmExpression<?> expression : predicate.getListExpressions() ) {
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
		final Expression testExpression = inPredicate.getTestExpression();
		if ( testExpression instanceof DiscriminatorPathInterpretation<?> ) {
			final DiscriminatorPathInterpretation<?> typeExpression = (DiscriminatorPathInterpretation<?>) testExpression;
			boolean containsNonLiteral = false;
			for ( Expression listExpression : inPredicate.getListExpressions() ) {
				if ( !( listExpression instanceof EntityTypeLiteral ) ) {
					containsNonLiteral = true;
					break;
				}
			}
			//noinspection unchecked
			handleTypeComparison(
					typeExpression,
					containsNonLiteral ? null : (List<EntityTypeLiteral>) (List<?>) inPredicate.getListExpressions(),
					!inPredicate.isNegated()
			);
		}
	}

	private InListPredicate processInListWithSingleParameter(
			SqmInListPredicate<?> sqmPredicate,
			SqmParameter<?> sqmParameter) {
		assert sqmParameter.allowMultiValuedBinding();

		if ( sqmParameter instanceof JpaCriteriaParameter ) {
			return processInSingleCriteriaParameter( sqmPredicate, (JpaCriteriaParameter<?>) sqmParameter );
		}

		return processInSingleHqlParameter( sqmPredicate, sqmParameter );
	}

	private InListPredicate processInSingleHqlParameter(SqmInListPredicate<?> sqmPredicate, SqmParameter<?> sqmParameter) {
		final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding<?> domainParamBinding = domainParameterBindings.getBinding( domainParam );
		if ( !domainParamBinding.isMultiValued() ) {
			// triggers normal processing
			return null;
		}

		return processInSingleParameter( sqmPredicate, sqmParameter, domainParam, domainParamBinding );
	}

	private InListPredicate processInSingleCriteriaParameter(
			SqmInListPredicate<?> sqmPredicate,
			JpaCriteriaParameter<?> jpaCriteriaParameter) {
		assert jpaCriteriaParameter.allowsMultiValuedBinding();

		final QueryParameterBinding<?> domainParamBinding = domainParameterBindings.getBinding( jpaCriteriaParameter );
		if ( !domainParamBinding.isMultiValued() ) {
			return null;
		}
		final SqmJpaCriteriaParameterWrapper<?> sqmWrapper = jpaCriteriaParamResolutions.get( jpaCriteriaParameter );

		return processInSingleParameter( sqmPredicate, sqmWrapper, jpaCriteriaParameter, domainParamBinding );
	}

	@SuppressWarnings( "rawtypes" )
	private InListPredicate processInSingleParameter(
			SqmInListPredicate<?> sqmPredicate,
			SqmParameter<?> sqmParameter,
			QueryParameterImplementor<?> domainParam,
			QueryParameterBinding<?> domainParamBinding) {
		final Iterator<?> iterator = domainParamBinding.getBindValues().iterator();

		final InListPredicate inListPredicate = new InListPredicate(
				(Expression) sqmPredicate.getTestExpression().accept( this ),
				sqmPredicate.isNegated(),
				getBooleanType()
		);

		final FromClauseIndex fromClauseIndex = fromClauseIndexStack.getCurrent();

		if ( !iterator.hasNext() ) {
			domainParamBinding.setType( (MappingModelExpressible) determineValueMapping( sqmPredicate.getTestExpression(), fromClauseIndex ) );
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
				final SqmParameter<?> sqmParamToConsume = sqmParameter.copy();
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
		inferrableTypeAccessStack.push( this::getBooleanType );
		final Expression booleanExpression = (Expression) predicate.getBooleanExpression().accept( this );
		inferrableTypeAccessStack.pop();
		if ( booleanExpression instanceof SelfRenderingExpression ) {
			final Predicate sqlPredicate = new SelfRenderingPredicate( (SelfRenderingExpression) booleanExpression );
			if ( predicate.isNegated() ) {
				return new NegatedPredicate( sqlPredicate );
			}
			return sqlPredicate;
		}
		else {
			final JdbcMapping jdbcMapping = booleanExpression.getExpressionType().getJdbcMapping( 0 );
			if ( jdbcMapping.getValueConverter() != null ) {
				// handle converted booleans (yes-no, etc)
				return new ComparisonPredicate(
						booleanExpression,
						ComparisonOperator.EQUAL,
						new JdbcLiteral<>( jdbcMapping.convertToRelationalValue( !predicate.isNegated() ), jdbcMapping )
				);
			}

			return new BooleanExpressionPredicate(
					booleanExpression,
					predicate.isNegated(),
					getBooleanType()
			);
		}
	}

	@Override
	public Object visitExistsPredicate(SqmExistsPredicate predicate) {
		inferrableTypeAccessStack.push( () -> null );
		final SelectStatement selectStatement = (SelectStatement) predicate.getExpression().accept( this );
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
	public Fetch visitIdentifierFetch(EntityResultGraphNode fetchParent) {
		final EntityIdentifierMapping identifierMapping = fetchParent.getReferencedMappingContainer()
				.getIdentifierMapping();
		return createFetch( fetchParent, (Fetchable) identifierMapping, false );
	}

	private Fetch createFetch(FetchParent fetchParent, Fetchable fetchable, Boolean isKeyFetchable) {
		if ( !fetchable.isSelectable() ) {
			return null;
		}
		final NavigablePath resolvedNavigablePath = fetchParent.resolveNavigablePath( fetchable );
		final Map.Entry<Integer, List<SqlSelection>> sqlSelectionsToTrack = trackedFetchSelectionsForGroup.get( resolvedNavigablePath );
		final int sqlSelectionStartIndexForFetch;
		if ( sqlSelectionsToTrack != null ) {
			final List<SqlSelection> selections = currentSqlSelectionCollector().getSelections( sqlSelectionsToTrack.getKey() );
			sqlSelectionStartIndexForFetch = selections.size();
		}
		else {
			sqlSelectionStartIndexForFetch = -1;
		}

		final String alias;
		FetchTiming fetchTiming = fetchable.getMappedFetchOptions().getTiming();
		boolean joined = false;

		final NavigablePath fetchablePath;
		final Integer maxDepth = getCreationContext().getMaximumFetchDepth();
		final FromClauseIndex fromClauseIndex = getFromClauseIndex();
		final SqmAttributeJoin<?, ?> fetchedJoin = fromClauseIndex.findFetchedJoinByPath( resolvedNavigablePath );

		boolean explicitFetch = false;
		EntityGraphTraversalState.TraversalResult traversalResult = null;

		TableGroup joinedTableGroup = null;

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
					traversalResult = entityGraphTraversalState.traverse(
							fetchParent,
							fetchable,
							isKeyFetchable
					);

					EntityGraphTraversalState.FetchStrategy fetchStrategy = traversalResult.getFetchStrategy();
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
							final FetchProfile enabledFetchProfile = getCreationContext()
									.getSessionFactory()
									.getFetchProfile( enabledFetchProfileName );
							final org.hibernate.engine.profile.Fetch profileFetch =
									enabledFetchProfile.getFetchByRole( fetchableRole );

							if ( profileFetch != null ) {
								fetchTiming = profileFetch.getTiming();
								joined = joined || profileFetch.getMethod() == FetchStyle.JOIN;
								if ( shouldExplicitFetch( maxDepth, fetchable ) ) {
									explicitFetch = true;
								}

								if ( currentBagRole != null && fetchable instanceof PluralAttributeMapping ) {
									final CollectionClassification collectionClassification =
											( (PluralAttributeMapping) fetchable ).getMappedType()
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
			if ( maxDepth != null ) {
				if ( fetchDepth >= maxDepth ) {
					joined = false;
				}
			}

			if ( joined && fetchable instanceof TableGroupJoinProducer ) {
				joinedTableGroup = fromClauseIndex.resolveTableGroup(
						fetchablePath,
						np -> {
							// generate the join
							final TableGroup tableGroup;
							final TableGroup lhs = fromClauseIndex.getTableGroup( fetchParent.getNavigablePath() );
							final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) fetchable;
							final TableGroup compatibleTableGroup = lhs.findCompatibleJoinedGroup(
									joinProducer,
									joinProducer.getDefaultSqlAstJoinType( lhs )
							);
							final SqmQueryPart<?> queryPart = getCurrentSqmQueryPart();
							if ( compatibleTableGroup == null
									// If the compatible table group is used in the where clause it cannot be reused for fetching
									|| ( queryPart != null && queryPart.getFirstQuerySpec().whereClauseContains( compatibleTableGroup.getNavigablePath(), this ) ) ) {
								final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
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
				final Fetch biDirectionalFetch = fetchable.resolveCircularFetch(
						fetchablePath,
						fetchParent,
						fetchTiming,
						this
				);

				if ( biDirectionalFetch != null ) {
					return biDirectionalFetch;
				}
			}
			final Fetch fetch = buildFetch(
					fetchablePath,
					fetchParent,
					fetchable,
					fetchTiming,
					joined,
					alias
			);

			if ( sqlSelectionsToTrack != null ) {
				final List<SqlSelection> selections = currentSqlSelectionCollector().getSelections( sqlSelectionsToTrack.getKey() );
				sqlSelectionsToTrack.getValue().addAll( selections.subList( sqlSelectionStartIndexForFetch, selections.size() ) );
			}

			if ( fetch != null && fetch.getTiming() == FetchTiming.IMMEDIATE ) {
				if ( fetchable instanceof TableGroupJoinProducer ) {
					if ( joinedTableGroup != null ) {
						final TableGroup actualTableGroup = joinedTableGroup instanceof PluralTableGroup ?
								( (PluralTableGroup) joinedTableGroup ).getElementTableGroup() :
								joinedTableGroup;
						final MappingType mappingType = actualTableGroup == null
								? null
								: actualTableGroup.getModelPart().getPartMappingType();
						if ( mappingType instanceof EntityMappingType ) {
							final EntityMappingType entityMappingType = (EntityMappingType) mappingType;
							registerEntityNameUsage(
									actualTableGroup,
									EntityNameUse.PROJECTION,
									entityMappingType.getEntityName(),
									true
							);
							if ( entityMappingType.getSuperMappingType() != null ) {
								// A joined table group was created by an enabled entity graph or fetch profile,
								// and it's of an inheritance subtype, so we should apply the discriminator
								getCurrentClauseStack().push( Clause.FROM );
								registerEntityNameUsage( actualTableGroup, EntityNameUse.TREAT, entityMappingType.getEntityName() );
								getCurrentClauseStack().pop();
							}
						}
					}
					if ( fetchable instanceof PluralAttributeMapping ) {
						final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;
						final CollectionClassification collectionClassification = pluralAttributeMapping.getMappedType()
								.getCollectionSemantics()
								.getCollectionClassification();
						if ( collectionClassification == CollectionClassification.BAG ) {
							if ( currentBagRole != null ) {
								throw new MultipleBagFetchException(
										Arrays.asList(
												currentBagRole,
												fetchable.getNavigableRole().getNavigableName()
										)
								);
							}
							currentBagRole = fetchable.getNavigableRole().getNavigableName();
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
		final SqlAstQueryPartProcessingStateImpl processingState = (SqlAstQueryPartProcessingStateImpl) getCurrentProcessingState();
		final FetchParent nestingFetchParent = processingState.getNestingFetchParent();
		processingState.setNestingFetchParent( fetchParent );
		final R result = action.apply( fetchParent );
		processingState.setNestingFetchParent( nestingFetchParent );
		return result;
	}

	@Override
	public ImmutableFetchList visitFetches(FetchParent fetchParent) {
		final FetchableContainer referencedMappingContainer = fetchParent.getReferencedMappingContainer();
		final int keySize = referencedMappingContainer.getNumberOfKeyFetchables();
		final int size = referencedMappingContainer.getNumberOfFetchables();
		final ImmutableFetchList.Builder fetches = new ImmutableFetchList.Builder( referencedMappingContainer );
		for ( int i = 0; i < keySize; i++ ) {
			final Fetch fetch = createFetch( fetchParent, referencedMappingContainer.getKeyFetchable( i ), true );
			if ( fetch != null ) {
				fetches.add( fetch );
			}
		}
		for ( int i = 0; i < size; i++ ) {
			final Fetch fetch = createFetch( fetchParent, referencedMappingContainer.getFetchable( i ), false );
			if ( fetch != null ) {
				fetches.add( fetch );
			}
		}
		return fetches.build();
	}

	private boolean shouldExplicitFetch(Integer maxDepth, Fetchable fetchable) {
		/*
			Forcing the value of explicitFetch to true will disable the fetch circularity check and
			for already visited association or collection this will cause a StackOverflow if maxFetchDepth is null, see HHH-15391.
		 */
		if ( maxDepth == null ) {
			if ( fetchable instanceof ToOneAttributeMapping ) {
				return !this.isAssociationKeyVisited(
						( (ToOneAttributeMapping) fetchable ).getForeignKeyDescriptor().getAssociationKey()
				);
			}
			else if ( fetchable instanceof PluralAttributeMapping ) {
				return !this.isAssociationKeyVisited(
						( (PluralAttributeMapping) fetchable ).getKeyDescriptor().getAssociationKey()
				);
			}
		}

		return true;
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

	private void applyOrdering(TableGroup tableGroup, PluralAttributeMapping pluralAttributeMapping) {
		if ( pluralAttributeMapping.getOrderByFragment() != null ) {
			applyOrdering( tableGroup, pluralAttributeMapping.getOrderByFragment() );
		}

		if ( pluralAttributeMapping.getManyToManyOrderByFragment() != null ) {
			applyOrdering( tableGroup, pluralAttributeMapping.getManyToManyOrderByFragment() );
		}
	}

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
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final MetadataKey<?, ?> that = (MetadataKey<?, ?>) o;
			return source.equals( that.source ) && producer.equals( that.producer );
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

		@SuppressWarnings("unchecked")
		public SqmAliasedNodePositionTracker(SqlExpressionResolver delegate, List<? extends SqmAliasedNode<?>> selections) {
			this.delegate = delegate;
			this.sqlSelectionsForSqmSelection = new List[countIndividualSelections( selections )];
		}

		private static int countIndividualSelections(List<? extends SqmAliasedNode<?>> selections) {
			int offset = 0;
			for ( int i = 0; i < selections.size(); i++ ) {
				final SqmSelectableNode<?> selectableNode = selections.get( i ).getSelectableNode();
				if ( selectableNode instanceof SqmDynamicInstantiation<?> ) {
					offset += countIndividualSelections( ( (SqmDynamicInstantiation<?>) selectableNode ).getArguments() );
				}
				else if ( selectableNode instanceof SqmJpaCompoundSelection<?> ) {
					for ( SqmSelectableNode<?> node : ( (SqmJpaCompoundSelection<?>) selectableNode ).getSelectionItems() ) {
						if ( node instanceof SqmDynamicInstantiation<?> ) {
							offset += countIndividualSelections( ( (SqmDynamicInstantiation<?>) node ).getArguments() );
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
			SqlSelection selection = delegate.resolveSqlSelection( expression, javaType, fetchParent, typeConfiguration );
			List<SqlSelection> sqlSelectionList = sqlSelectionsForSqmSelection[index];
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
		final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter( sqmParameter );
		final QueryParameterBinding<?> domainParamBinding = domainParameterBindings.getBinding( domainParam );

		final Collection<?> bindValues = domainParamBinding.getBindValues();
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
			final Expression expression = consumeSingleSqmParameter( sqmParamToConsume );
			result.add( expression );
		}
		return result;
	}

	private static JdbcMappingContainer highestPrecedence(JdbcMappingContainer type1, JdbcMappingContainer type2) {
		if ( type1 == null ) {
			return type2;
		}

		if ( type2 == null ) {
			return type1;
		}

		if ( type1 instanceof ModelPart ) {
			return type1;
		}

		if ( type2 instanceof ModelPart ) {
			return type2;
		}

		// todo (6.0) : we probably want a precedence based on generic resolutions such as those based on Serializable

		// todo (6.0) : anything else to consider?

		return type1;
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
			final CteStatement cteStatement = cteStatements.get( cteLabel );
			if ( cteStatement == null && parent != null ) {
				return parent.getCteStatement( cteLabel );
			}
			return cteStatement;
		}

		@Override
		public void addCteStatement(CteStatement cteStatement) {
			if ( cteStatements.putIfAbsent( cteStatement.getCteTable().getTableExpression(), cteStatement ) != null ) {
				throw new IllegalArgumentException( "A CTE with the label " + cteStatement.getCteTable().getTableExpression() + " already exists" );
			}
		}

		@Override
		public Map<String, CteObject> getCteObjects() {
			return cteObjects;
		}

		@Override
		public CteObject getCteObject(String cteObjectName) {
			final CteObject cteObject = cteObjects.get( cteObjectName );
			if ( cteObject == null && parent != null ) {
				return parent.getCteObject( cteObjectName );
			}
			return cteObject;
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
