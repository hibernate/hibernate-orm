/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.Collections;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 */
public class EntityJoinFromElement extends FromElement {
	public EntityJoinFromElement(
			HqlSqlWalker walker,
			FromClause fromClause,
			EntityPersister entityPersister,
			JoinType joinType,
			boolean fetchProperties,
			String alias) {
		initialize( walker );

		final String tableName = ( (Joinable) entityPersister ).getTableName();
		final String tableAlias = fromClause.getAliasGenerator().createName( entityPersister.getEntityName() );

		final EntityType entityType = (EntityType) ( (Queryable) entityPersister ).getType();

		initializeEntity(
				fromClause,
				entityPersister.getEntityName(),
				entityPersister,
				entityType,
				alias,
				tableAlias
		);

		EntityJoinJoinSequenceImpl joinSequence = new EntityJoinJoinSequenceImpl(
				getSessionFactoryHelper().getFactory(),
				entityType,
				tableName,
				tableAlias,
				joinType

		);
		setJoinSequence( joinSequence );

		setAllPropertyFetch( fetchProperties );

		// Add to the query spaces.
		fromClause.getWalker().addQuerySpaces( entityPersister.getQuerySpaces() );

		setType( HqlSqlTokenTypes.ENTITY_JOIN );
//		setType( HqlSqlTokenTypes.FROM_FRAGMENT );
		setText( tableName );
	}

	@Override
	public void setText(String s) {
		super.setText( s );
	}


	private static class EntityJoinJoinSequenceImpl extends JoinSequence {
		private final SessionFactoryImplementor factory;
		private final String entityTableText;
		private final String entityTableAlias;
		private final EntityType entityType;
		private final JoinType joinType;

		public EntityJoinJoinSequenceImpl(
				SessionFactoryImplementor factory,
				EntityType entityType,
				String entityTableText,
				String entityTableAlias,
				JoinType joinType) {
			super( factory );
			this.factory = factory;
			this.entityType = entityType;
			this.entityTableText = entityTableText;
			this.entityTableAlias = entityTableAlias;
			this.joinType = joinType;

			setUseThetaStyle( false );
		}

		/**
		 * Ugh!
		 */
		@Override
		public JoinFragment toJoinFragment(
				Map enabledFilters,
				boolean includeAllSubclassJoins,
				String withClauseFragment,
				String withClauseJoinAlias,
				String withClauseCollectionJoinAlias) throws MappingException {
			final String joinString;
			switch ( joinType ) {
				case INNER_JOIN:
					joinString = " inner join ";
					break;
				case LEFT_OUTER_JOIN:
					joinString = " left outer join ";
					break;
				case RIGHT_OUTER_JOIN:
					joinString = " right outer join ";
					break;
				case FULL_JOIN:
					joinString = " full outer join ";
					break;
				default:
					throw new AssertionFailure( "undefined join type" );
			}

			final StringBuilder buffer = new StringBuilder();
			buffer.append( joinString )
					.append( entityTableText )
					.append( ' ' )
					.append( entityTableAlias )
					.append( " on " );

			final String filters = 	entityType.getOnCondition(
					entityTableAlias,
					factory,
					enabledFilters,
					Collections.<String>emptySet()
			);

			buffer.append( filters );
			if ( withClauseFragment != null ) {
				if ( StringHelper.isNotEmpty( filters ) ) {
					buffer.append( " and " );
				}
				buffer.append( withClauseFragment );
			}

			return new EntityJoinJoinFragment( buffer.toString() );
		}
	}

	private static class EntityJoinJoinFragment extends JoinFragment {
		private final String fragmentString;

		public EntityJoinJoinFragment(String fragmentString) {
			this.fragmentString = fragmentString;
		}

		@Override
		public void addJoin(
				String tableName,
				String alias,
				String[] fkColumns,
				String[] pkColumns,
				JoinType joinType) {
		}

		@Override
		public void addJoin(
				String tableName,
				String alias,
				String[] fkColumns,
				String[] pkColumns,
				JoinType joinType,
				String on) {
		}

		@Override
		public void addCrossJoin(String tableName, String alias) {
		}

		@Override
		public void addJoins(String fromFragment, String whereFragment) {
		}

		@Override
		public String toFromFragmentString() {
			return fragmentString;
		}

		@Override
		public String toWhereFragmentString() {
			return null;
		}

		@Override
		public void addCondition(String alias, String[] fkColumns, String[] pkColumns) {

		}

		@Override
		public boolean addCondition(String condition) {
			return false;
		}

		@Override
		public JoinFragment copy() {
			return null;
		}

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// n/a


	@Override
	protected void initializeComponentJoin(FromElementType elementType) {
	}

	@Override
	public void initializeCollection(FromClause fromClause, String classAlias, String tableAlias) {
	}

	@Override
	public String getCollectionSuffix() {
		return null;
	}

	@Override
	public void setCollectionSuffix(String suffix) {
	}
}
