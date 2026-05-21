/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import org.hibernate.AssertionFailure;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 *  Base class for {@link RootGraph} and {@link SubGraph} implementations.
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public abstract class GraphImpl<J> extends AbstractGraphNode<J> implements GraphImplementor<J> {

	private final ManagedDomainType<J> managedType;
	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> treatedSubgraphs;
	private Map<PersistentAttribute<? super J,?>, AttributeNodeImplementor<?,?,?>> attributeNodes;

	public GraphImpl(ManagedDomainType<J> managedType, boolean mutable) {
		super( mutable );
		this.managedType = managedType;
	}

	protected GraphImpl(ManagedDomainType<J> managedType, GraphImplementor<J> graph, boolean mutable) {
		super( mutable );
		this.managedType = managedType;
		var attributeNodesByAttribute = graph.getNodes();
		var subGraphMap = graph.getTreatedSubgraphs();
		attributeNodes = attributeNodesByAttribute.isEmpty() ? null : new HashMap<>( attributeNodesByAttribute.size() );
		treatedSubgraphs = subGraphMap.isEmpty() ? null : new HashMap<>( subGraphMap.size() );
		mergeInternal( graph );
	}

	protected GraphImpl(GraphImplementor<J> graph, boolean mutable) {
		this( graph.getGraphedType(), graph, mutable );
	}

	@Override
	public final ManagedDomainType<J> getGraphedType() {
		return managedType;
	}
	@Override
	public List<jakarta.persistence.AttributeNode<?>> getAttributeNodes() {
		//noinspection unchecked,rawtypes
		return (List) getAttributeNodeList();
	}


	@Override
	public List<AttributeNodeImplementor<?,?,?>> getAttributeNodeList() {
		if ( attributeNodes == null ) {
			return emptyList();
		}
		else {
			// we need to filter out removed nodes
			return attributeNodes.values().stream().filter( (node) -> !node.isRemoved() ).toList();
		}
	}

	@Override
	public Map<PersistentAttribute<? super J, ?>, AttributeNodeImplementor<?,?,?>> getNodes() {
		if ( attributeNodes == null ) {
			return emptyMap();
		}
		else {
			return attributeNodes
					.entrySet()
					.stream()
					.filter( (entry) -> !entry.getValue().isRemoved() )
					.collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
		}
	}

	@Override
	public <Y> AttributeNodeImplementor<Y,?,?> getAttributeNode(String attributeName) {
		@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
		final var node = (AttributeNodeImplementor<Y,?,?>) findNode( attributeName );
		return node == null || node.isRemoved() ? null : node;
	}

	@Override
	public <Y> AttributeNodeImplementor<Y,?,?> getAttributeNode(Attribute<? super J, Y> attribute) {
		return getActiveNode( (PersistentAttribute<?, Y>) attribute );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ,?,?> addAttributeNode(String attributeName) {
		final var node = findOrCreateAttributeNode( attributeName );
		node.markRemoved( false );
		@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
		final var castNode = (AttributeNodeImplementor<AJ, ?, ?>) node;
		return castNode;
	}

	@Override
	public <Y> AttributeNodeImplementor<Y,?,?> addAttributeNode(Attribute<? super J, Y> attribute) {
		final var node = findOrCreateAttributeNode( (PersistentAttribute<? super J, Y>) attribute );
		node.markRemoved( false );
		return node;
	}

	@Override
	public void addAttributeNodes(String... attributeNames) {
		for ( String attributeName : attributeNames ) {
			addAttributeNode( attributeName );
		}
	}

	@Override @SafeVarargs
	public final void addAttributeNodes(Attribute<? super J, ?>... attributes) {
		for ( var attribute : attributes ) {
			addAttributeNode( attribute );
		}
	}

	@Override
	public void removeAttributeNode(String attributeName) {
		verifyMutability();
		removeAttributeNode( managedType.findAttribute( attributeName ) );
	}

	@Override
	public void removeAttributeNode(Attribute<? super J, ?> attribute) {
		verifyMutability();
		findOrCreateAttributeNode( (PersistentAttribute<? super J, ?>) attribute ).markRemoved( true );
	}

	@Override
	public void removeAttributeNodes(Attribute.PersistentAttributeType nodeType) {
		verifyMutability();
		for ( var typeAttribute : managedType.getAttributes() ) {
			if ( typeAttribute.getPersistentAttributeType() == nodeType ) {
				findOrCreateAttributeNode( (PersistentAttribute<? super J, ?>) typeAttribute )
						.markRemoved( true );
			}
		}
	}

	private <T> AttributeNodeImplementor<T,?,?> findOrCreateAttributeNode(PersistentAttribute<? super J, T> attribute) {
		verifyMutability();
		final var node = getNodeForPut( attribute );
		if ( node == null ) {
			final var newAttrNode = AttributeNodeImpl.create( attribute, isMutable() );
			attributeNodes.put( attribute, newAttrNode );
			return newAttrNode;
		}
		else {
			return node;
		}
	}

	private <C,E> AttributeNodeImplementor<C,E,?> findOrCreateAttributeNode(PluralPersistentAttribute<? super J, C, E> attribute) {
		verifyMutability();
		final var node = getPluralNodeForPut( attribute );
		if ( node == null ) {
			final var newAttrNode = AttributeNodeImpl.create( attribute, isMutable() );
			attributeNodes.put( attribute, newAttrNode );
			return newAttrNode;
		}
		else {
			return node;
		}
	}

	private <K,V> AttributeNodeImplementor<Map<K,V>,V,K> findOrCreateAttributeNode(MapPersistentAttribute<? super J, K, V> attribute) {
		verifyMutability();
		final var node = getMapNodeForPut( attribute );
		if ( node == null ) {
			final var newAttrNode = AttributeNodeImpl.create( attribute, isMutable() );
			attributeNodes.put( attribute, newAttrNode );
			return newAttrNode;
		}
		else {
			return node;
		}
	}

	@Override
	public AttributeNodeImplementor<?,?,?> findOrCreateAttributeNode(String attributeName) {
		final var attribute = getAttribute( attributeName );
		final var persistentAttribute = (PersistentAttribute<? super J, ?>) attribute;
		return findOrCreateAttributeNode( persistentAttribute );
	}

	private PersistentAttribute<? super J, ?> findAttributeInSupertypes(String attributeName) {
		final var attribute = managedType.findAttributeInSuperTypes( attributeName );
		return attribute instanceof SqmPathSource<?> sqmPathSource && sqmPathSource.isGeneric()
				? managedType.findConcreteGenericAttribute( attributeName )
				: attribute;
	}

	private PersistentAttribute<? super J, ?> getAttribute(String attributeName) {
		final var attribute = managedType.getAttribute( attributeName );
		return attribute instanceof SqmPathSource<?> sqmPathSource && sqmPathSource.isGeneric()
				? managedType.findConcreteGenericAttribute( attributeName )
				: attribute;
	}

	@Override
	public AttributeNodeImplementor<?,?,?> findNode(String attributeName) {
		final var attribute = findAttributeInSupertypes( attributeName );
		if ( attribute != null ) {
			final var node = getExistingNode( attribute );
			if ( node != null ) {
				return node;
			}
		}

		if ( treatedSubgraphs != null ) {
			for ( var subgraph : treatedSubgraphs.values() ) {
				final var subgraphNode = subgraph.getExistingNode( attributeName );
				if ( subgraphNode != null ) {
					return subgraphNode;
				}
			}
		}

		return null;
	}

	@Override
	public AttributeNodeImplementor<?,?,?> getExistingNode(PersistentAttribute<?, ?> attribute) {
		return attributeNodes == null ? null : attributeNodes.get( attribute );
	}

	@Override
	public AttributeNodeImplementor<?,?,?> getExistingNode(String attributeName) {
		return attributeNodes == null ? null : attributeNodes.get( managedType.getAttribute( attributeName ) );
	}

	private <T> AttributeNodeImplementor<T,?,?> getActiveNode(PersistentAttribute<?, T> attribute) {
		if ( attributeNodes == null ) {
			return null;
		}
		final var node = attributeNodes.get( attribute );
		if ( node == null || node.isRemoved() ) {
			return null;
		}
		@SuppressWarnings("unchecked")
		final var castNode = (AttributeNodeImplementor<T, ?, ?>) node;
		return castNode;
	}

	private <T, E> AttributeNodeImplementor<T,E,?> getActiveNode(PluralPersistentAttribute<?, T, E> attribute) {
		final var node = getActiveNode( (PersistentAttribute<?, ? extends T>) attribute );
		@SuppressWarnings("unchecked")
		final var castNode = (AttributeNodeImplementor<T, E, ?>) node;
		return castNode;
	}

	private <K, V> AttributeNodeImplementor<Map<K,V>, V, K> getActiveNode(MapPersistentAttribute<?, K, V> attribute) {
		final var node = getActiveNode( (PersistentAttribute<?, Map<K,V>>) attribute );
		@SuppressWarnings("unchecked")
		final var castNode = (AttributeNodeImplementor<Map<K, V>, V, K>) node;
		return castNode;
	}

	private <T,AJ> AttributeNodeImplementor<AJ,?,?> getNodeForPut(PersistentAttribute<T, AJ> attribute) {
		if ( attributeNodes == null ) {
			attributeNodes = new HashMap<>();
			return null;
		}
		else {
			return getActiveNode( attribute );
		}
	}

	private <C, E> AttributeNodeImplementor<C,E,?> getPluralNodeForPut(PluralPersistentAttribute<?, C, E> attribute) {
		if ( attributeNodes == null ) {
			attributeNodes = new HashMap<>();
			return null;
		}
		else {
			return getActiveNode( attribute );
		}
	}

	private <V, K> AttributeNodeImplementor<Map<K,V>, V, K> getMapNodeForPut(MapPersistentAttribute<?, K, V> attribute) {
		if ( attributeNodes == null ) {
			attributeNodes = new HashMap<>();
			return null;
		}
		else {
			return getActiveNode( attribute );
		}
	}

	@SuppressWarnings("unchecked")
	private <S extends J> SubGraphImplementor<S> getTreatedSubgraph(Class<S> javaType) {
		return treatedSubgraphs == null ? null : (SubGraphImplementor<S>) treatedSubgraphs.get( javaType );
	}

	private <S extends J> SubGraphImplementor<S> getTreatedSubgraphForPut(Class<S> javaType) {
		if ( treatedSubgraphs == null ) {
			treatedSubgraphs = new HashMap<>(1);
			return null;
		}
		else {
			return getTreatedSubgraph( javaType );
		}
	}

	@Override
	public void merge(GraphImplementor<J> graph) {
		if ( graph != null ) {
			verifyMutability();
			mergeInternal( graph );
		}
	}

	@Override
	public void mergeInternal(GraphImplementor<J> graph) {
		// skip verifyMutability()
		graph.getNodes().forEach( this::mergeNode );
		graph.getTreatedSubgraphs().values().forEach( this::mergeGraph );
	}

	private void mergeNode(PersistentAttribute<? super J, ?> attribute, AttributeNodeImplementor<?,?,?> node) {
		final var existingNode = getNodeForPut( attribute );
		if ( existingNode == null ) {
			attributeNodes.put( attribute, node.makeCopy( isMutable() ) );
		}
		else {
			// keep the local one but merge in the incoming one
			mergeNode( node, existingNode );
		}
	}

	private <T extends J> void mergeGraph(SubGraphImplementor<T> subgraph) {
		final var javaType = subgraph.getClassType();
		final var existing = getTreatedSubgraphForPut( javaType );
		if ( existing == null ) {
			treatedSubgraphs.put( javaType, subgraph.makeCopy( isMutable() ) );
		}
		else {
			// even if immutable, we need to merge here
			existing.mergeInternal( subgraph );
		}
	}

	private static <T,E,K> void mergeNode(
			AttributeNodeImplementor<?,?,?> node, AttributeNodeImplementor<T,E,K> existingNode) {
		if ( existingNode.getAttributeDescriptor() == node.getAttributeDescriptor() ) {
			@SuppressWarnings("unchecked") // safe, we just checked
			final var castNode = (AttributeNodeImplementor<T,E,K>) node;
			existingNode.merge( castNode );
		}
		else {
			throw new AssertionFailure( "Attributes should have been identical" );
		}
	}



	@Override
	public <X> SubGraphImplementor<X> addSubgraph(String attributeName) {
		final var valueSubgraph = findOrCreateAttributeNode( attributeName ).addValueSubgraph();
		@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
		final var castSubgraph = (SubGraphImplementor<X>) valueSubgraph;
		return castSubgraph;
	}

	@Override
	public <X> SubGraphImplementor<X> addSubgraph(String attributeName, Class<X> type) {
		return addSubgraph( attributeName ).addTreatedSubgraph( type );
	}

	@Override
	public <X> SubGraphImplementor<X> addSubgraph(Attribute<? super J, X> attribute) {
		return findOrCreateAttributeNode( (PersistentAttribute<? super J, X>) attribute )
				.addSingularSubgraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addTreatedSubgraph(Attribute<? super J, ? super AJ> attribute, ManagedType<AJ> type) {
		return addSubgraph( attribute ).addTreatedSubgraph( type );
	}

	@Override
	public <X> SubGraphImplementor<X> addElementSubgraph(String attributeName) {
		final var elementSubgraph = findOrCreateAttributeNode( attributeName ).addElementSubgraph();
		@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
		final var castSubgraph = (SubGraphImplementor<X>) elementSubgraph;
		return castSubgraph;
	}

	@Override
	public <X> SubGraphImplementor<X> addElementSubgraph(String attributeName, Class<X> type) {
		return addElementSubgraph( attributeName ).addTreatedSubgraph( type );
	}

	@Override
	public <E> SubGraphImplementor<E> addElementSubgraph(PluralAttribute<? super J, ?, E> attribute) {
		return findOrCreateAttributeNode( (PluralPersistentAttribute<? super J, ?, E>) attribute )
				.addElementSubgraph();
	}

	@Override
	public <E> SubGraphImplementor<E> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super E> attribute, Class<E> type) {
		return addElementSubgraph( attribute ).addTreatedSubgraph( type );
	}

	@Override
	public <AJ> SubGraph<AJ> addTreatedElementSubgraph(PluralAttribute<? super J, ?, ? super AJ> attribute, ManagedType<AJ> type) {
		return addElementSubgraph( attribute ).addTreatedSubgraph( type );
	}

	@Override
	public <X> SubGraphImplementor<X> addKeySubgraph(String attributeName) {
		final var keySubgraph = findOrCreateAttributeNode( attributeName ).addKeySubgraph();
		@SuppressWarnings("unchecked") // The API is unsafe by nature
		final var castSubgraph = (SubGraphImplementor<X>) keySubgraph;
		return castSubgraph;
	}

	@Override
	public <X> SubGraphImplementor<X> addKeySubgraph(String attributeName, Class<X> type) {
		return addKeySubgraph( attributeName ).addTreatedSubgraph( type );
	}

	@Override
	public <K> SubGraphImplementor<K> addMapKeySubgraph(MapAttribute<? super J, K, ?> attribute) {
		return findOrCreateAttributeNode( (MapPersistentAttribute<? super J, K, ?>) attribute )
				.addKeySubgraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addTreatedMapKeySubgraph(MapAttribute<? super J, ? super AJ, ?> attribute, ManagedType<AJ> type) {
		return addMapKeySubgraph( attribute ).addTreatedSubgraph( type );
	}

	@Override
	public <K> SubGraphImplementor<K> addTreatedMapKeySubgraph(
			MapAttribute<? super J, ? super K, ?> attribute,
			Class<K> type) {
		return addMapKeySubgraph( attribute ).addTreatedSubgraph( type );
	}

	@Override
	public <Y> SubGraphImplementor<Y> addTreatedSubgraph(Attribute<? super J, ? super Y> attribute, Class<Y> type) {
		return addSubgraph( attribute ).addTreatedSubgraph( type );
	}

	@Override
	public <S extends J> SubGraphImplementor<S> addTreatedSubgraph(ManagedType<S> type) {
		verifyMutability();
		if ( getGraphedType().equals( type ) ) {
			//noinspection unchecked
			return (SubGraphImplementor<S>) this;
		}
		else {
			final var javaType = type.getJavaType();
			final var castSubgraph = getTreatedSubgraphForPut( javaType );
			if ( castSubgraph == null ) {
				final var subgraph = new SubGraphImpl<>( (ManagedDomainType<S>) type, true );
				treatedSubgraphs.put( javaType, subgraph );
				return subgraph;
			}
			else {
				return castSubgraph;
			}
		}
	}

	@Override
	public <S extends J> SubGraphImplementor<S> addTreatedSubgraph(Class<S> type) {
		return addTreatedSubgraph( getGraphedType().getMetamodel().managedType( type ) );
	}

	@Override
	public Map<Class<? extends J>, SubGraphImplementor<? extends J>> getTreatedSubgraphs() {
		return treatedSubgraphs == null ? emptyMap() : unmodifiableMap( treatedSubgraphs );
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder( "Graph[" ).append( managedType.getTypeName() );
		if ( attributeNodes != null ) {
			builder.append( ", nodes=" )
					.append( attributeNodes.values().stream()
							.map( node -> node.getAttributeDescriptor().getName() ).toList() );
		}
		if ( treatedSubgraphs != null ) {
			builder.append( ", subgraphs=" )
					.append( treatedSubgraphs.values().stream()
							.map( subgraph -> subgraph.getGraphedType().getTypeName() ).toList() );
		}
		return builder.append( ']' ).toString();
	}
}
