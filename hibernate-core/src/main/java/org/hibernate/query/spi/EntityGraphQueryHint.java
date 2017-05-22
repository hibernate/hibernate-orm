/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.graph.spi.EntityGraphImplementor;

/**
 * Encapsulates a JPA EntityGraph provided through a JPQL query hint.  Converts the fetches into a list of AST
 * FromElements.  The logic is kept here as much as possible in order to make it easy to remove this in the future,
 * once our AST is improved and this "hack" is no longer needed.
 *
 * @author Brett Meyer
 */
public class EntityGraphQueryHint {
	public enum Type {
		/**
		 * Indicates a "fetch graph" EntityGraph.  Attributes explicitly specified
		 * as AttributeNodes are treated as FetchType.EAGER (via join fetch or
		 * subsequent select).
		 * <p/>
		 * Note: Currently, attributes that are not specified are treated as
		 * FetchType.LAZY or FetchType.EAGER depending on the attribute's definition
		 * in metadata, rather than forcing FetchType.LAZY.
		 */
		FETCH( "javax.persistence.fetchgraph" ),

		/**
		 * Indicates a "load graph" EntityGraph.  Attributes explicitly specified
		 * as AttributeNodes are treated as FetchType.EAGER (via join fetch or
		 * subsequent select).  Attributes that are not specified are treated as
		 * FetchType.LAZY or FetchType.EAGER depending on the attribute's definition
		 * in metadata
		 */
		LOAD( "javax.persistence.loadgraph" ),

		NONE( null );

		private final String jpaHintName;

		Type(String jpaHintName) {
			this.jpaHintName = jpaHintName;
		}

		public String getJpaHintName() {
			return jpaHintName;
		}

		public static Type fromJpaHintName(String hintName) {
			assert hintName != null;

			if ( FETCH.getJpaHintName().equals( hintName ) ) {
				return FETCH;
			}

			if ( LOAD.getJpaHintName().equalsIgnoreCase( hintName ) ) {
				return LOAD;
			}

			throw new IllegalArgumentException( "Unknown EntityGraph hint type name [" + hintName + "]" );
		}
	}

	private final Type type;
	private final EntityGraphImplementor<?> hintedGraph;

	public EntityGraphQueryHint(String hintName, EntityGraphImplementor<?> hintedGraph) {
		this( Type.fromJpaHintName( hintName ), hintedGraph );
	}

	public EntityGraphQueryHint(Type type, EntityGraphImplementor<?> hintedGraph) {
		this.type = type;
		this.hintedGraph = hintedGraph;
	}

	public Type getType() {
		return type;
	}

	public String getHintName() {
		return getType().getJpaHintName();
	}

	public EntityGraphImplementor<?> getHintedGraph() {
		return hintedGraph;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// The plan is to add these as part of the SQM -> SQL conversion.  In fact
	// those hooks are already in place, see SqmSelectToSqlAstConverter.applyFetchesAndEntityGraph

//	public List<FromElement> toFromElements(FromClause fromClause, HqlSqlWalker walker) {
//		// If a role already has an explicit fetch in the query, skip it in the graph.
//		Map<String, FromElement> explicitFetches = new HashMap<String, FromElement>();
//		for ( Object o : fromClause.getFromElements() ) {
//			final FromElement fromElement = (FromElement) o;
//			if ( fromElement.getRole() != null  && ! (fromElement instanceof ImpliedFromElement) ) {
//				explicitFetches.put( fromElement.getRole(), fromElement );
//			}
//		}
//
//		return getFromElements(
//				fromClause.getLevel() == FromClause.ROOT_LEVEL ? hintedGraph.getAttributeNodes():
//					Collections.emptyList(),
//				fromClause.getFromElement(),
//				fromClause,
//				walker,
//				explicitFetches
//		);
//	}
//
//	private List<FromElement> getFromElements(
//			List attributeNodes,
//			FromElement origin,
//			FromClause fromClause,
//			HqlSqlWalker walker,
//			Map<String, FromElement> explicitFetches) {
//		final List<FromElement> fromElements = new ArrayList<FromElement>();
//
//		for ( Object obj : attributeNodes ) {
//			final AttributeNode<?> attributeNode = (AttributeNode<?>) obj;
//
//			final String attributeName = attributeNode.getAttributeName();
//			final String className = origin.getClassName();
//			// TODO: This is ignored by collection types and probably wrong for entity types.  Presumably it screws
//			// with inheritance.
//			final String role = className + "." + attributeName;
//			final String classAlias = origin.getClassAlias();
//			final String originTableAlias = origin.getTableAlias();
//			final Type propertyType = origin.getPropertyType( attributeName, attributeName );
//
//			try {
//				FromElement fromElement = explicitFetches.get( role );
//				boolean explicitFromElement = false;
//				if ( fromElement == null ) {
//					if ( propertyType.getClassification().equals( Type.Classification.ENTITY ) ) {
//						final EntityType entityType = (EntityType) propertyType;
//
//						final String[] columns = origin.toColumns( originTableAlias, attributeName, false );
//						final String tableAlias = walker.getAliasGenerator().createName(
//								entityType.getAssociatedEntityName()
//						);
//
//						final FromElementFactory fromElementFactory = new FromElementFactory(
//								fromClause, origin,
//								attributeName, classAlias, columns, false
//						);
//						final JoinSequence joinSequence = walker.getSessionFactoryHelper().createJoinSequence(
//								false, entityType, tableAlias, JoinType.LEFT_OUTER_JOIN, columns
//						);
//						fromElement = fromElementFactory.createEntityJoin(
//								entityType.getAssociatedEntityName(),
//								tableAlias,
//								joinSequence,
//								true,
//								walker.isInFrom(),
//								entityType,
//								role,
//								null
//						);
//					}
//					else if ( propertyType.getClassification().equals( Type.Classification.COLLECTION ) ) {
//						CollectionType collectionType = (CollectionType) propertyType;
//						final String[] columns = origin.toColumns( originTableAlias, attributeName, false );
//
//						final FromElementFactory fromElementFactory = new FromElementFactory(
//								fromClause, origin,
//								attributeName, classAlias, columns, false
//						);
//						final QueryableCollection queryableCollection = walker.getSessionFactoryHelper()
//								.requireQueryableCollection( collectionType.getRole() );
//						fromElement = fromElementFactory.createCollection(
//								queryableCollection, collectionType.getRole(), JoinType.LEFT_OUTER_JOIN, true, false
//						);
//					}
//				}
//				else {
//					explicitFromElement = true;
//					fromElement.setInProjectionList( true );
//					fromElement.setFetch( true );
//				}
//
//				if ( fromElement != null ) {
//					if( !explicitFromElement ){
//						fromElements.add( fromElement );
//					}
//
//					// recurse into subgraphs
//					for ( Subgraph<?> subgraph : attributeNode.getSubgraphs().values() ) {
//						fromElements.addAll(
//								getFromElements(
//										subgraph.getAttributeNodes(), fromElement,
//										fromClause, walker, explicitFetches
//								)
//						);
//					}
//				}
//			}
//			catch (Exception e) {
//				throw new QueryException( "Could not apply the EntityGraph to the Query!", e );
//			}
//		}
//
//		return fromElements;
//	}
}
