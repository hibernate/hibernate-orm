/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.util.SessionFactoryHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class MapKeyEntityFromElement extends FromElement {
	private final boolean useThetaJoin;

	public MapKeyEntityFromElement(boolean useThetaJoin) {
		super();
		this.useThetaJoin = useThetaJoin;
	}

	@Override
	public boolean isImplied() {
		return useThetaJoin;
	}

	@Override
	public int getType() {
		return useThetaJoin ? HqlSqlTokenTypes.FROM_FRAGMENT : HqlSqlTokenTypes.JOIN_FRAGMENT;
	}

	public static MapKeyEntityFromElement buildKeyJoin(FromElement collectionFromElement) {
		final HqlSqlWalker walker = collectionFromElement.getWalker();
		final SessionFactoryHelper sfh = walker.getSessionFactoryHelper();
		final SessionFactoryImplementor sf = sfh.getFactory();

		final QueryableCollection collectionPersister = collectionFromElement.getQueryableCollection();
		final Type indexType = collectionPersister.getIndexType();
		if ( indexType == null ) {
			throw new IllegalArgumentException( "Given collection is not indexed" );
		}
		if ( !indexType.isEntityType() ) {
			throw new IllegalArgumentException( "Given collection does not have an entity index" );
		}

		final EntityType indexEntityType = (EntityType) indexType;
		final EntityPersister indexEntityPersister = (EntityPersister) indexEntityType.getAssociatedJoinable( sf );

		final String rhsAlias = walker.getAliasGenerator().createName( indexEntityPersister.getEntityName() );
		final boolean useThetaJoin = collectionFromElement.getJoinSequence().isThetaStyle();

		MapKeyEntityFromElement join = new MapKeyEntityFromElement( useThetaJoin );
		join.initialize( HqlSqlTokenTypes.JOIN_FRAGMENT, ( (Joinable) indexEntityPersister ).getTableName() );
		join.initialize( collectionFromElement.getWalker() );

		join.initializeEntity(
				collectionFromElement.getFromClause(),
				indexEntityPersister.getEntityName(),
				indexEntityPersister,
				indexEntityType,
				"<map-key-join-" + collectionFromElement.getClassAlias() + ">",
				rhsAlias
		);

//		String[] joinColumns = determineJoinColuns( collectionPersister, joinTableAlias );
		// todo : assumes columns, no formulas
		String[] joinColumns = collectionPersister.getIndexColumnNames( collectionFromElement.getCollectionTableAlias() );

		JoinSequence joinSequence = sfh.createJoinSequence(
				useThetaJoin,
				indexEntityType,
				rhsAlias,
				// todo : ever a time when INNER is appropriate?
				//JoinType.LEFT_OUTER_JOIN,
				// needs to be an inner join because of how JoinSequence/JoinFragment work - ugh
				JoinType.INNER_JOIN,
				joinColumns
		);
		join.setJoinSequence( joinSequence );

		join.setOrigin( collectionFromElement, true );
		join.setColumns( joinColumns );

		join.setUseFromFragment( collectionFromElement.useFromFragment() );
		join.setUseWhereFragment( collectionFromElement.useWhereFragment() );

		walker.addQuerySpaces( indexEntityPersister.getQuerySpaces() );

		return join;
	}
}
