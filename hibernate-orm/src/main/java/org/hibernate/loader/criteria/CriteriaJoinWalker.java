/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.AbstractEntityJoinWalker;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;

/**
 * A <tt>JoinWalker</tt> for <tt>Criteria</tt> queries.
 *
 * @author Gavin King
 * @see CriteriaLoader
 */
public class CriteriaJoinWalker extends AbstractEntityJoinWalker {

	//TODO: add a CriteriaImplementor interface
	//      this class depends directly upon CriteriaImpl in the impl package...

	private final CriteriaQueryTranslator translator;
	private final Set querySpaces;
	private final Type[] resultTypes;
	private final boolean[] includeInResultRow;

	//the user visible aliases, which are unknown to the superclass,
	//these are not the actual "physical" SQL aliases
	private final String[] userAliases;
	private final List<String> userAliasList = new ArrayList<String>();
	private final List<Type> resultTypeList = new ArrayList<Type>();
	private final List<Boolean> includeInResultRowList = new ArrayList<Boolean>();

	public Type[] getResultTypes() {
		return resultTypes;
	}

	public String[] getUserAliases() {
		return userAliases;
	}

	public boolean[] includeInResultRow() {
		return includeInResultRow;
	}

	public CriteriaJoinWalker(
			final OuterJoinLoadable persister,
			final CriteriaQueryTranslator translator,
			final SessionFactoryImplementor factory,
			final CriteriaImpl criteria,
			final String rootEntityName,
			final LoadQueryInfluencers loadQueryInfluencers) {
		this( persister, translator, factory, criteria, rootEntityName, loadQueryInfluencers, null );
	}

	public CriteriaJoinWalker(
			final OuterJoinLoadable persister,
			final CriteriaQueryTranslator translator,
			final SessionFactoryImplementor factory,
			final CriteriaImpl criteria,
			final String rootEntityName,
			final LoadQueryInfluencers loadQueryInfluencers,
			final String alias) {
		super( persister, factory, loadQueryInfluencers, alias );

		this.translator = translator;

		querySpaces = translator.getQuerySpaces();

		if ( translator.hasProjection() ) {
			initProjection(
					translator.getSelect(),
					translator.getWhereCondition(),
					translator.getOrderBy(),
					translator.getGroupBy(),
					LockOptions.NONE
			);
			resultTypes = translator.getProjectedTypes();
			userAliases = translator.getProjectedAliases();
			includeInResultRow = new boolean[resultTypes.length];
			Arrays.fill( includeInResultRow, true );
		}
		else {
			initAll( translator.getWhereCondition(), translator.getOrderBy(), LockOptions.NONE );
			// root entity comes last
			userAliasList.add( criteria.getAlias() ); //root entity comes *last*
			resultTypeList.add( translator.getResultType( criteria ) );
			includeInResultRowList.add( true );
			userAliases = ArrayHelper.toStringArray( userAliasList );
			resultTypes = ArrayHelper.toTypeArray( resultTypeList );
			includeInResultRow = ArrayHelper.toBooleanArray( includeInResultRowList );
		}
	}

	@Override
	protected JoinType getJoinType(
			OuterJoinLoadable persister,
			final PropertyPath path,
			int propertyNumber,
			AssociationType associationType,
			FetchMode metadataFetchMode,
			CascadeStyle metadataCascadeStyle,
			String lhsTable,
			String[] lhsColumns,
			final boolean nullable,
			final int currentDepth) throws MappingException {
		final JoinType resolvedJoinType;
		if ( translator.isJoin( path.getFullPath() ) ) {
			resolvedJoinType = translator.getJoinType( path.getFullPath() );
		}
		else {
			if ( translator.hasProjection() ) {
				resolvedJoinType = JoinType.NONE;
			}
			else {
				FetchMode fetchMode = translator.getRootCriteria().getFetchMode( path.getFullPath() );
				if ( isDefaultFetchMode( fetchMode ) ) {
					if ( persister != null ) {
						if ( isJoinFetchEnabledByProfile( persister, path, propertyNumber ) ) {
							if ( isDuplicateAssociation( lhsTable, lhsColumns, associationType ) ) {
								resolvedJoinType = JoinType.NONE;
							}
							else if ( isTooDeep( currentDepth ) || ( associationType.isCollectionType() && isTooManyCollections() ) ) {
								resolvedJoinType = JoinType.NONE;
							}
							else {
								resolvedJoinType = getJoinType( nullable, currentDepth );
							}
						}
						else {
							resolvedJoinType = super.getJoinType(
									persister,
									path,
									propertyNumber,
									associationType,
									metadataFetchMode,
									metadataCascadeStyle,
									lhsTable,
									lhsColumns,
									nullable,
									currentDepth
							);
						}
					}
					else {
						resolvedJoinType = super.getJoinType(
								associationType,
								metadataFetchMode,
								path,
								lhsTable,
								lhsColumns,
								nullable,
								currentDepth,
								metadataCascadeStyle
						);

					}
				}
				else {
					if ( fetchMode == FetchMode.JOIN ) {
						isDuplicateAssociation(
								lhsTable,
								lhsColumns,
								associationType
						); //deliberately ignore return value!
						resolvedJoinType = getJoinType( nullable, currentDepth );
					}
					else {
						resolvedJoinType = JoinType.NONE;
					}
				}
			}
		}
		return resolvedJoinType;
	}

	@Override
	protected JoinType getJoinType(
			AssociationType associationType,
			FetchMode config,
			PropertyPath path,
			String lhsTable,
			String[] lhsColumns,
			boolean nullable,
			int currentDepth,
			CascadeStyle cascadeStyle) throws MappingException {
		return getJoinType(
				null,
				path,
				-1,
				associationType,
				config,
				cascadeStyle,
				lhsTable,
				lhsColumns,
				nullable,
				currentDepth
		);
	}

	private static boolean isDefaultFetchMode(FetchMode fetchMode) {
		return fetchMode == null || fetchMode == FetchMode.DEFAULT;
	}

	/**
	 * Use the discriminator, to narrow the select to instances
	 * of the queried subclass, also applying any filters.
	 */
	@Override
	protected String getWhereFragment() throws MappingException {
		return super.getWhereFragment() +
				( (Queryable) getPersister() ).filterFragment(
						getAlias(),
						getLoadQueryInfluencers().getEnabledFilters()
				);
	}

	@Override
	protected String generateTableAlias(int n, PropertyPath path, Joinable joinable) {
		// TODO: deal with side-effects (changes to includeInResultRowList, userAliasList, resultTypeList)!!!

		// for collection-of-entity, we are called twice for given "path"
		// once for the collection Joinable, once for the entity Joinable.
		// the second call will/must "consume" the alias + perform side effects according to consumesEntityAlias()
		// for collection-of-other, however, there is only one call 
		// it must "consume" the alias + perform side effects, despite what consumeEntityAlias() return says
		// 
		// note: the logic for adding to the userAliasList is still strictly based on consumesEntityAlias return value
		boolean checkForSqlAlias = joinable.consumesEntityAlias();

		if ( !checkForSqlAlias && joinable.isCollection() ) {
			// is it a collection-of-other (component or value) ?
			CollectionPersister collectionPersister = (CollectionPersister) joinable;
			Type elementType = collectionPersister.getElementType();
			if ( elementType.isComponentType() || !elementType.isEntityType() ) {
				checkForSqlAlias = true;
			}
		}

		String sqlAlias = null;

		if ( checkForSqlAlias ) {
			final Criteria subcriteria = translator.getCriteria( path.getFullPath() );
			sqlAlias = subcriteria == null ? null : translator.getSQLAlias( subcriteria );

			if ( joinable.consumesEntityAlias() && !translator.hasProjection() ) {
				includeInResultRowList.add( subcriteria != null && subcriteria.getAlias() != null );
				if ( sqlAlias != null ) {
					if ( subcriteria.getAlias() != null ) {
						userAliasList.add( subcriteria.getAlias() );
						resultTypeList.add( translator.getResultType( subcriteria ) );
					}
				}
			}
		}

		if ( sqlAlias == null ) {
			sqlAlias = super.generateTableAlias( n + translator.getSQLAliasCount(), path, joinable );
		}

		return sqlAlias;
	}

	@Override
	protected String generateRootAlias(String tableName) {
		return CriteriaQueryTranslator.ROOT_SQL_ALIAS;
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	@Override
	public String getComment() {
		return "criteria query";
	}

	@Override
	protected String getWithClause(PropertyPath path) {
		return translator.getWithClause( path.getFullPath() );
	}

	@Override
	protected boolean hasRestriction(PropertyPath path) {
		return translator.hasRestriction( path.getFullPath() );
	}
}
