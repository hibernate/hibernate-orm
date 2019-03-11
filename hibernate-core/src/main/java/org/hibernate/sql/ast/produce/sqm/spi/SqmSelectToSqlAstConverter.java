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
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.expression.SqmLiteralEntityType;
import org.hibernate.query.sqm.tree.expression.domain.SqmDiscriminatorReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypedReference;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationArgument;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.SelectStatement;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.spi.expression.domain.DiscriminatorReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiation;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
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

	private Fetch buildFetch(FetchParent fetchParent, Fetchable fetchable) {
		LockMode lockMode = LockMode.READ;
		FetchTiming fetchTiming = fetchable.getMappedFetchStrategy().getTiming();

		final SqmNavigableJoin fetchedJoin = findFetchedJoin( fetchParent, fetchable );

		final String alias;
		boolean joined;
		if ( fetchedJoin != null ) {
			fetchTiming = FetchTiming.IMMEDIATE;
			joined = true;

			lockMode = getQueryOptions().getLockOptions().getEffectiveLockMode(
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

		final Integer maximumFetchDepth = getCreationContext().getMaximumFetchDepth();

		if ( maximumFetchDepth != null ) {
			if ( fetchDepth == maximumFetchDepth ) {
				joined = false;
			}
			else if ( fetchDepth > maximumFetchDepth ) {
				return null;
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
	public Expression visitDiscriminatorReference(SqmDiscriminatorReference expression) {
		final SqmEntityTypedReference binding = expression.getSourceReference();
		final TableGroup resolvedTableGroup = getFromClauseIndex().findResolvedTableGroup( binding.getExportedFromElement() );
		final EntityValuedNavigableReference entityReference = (EntityValuedNavigableReference) resolvedTableGroup.getNavigableReference();

		return new DiscriminatorReference( entityReference );
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
