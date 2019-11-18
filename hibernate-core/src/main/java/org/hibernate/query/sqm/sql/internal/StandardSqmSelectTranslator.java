/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.DynamicInstantiationNature;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.SqmQuerySpecTranslation;
import org.hibernate.query.sqm.sql.SqmSelectTranslation;
import org.hibernate.query.sqm.sql.SqmSelectTranslator;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.cte.CteStatement;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.internal.domain.instantiation.DynamicInstantiation;
import org.hibernate.sql.results.spi.CircularFetchDetector;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityResultNode;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Interprets an SqmSelectStatement as a SQL-AST SelectQuery.
 *
 * @author Steve Ebersole
 * @author John O'Hara
 */
@SuppressWarnings("unchecked")
public class StandardSqmSelectTranslator
		extends BaseSqmToSqlAstConverter
		implements DomainResultCreationState, SqmSelectTranslator {
	private final LoadQueryInfluencers fetchInfluencers;
	private final CircularFetchDetector circularFetchDetector = new CircularFetchDetector();

	// prepare for 10 root selections to avoid list growth in most cases
	private final List<DomainResult> domainResults = CollectionHelper.arrayList( 10 );

	private GraphImplementor<?> currentJpaGraphNode;

	public StandardSqmSelectTranslator(
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers fetchInfluencers,
			SqlAstCreationContext creationContext) {
		super( creationContext, queryOptions, domainParameterXref, domainParameterBindings );
		this.fetchInfluencers = fetchInfluencers;

		if ( fetchInfluencers != null ) {
			if ( fetchInfluencers.getEffectiveEntityGraph().getSemantic() != null ) {
				currentJpaGraphNode = fetchInfluencers.getEffectiveEntityGraph().getGraph();
			}
		}
	}

	@Override
	public SqmSelectTranslation translate(SqmSelectStatement sqmStatement) {
		return new SqmSelectTranslation(
				visitSelectStatement( sqmStatement ),
				getJdbcParamsBySqmParam()
		);
	}

	@Override
	public SqmQuerySpecTranslation translate(SqmQuerySpec querySpec) {
		return new SqmQuerySpecTranslation(
				visitQuerySpec( querySpec ),
				getJdbcParamsBySqmParam()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultCreationState

	@Override
	public SqlAstCreationState getSqlAstCreationState() {
		return this;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// walker

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement statement) {
		final QuerySpec querySpec = visitQuerySpec( statement.getQuerySpec() );

		return new SelectStatement( querySpec, domainResults );
	}


	@Override
	public Void visitSelection(SqmSelection sqmSelection) {
		final DomainResultProducer resultProducer = resolveDomainResultProducer( sqmSelection );

		if ( getProcessingStateStack().depth() > 1 ) {
			resultProducer.applySqlSelections( this );
		}
		else {

			final DomainResult domainResult = resultProducer.createDomainResult(
					sqmSelection.getAlias(),
					this
			);

			domainResults.add( domainResult );
		}

		return null;
	}

	private DomainResultProducer resolveDomainResultProducer(SqmSelection sqmSelection) {
		return (DomainResultProducer) sqmSelection.getSelectableNode().accept( this );
	}

	private int fetchDepth = 0;

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		final List<Fetch> fetches = CollectionHelper.arrayList( fetchParent.getReferencedMappingType().getNumberOfFetchables() );

		//noinspection Convert2Lambda
		final Consumer<Fetchable> fetchableConsumer = new Consumer<Fetchable>() {
			@Override
			public void accept(Fetchable fetchable) {
				final Fetch biDirectionalFetch = circularFetchDetector.findBiDirectionalFetch(
						fetchParent,
						fetchable,
						getSqlAstCreationState().getCurrentProcessingState()
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
			}
		};

// todo (6.0) : determine how to best handle TREAT
//		fetchParent.getReferencedMappingContainer().visitKeyFetchables( fetchableConsumer, treatTargetType );
//		fetchParent.getReferencedMappingContainer().visitFetchables( fetchableConsumer, treatTargetType );
		fetchParent.getReferencedMappingContainer().visitKeyFetchables( fetchableConsumer, null );
		fetchParent.getReferencedMappingContainer().visitFetchables( fetchableConsumer, null );

		return fetches;
	}

	private Fetch buildFetch(FetchParent fetchParent, Fetchable fetchable) {
		// fetch has access to its parent in addition to the parent having its fetches.
		//
		// we could sever the parent -> fetch link ... it would not be "seen" while walking
		// but it would still have access to its parent info - and be able to access its
		// "initializing" state as part of AfterLoadAction

		final NavigablePath fetchablePath = fetchParent.getNavigablePath().append( fetchable.getFetchableName() );

		final GraphImplementor<?> previousGraphNode = currentJpaGraphNode;

		final String alias;
		LockMode lockMode = LockMode.READ;
		FetchTiming fetchTiming = fetchable.getMappedFetchStrategy().getTiming();
		boolean joined = false;

		final SqmAttributeJoin fetchedJoin = getFromClauseIndex().findFetchedJoinByPath( fetchablePath );

		if ( fetchedJoin != null ) {
			// there was an explicit fetch in the SQM
			//		there should be a TableGroupJoin registered for this `fetchablePath` already
			//		because it
			assert getFromClauseIndex().getTableGroup( fetchablePath ) != null;

			fetchTiming = FetchTiming.IMMEDIATE;
			joined = true;
			alias = fetchedJoin.getExplicitAlias();
			lockMode = determineLockMode( alias );
		}
		else {
			// there was not an explicit fetch in the SQM
			alias = null;

			// see if we have any "influencer" in effect that indicates
			if ( this.currentJpaGraphNode != null && appliesTo( this.currentJpaGraphNode, fetchParent ) ) {
				final AttributeNodeImplementor<?> attributeNode = this.currentJpaGraphNode.findAttributeNode( fetchable.getFetchableName() );
				// todo (6.0) : need to account for `org.hibernate.graph.GraphSemantic` here as well
				if ( attributeNode != null ) {
					fetchTiming = FetchTiming.IMMEDIATE;
					joined = true;
				}
			}
			else if ( fetchInfluencers.hasEnabledFetchProfiles() ) {
				if ( fetchParent instanceof EntityResultNode ) {
					final EntityResultNode entityFetchParent = (EntityResultNode) fetchParent;
					final EntityMappingType entityMappingType = entityFetchParent.getEntityValuedModelPart().getEntityMappingType();
					final String fetchParentEntityName = entityMappingType.getEntityName();
					final String fetchableRole = fetchParentEntityName + "." + fetchable.getFetchableName();

					for ( String enabledFetchProfileName : fetchInfluencers.getEnabledFetchProfileNames() ) {
						final FetchProfile enabledFetchProfile = getCreationContext().getSessionFactory().getFetchProfile( enabledFetchProfileName );
						final org.hibernate.engine.profile.Fetch profileFetch = enabledFetchProfile.getFetchByRole( fetchableRole );

						fetchTiming = FetchTiming.IMMEDIATE;
						joined = joined || profileFetch.getStyle() == org.hibernate.engine.profile.Fetch.Style.JOIN;
					}
				}
			}

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
									fetchablePath,
									lhs,
									alias,
									JoinType.LEFT,
									LockMode.NONE,
									getSqlAliasBaseManager(),
									getSqlExpressionResolver(),
									getCreationContext()
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
					fetchablePath,
					fetchTiming,
					joined,
					lockMode,
					alias,
					StandardSqmSelectTranslator.this
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
		finally {
			currentJpaGraphNode = previousGraphNode;
		}
	}

	private static boolean appliesTo(GraphImplementor<?> graphNode, FetchParent fetchParent) {
		if ( ! ( fetchParent instanceof EntityResultNode ) ) {
			return false;
		}

		final EntityResultNode entityFetchParent = (EntityResultNode) fetchParent;
		final EntityMappingType entityFetchParentMappingType = entityFetchParent.getEntityValuedModelPart().getEntityMappingType();

		assert graphNode.getGraphedType() instanceof EntityDomainType;
		final EntityDomainType entityDomainType = (EntityDomainType) graphNode.getGraphedType();

		return entityDomainType.getHibernateEntityName().equals( entityFetchParentMappingType.getEntityName() );
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

	@Override
	public DynamicInstantiation visitDynamicInstantiation(SqmDynamicInstantiation<?> sqmDynamicInstantiation) {
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
	public Expression visitEntityTypeLiteralExpression(SqmLiteralEntityType sqmExpression) {
		final EntityDomainType<?> nodeType = sqmExpression.getNodeType();
		final EntityPersister mappingDescriptor = getCreationContext().getDomainModel().getEntityDescriptor( nodeType.getHibernateEntityName() );

		return new EntityTypeLiteral( mappingDescriptor );
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
	public CteStatement translate(SqmCteStatement sqmCte) {
		return visitCteStatement( sqmCte );
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
