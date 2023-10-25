/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import antlr.SemanticException;
import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.FromClause;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.FromElementFactory;
import org.hibernate.hql.internal.ast.tree.ImpliedFromElement;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.sql.JoinType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Encapsulates a JPA EntityGraph provided through a JPQL query hint.  Converts the fetches into a list of AST
 * FromElements.  The logic is kept here as much as possible in order to make it easy to remove this in the future,
 * once our AST is improved and this "hack" is no longer needed.
 *
 * @author Brett Meyer
 */
public class EntityGraphQueryHint implements AppliedGraph {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EntityGraphQueryHint.class );

	private final RootGraphImplementor<?> graph;
	private final GraphSemantic semantic;

	public EntityGraphQueryHint(String hintName, EntityGraph<?> graph) {
		assert hintName != null;

		this.semantic = GraphSemantic.fromJpaHintName( hintName );
		this.graph = (RootGraphImplementor<?>) graph;
	}

	public EntityGraphQueryHint(RootGraphImplementor<?> graph, GraphSemantic semantic	) {
		this.semantic = semantic;
		this.graph = graph;
	}

	@Override
	public GraphSemantic getSemantic() {
		return semantic;
	}

	@Override
	public RootGraphImplementor<?> getGraph() {
		return graph;
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

		boolean applyEntityGraph = false;
		if ( fromClause.getLevel() == FromClause.ROOT_LEVEL ) {
			final String fromElementEntityName = fromClause.getFromElement().getEntityPersister().getEntityName();
			applyEntityGraph = graph.appliesTo( fromElementEntityName );
			if ( !applyEntityGraph ) {
				LOG.warnf( "Entity graph is not applicable to the root entity [%s]; Ignored.", fromElementEntityName );
			}
		}

		return getFromElements(
				applyEntityGraph ? graph.getAttributeNodes() : Collections.emptyList(),
				fromClause.getFromElement(),
				Collections.emptyList(),
				fromClause,
				walker,
				explicitFetches
		);
	}

	private List<FromElement> getFromElements(
			List attributeNodes,
			FromElement origin,
			List<PersistentAttributeDescriptor<?, ?>> parentEmbeddedNodeAttributeDescriptors,
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
			String embeddedPrefix = parentEmbeddedNodeAttributeDescriptors.stream()
					.map( descriptor -> descriptor.getName() + "." )
					.collect( Collectors.joining());
			final String role = className + "." + embeddedPrefix + attributeName;
			final String classAlias = origin.getClassAlias();
			final String originTableAlias = origin.getTableAlias();
			final Type propertyType = parentEmbeddedNodeAttributeDescriptors.isEmpty() ?
					origin.getPropertyType( attributeName, attributeName ) :
					getEmbeddedTypeFieldType( parentEmbeddedNodeAttributeDescriptors, attributeName );

			try {
				FromElement fromElement = explicitFetches.get( role );
				boolean explicitFromElement = false;
				if ( fromElement == null ) {
					if ( propertyType.isEntityType() ) {
						final EntityType entityType = (EntityType) propertyType;

						final String[] columns = origin.toColumns( originTableAlias, embeddedPrefix + attributeName, false );
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
						final String[] columns = origin.toColumns( originTableAlias, embeddedPrefix + attributeName, false );

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

				List<PersistentAttributeDescriptor<?, ?>> currentEmbeddedNodeAttributeDescriptors
						= Collections.emptyList();
				if ( fromElement != null ) {
					if( !explicitFromElement ){
						fromElements.add( fromElement );
					}
				} else {
					if ( attributeNode instanceof AttributeNodeImplementor ) {
						AttributeNodeImplementor<?> attributeNodeImplementor = (AttributeNodeImplementor<?>) attributeNode;
						PersistentAttributeDescriptor<?, ?> embeddedNodeAttributeDescriptor
								= attributeNodeImplementor.getAttributeDescriptor();
						if ( embeddedNodeAttributeDescriptor.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED ) {
							// embedded type, descend into it keeping the origin as from
							fromElement = origin;

							if (parentEmbeddedNodeAttributeDescriptors.isEmpty()) {
								currentEmbeddedNodeAttributeDescriptors = Collections.singletonList( embeddedNodeAttributeDescriptor );
							} else {
								currentEmbeddedNodeAttributeDescriptors = new ArrayList<>( parentEmbeddedNodeAttributeDescriptors );
								currentEmbeddedNodeAttributeDescriptors.add( embeddedNodeAttributeDescriptor );
							}
						}
					}
				}

				if ( fromElement != null ) {
					// recurse into subgraphs
					for ( Subgraph<?> subgraph : attributeNode.getSubgraphs().values() ) {
						fromElements.addAll(
								getFromElements(
										subgraph.getAttributeNodes(), fromElement,
										currentEmbeddedNodeAttributeDescriptors,
										fromClause, walker, explicitFetches
								)
						);
					}
				}
			}
			catch (SemanticException e) {
				throw new QueryException( "Could not apply the EntityGraph to the Query!", e );
			}
		}

		return fromElements;
	}

	private Type getEmbeddedTypeFieldType(List<PersistentAttributeDescriptor<?, ?>> parentEmbeddedNodeAttributeDescriptors, String attributeName) {
		PersistentAttributeDescriptor<?, ?> parentEmbeddedNodeAttributeDescriptor
				= parentEmbeddedNodeAttributeDescriptors.get( parentEmbeddedNodeAttributeDescriptors.size() - 1 );
		CompositeType compositeType = ((EmbeddedTypeDescriptor<?>) parentEmbeddedNodeAttributeDescriptor.getValueGraphType()).getHibernateType();
		String[] propertyNames = compositeType.getPropertyNames();
		for ( int i = 0; i < propertyNames.length; i++ ) {
			if ( attributeName.equals( propertyNames[i] ) ) {
				return compositeType.getSubtypes()[i];
			}
		}
		throw new IllegalStateException( "Cannot find attribute " + attributeName + " in " + parentEmbeddedNodeAttributeDescriptor );
	}

	/**
	 * @deprecated (5.4) Use {@link #getGraph}
	 */
	@Deprecated
	public EntityGraph<?> getOriginEntityGraph() {
		return getGraph();
	}

	/**
	 * @deprecated (5.4) Use {@link #getSemantic}
	 */
	@Deprecated
	public GraphSemantic getGraphSemantic() {
		return getSemantic();
	}

	/**
	 * @deprecated (5.4) Use {@link #getSemantic}
	 */
	@Deprecated
	public String getHintName() {
		return getGraphSemantic().getJpaHintName();
	}
}
