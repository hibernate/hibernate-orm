/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;

import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.FromClause;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.FromElementFactory;
import org.hibernate.hql.internal.ast.tree.ImpliedFromElement;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.sql.JoinType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.jpa.QueryHints.HINT_FETCHGRAPH;
import static org.hibernate.jpa.QueryHints.HINT_LOADGRAPH;

/**
 * Encapsulates a JPA EntityGraph provided through a JPQL query hint.  Converts the fetches into a list of AST
 * FromElements.  The logic is kept here as much as possible in order to make it easy to remove this in the future,
 * once our AST is improved and this "hack" is no longer needed.
 *
 * @author Brett Meyer
 */
public class EntityGraphQueryHint {
	private final String hintName;
	private final EntityGraph<?> originEntityGraph;

	public EntityGraphQueryHint(String hintName, EntityGraph<?> originEntityGraph) {
		assert hintName != null;
		assert HINT_FETCHGRAPH.equals( hintName ) || HINT_LOADGRAPH.equals( hintName );

		this.hintName = hintName;
		this.originEntityGraph = originEntityGraph;
	}

	public String getHintName() {
		return hintName;
	}

	public EntityGraph<?> getOriginEntityGraph() {
		return originEntityGraph;
	}

	public List<FromElement> toFromElements(FromClause fromClause, HqlSqlWalker walker) {
		// If a role already has an explicit fetch in the query, skip it in the graph.
		Map<String, FromElement> explicitFetches = new HashMap<String, FromElement>();
		for ( Object o : fromClause.getFromElements() ) {
			final FromElement fromElement = (FromElement) o;
			if ( fromElement.getRole() != null  && ! (fromElement instanceof ImpliedFromElement) ) {
				explicitFetches.put( fromElement.getRole(), fromElement );
			}
		}

		return getFromElements(
				fromClause.getLevel() == FromClause.ROOT_LEVEL ? originEntityGraph.getAttributeNodes():
					Collections.emptyList(),
				fromClause.getFromElement(),
				fromClause,
				walker,
				explicitFetches
		);
	}

	private List<FromElement> getFromElements(
			List attributeNodes,
			FromElement origin,
			FromClause fromClause,
			HqlSqlWalker walker,
			Map<String, FromElement> explicitFetches) {
		final List<FromElement> fromElements = new ArrayList<FromElement>();

		for ( Object obj : attributeNodes ) {
			final AttributeNode<?> attributeNode = (AttributeNode<?>) obj;

			final String attributeName = attributeNode.getAttributeName();
			final String className = origin.getClassName();
			// TODO: This is ignored by collection types and probably wrong for entity types.  Presumably it screws
			// with inheritance.
			final String role = className + "." + attributeName;
			final String classAlias = origin.getClassAlias();
			final String originTableAlias = origin.getTableAlias();
			final Type propertyType = origin.getPropertyType( attributeName, attributeName );

			try {
				FromElement fromElement = explicitFetches.get( role );
				boolean explicitFromElement = false;
				if ( fromElement == null ) {
					if ( propertyType.isEntityType() ) {
						final EntityType entityType = (EntityType) propertyType;

						final String[] columns = origin.toColumns( originTableAlias, attributeName, false );
						final String tableAlias = walker.getAliasGenerator().createName(
								entityType.getAssociatedEntityName()
						);

						final FromElementFactory fromElementFactory = new FromElementFactory(
								fromClause, origin,
								attributeName, classAlias, columns, false
						);
						final JoinSequence joinSequence = walker.getSessionFactoryHelper().createJoinSequence(
								false, entityType, tableAlias, JoinType.LEFT_OUTER_JOIN, columns
						);
						fromElement = fromElementFactory.createEntityJoin(
								entityType.getAssociatedEntityName(),
								tableAlias,
								joinSequence,
								true,
								walker.isInFrom(),
								entityType,
								role,
								null
						);
					}
					else if ( propertyType.isCollectionType() ) {
						CollectionType collectionType = (CollectionType) propertyType;
						final String[] columns = origin.toColumns( originTableAlias, attributeName, false );

						final FromElementFactory fromElementFactory = new FromElementFactory(
								fromClause, origin,
								attributeName, classAlias, columns, false
						);
						final QueryableCollection queryableCollection = walker.getSessionFactoryHelper()
								.requireQueryableCollection( collectionType.getRole() );
						fromElement = fromElementFactory.createCollection(
								queryableCollection, collectionType.getRole(), JoinType.LEFT_OUTER_JOIN, true, false
						);
					}
				}
				else {
					explicitFromElement = true;
					fromElement.setInProjectionList( true );
					fromElement.setFetch( true );
				}

				if ( fromElement != null ) {
					if( !explicitFromElement ){
						fromElements.add( fromElement );
					}

					// recurse into subgraphs
					for ( Subgraph<?> subgraph : attributeNode.getSubgraphs().values() ) {
						fromElements.addAll(
								getFromElements(
										subgraph.getAttributeNodes(), fromElement,
										fromClause, walker, explicitFetches
								)
						);
					}
				}
			}
			catch (Exception e) {
				throw new QueryException( "Could not apply the EntityGraph to the Query!", e );
			}
		}

		return fromElements;
	}
}
