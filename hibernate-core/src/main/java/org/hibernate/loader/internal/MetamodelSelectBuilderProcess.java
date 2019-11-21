/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.spi.Loadable;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.results.spi.CircularFetchDetector;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.FetchableContainer;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class MetamodelSelectBuilderProcess {
	private static final Logger log = Logger.getLogger( MetamodelSelectBuilderProcess.class );

	public interface SqlAstDescriptor {
		SelectStatement getSqlAst();
		List<JdbcParameter> getJdbcParameters();
	}

	@SuppressWarnings("WeakerAccess")
	public static SqlAstDescriptor createSelect(
			SessionFactoryImplementor sessionFactory,
			Loadable loadable,
			List<ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult domainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		final MetamodelSelectBuilderProcess process = new MetamodelSelectBuilderProcess(
				sessionFactory,
				loadable,
				partsToSelect,
				restrictedPart,
				domainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions
		);

		return process.execute();
	}

	private final SqlAstCreationContext creationContext;
	private final Loadable loadable;
	private final List<ModelPart> partsToSelect;
	private final ModelPart restrictedPart;
	private final DomainResult domainResult;
	private final int numberOfKeysToLoad;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;


	private MetamodelSelectBuilderProcess(
			SqlAstCreationContext creationContext,
			Loadable loadable,
			List<ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult domainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions) {
		this.creationContext = creationContext;
		this.loadable = loadable;
		this.partsToSelect = partsToSelect;
		this.restrictedPart = restrictedPart;
		this.domainResult = domainResult;
		this.numberOfKeysToLoad = numberOfKeysToLoad;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockOptions = lockOptions != null ? lockOptions : LockOptions.NONE;
	}

	private SqlAstDescriptor execute() {
		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final List<DomainResult> domainResults;

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				creationContext
		);

		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getPathName() );

		final TableGroup rootTableGroup = loadable.createRootTableGroup(
				rootNavigablePath,
				null,
				null,
				lockOptions.getLockMode(),
				sqlAstCreationState.getSqlAliasBaseManager(),
				sqlAstCreationState.getSqlExpressionResolver(),
				() -> rootQuerySpec::applyPredicate,
				creationContext
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( rootNavigablePath, rootTableGroup );

		if ( partsToSelect != null && !partsToSelect.isEmpty() ) {
			domainResults = new ArrayList<>();
			for ( ModelPart part : partsToSelect ) {
				final NavigablePath navigablePath = rootNavigablePath.append( part.getPartName() );
				domainResults.add(
						part.createDomainResult(
								navigablePath,
								rootTableGroup,
								null,
								sqlAstCreationState
						)
				);
			}
		}
		else {
			// use the one passed to the constructor or create one (maybe always create and pass?)
			//		allows re-use as they can be re-used to save on memory - they
			//		do not share state between
			final DomainResult domainResult;
			if ( this.domainResult != null ) {
				// used the one passed to the constructor
				domainResult = this.domainResult;
			}
			else {
				// create one
				domainResult = loadable.createDomainResult(
						rootNavigablePath,
						rootTableGroup,
						null,
						sqlAstCreationState
				);
			}

			domainResults = Collections.singletonList( domainResult );
		}

		final int numberOfKeyColumns = restrictedPart.getJdbcTypeCount(
				creationContext.getDomainModel().getTypeConfiguration()
		);

		final List<JdbcParameter> jdbcParameters = new ArrayList<>( numberOfKeyColumns * numberOfKeysToLoad );

		applyKeyRestriction(
				rootQuerySpec,
				rootNavigablePath,
				rootTableGroup,
				restrictedPart,
				numberOfKeyColumns,
				jdbcParameters::add,
				sqlAstCreationState
		);

		return new SqlAstDescriptorImpl(
				new SelectStatement( rootQuerySpec, domainResults ),
				jdbcParameters
		);
	}

	private void applyKeyRestriction(
			QuerySpec rootQuerySpec,
			NavigablePath rootNavigablePath,
			TableGroup rootTableGroup,
			ModelPart keyPart,
			int numberOfKeyColumns,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			LoaderSqlAstCreationState sqlAstCreationState) {
		final NavigablePath keyPath = rootNavigablePath.append( keyPart.getPartName() );

		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();

		if ( numberOfKeyColumns == 1 ) {
			assert keyPart instanceof BasicValuedModelPart;
			final BasicValuedModelPart basicKeyPart = (BasicValuedModelPart) keyPart;

			final JdbcMapping jdbcMapping = basicKeyPart.getJdbcMapping();

			final String tableExpression = basicKeyPart.getContainingTableExpression();
			final String columnExpression = basicKeyPart.getMappedColumnExpression();
			final TableReference tableReference = rootTableGroup.resolveTableReference( tableExpression );
			final ColumnReference columnRef = (ColumnReference) sqlExpressionResolver.resolveSqlExpression(
					SqlExpressionResolver.createColumnReferenceKey( tableReference, columnExpression ),
					p -> new ColumnReference(
							tableReference,
							columnExpression,
							jdbcMapping,
							creationContext.getSessionFactory()
					)
			);

			if ( numberOfKeysToLoad == 1 ) {
				final JdbcParameter jdbcParameter = new JdbcParameterImpl( jdbcMapping );
				jdbcParameterConsumer.accept( jdbcParameter );

				rootQuerySpec.applyPredicate(
						new ComparisonPredicate( columnRef, ComparisonOperator.EQUAL, jdbcParameter )
				);
			}
			else {
				final InListPredicate predicate = new InListPredicate( columnRef );
				for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
					for ( int j = 0; j < numberOfKeyColumns; j++ ) {
						final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnRef.getJdbcMapping() );
						jdbcParameterConsumer.accept( jdbcParameter );
						predicate.addExpression( jdbcParameter );
					}
				}
				rootQuerySpec.applyPredicate( predicate );
			}
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( numberOfKeyColumns );

			keyPart.visitColumns(
					(columnExpression, containingTableExpression, jdbcMapping) -> {
						final TableReference tableReference = rootTableGroup.resolveTableReference( containingTableExpression );
						columnReferences.add(
								(ColumnReference) sqlExpressionResolver.resolveSqlExpression(
										SqlExpressionResolver.createColumnReferenceKey( tableReference, columnExpression ),
										p -> new ColumnReference(
												tableReference,
												columnExpression,
												jdbcMapping,
												creationContext.getSessionFactory()
										)
								)
						);
					}
			);

			final SqlTuple tuple = new SqlTuple( columnReferences, keyPart );
			final InListPredicate predicate = new InListPredicate( tuple );

			for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
				final List<JdbcParameter> tupleParams = new ArrayList<>(  );
				for ( int j = 0; j < numberOfKeyColumns; j++ ) {
					final ColumnReference columnReference = columnReferences.get( j );
					final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnReference.getJdbcMapping() );
					jdbcParameterConsumer.accept( jdbcParameter );
					tupleParams.add( jdbcParameter );
				}
				final SqlTuple paramTuple = new SqlTuple( tupleParams, keyPart );
				predicate.addExpression( paramTuple );
			}

			rootQuerySpec.applyPredicate( predicate );
		}
	}

	private final CircularFetchDetector circularFetchDetector = new CircularFetchDetector();
	private int fetchDepth = 0;

	private List<Fetch> visitFetches(FetchParent fetchParent, LoaderSqlAstCreationState creationState) {
		log.tracef( "Starting visitation of FetchParent's Fetchables : %s", fetchParent.getNavigablePath() );

		final List<Fetch> fetches = new ArrayList<>();

		final Consumer<Fetchable> processor = fetchable -> {
			final NavigablePath fetchablePath = fetchParent.getNavigablePath().append( fetchable.getFetchableName() );

			final Fetch biDirectionalFetch = circularFetchDetector.findBiDirectionalFetch(
					fetchParent,
					fetchable,
					creationState
			);

			if ( biDirectionalFetch != null ) {
				fetches.add( biDirectionalFetch );
				return;
			}

			LockMode lockMode = LockMode.READ;
			FetchTiming fetchTiming = fetchable.getMappedFetchStrategy().getTiming();
			boolean joined = fetchable.getMappedFetchStrategy().getStyle() == FetchStyle.JOIN;

//			if ( loadable instanceof PluralValuedNavigable ) {
//				// processing a collection-loader
//
//				// if the `fetchable` is the "collection owner" and the collection owner is available in Session - don't join
//				final String collectionMappedByProperty = ( (PluralValuedNavigable) rootContainer ).getCollectionDescriptor()
//						.getMappedByProperty();
//				if ( collectionMappedByProperty != null && collectionMappedByProperty.equals( fetchable.getNavigableName() ) ) {
//					joined = false;
//				}
//			}

			final Integer maximumFetchDepth = creationContext.getMaximumFetchDepth();

			if ( maximumFetchDepth != null ) {
				if ( fetchDepth == maximumFetchDepth ) {
					joined = false;
				}
				else if ( fetchDepth > maximumFetchDepth ) {
					return;
				}
			}

			try {
				if(!(fetchable instanceof BasicValuedModelPart)) {
					fetchDepth--;
				}
				Fetch fetch = fetchable.generateFetch(
						fetchParent,
						fetchablePath,
						fetchTiming,
						joined,
						lockMode,
						null,
						creationState
				);
				fetches.add( fetch );
			}
			finally {
				if(!(fetchable instanceof BasicValuedModelPart)) {
					fetchDepth--;
				}
			}
		};

		final FetchableContainer referencedMappingContainer = fetchParent.getReferencedMappingContainer();
		referencedMappingContainer.visitKeyFetchables( processor, null );
		referencedMappingContainer.visitFetchables( processor, null );

		return fetches;
	}

	static class SqlAstDescriptorImpl implements SqlAstDescriptor {
		private final SelectStatement sqlAst;
		private final List<JdbcParameter> jdbcParameters;

		public SqlAstDescriptorImpl(
				SelectStatement sqlAst,
				List<JdbcParameter> jdbcParameters) {
			this.sqlAst = sqlAst;
			this.jdbcParameters = jdbcParameters;
		}

		@Override
		public SelectStatement getSqlAst() {
			return sqlAst;
		}

		@Override
		public List<JdbcParameter> getJdbcParameters() {
			return jdbcParameters;
		}
	}
}

