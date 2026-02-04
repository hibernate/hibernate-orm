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
import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
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
	public <AJ> AttributeNodeImplementor<AJ,?,?> findAttributeNode(String attributeName) {
		final AttributeNodeImplementor<AJ,?,?> node = findNode( attributeName );
		return node == null || node.isRemoved() ? null : node;
	}

	@Override
	public <Y> AttributeNodeImplementor<Y,?,?> getAttributeNode(String attributeName) {
		return findAttributeNode( attributeName );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ,?,?> findAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		return getActiveNode( attribute );
	}

	@Override
	public <Y> AttributeNodeImplementor<Y,?,?> getAttributeNode(Attribute<? super J, Y> attribute) {
		return getActiveNode( (PersistentAttribute<?, ? extends Y>) attribute );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ,?,?> addAttributeNode(String attributeName) {
		final AttributeNodeImplementor<AJ, ?, ?> node = findOrCreateAttributeNode( attributeName );
		node.markRemoved( false );
		return node;
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ,?,?> addAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		final AttributeNodeImplementor<AJ, ?, ?> node = findOrCreateAttributeNode( attribute );
		node.markRemoved( false );
		return node;
	}

	@Override
	public <Y> AttributeNodeImplementor<Y,?,?> addAttributeNode(Attribute<? super J, Y> attribute) {
		return addAttributeNode( (PersistentAttribute<? super J, Y>) attribute  );
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
		for ( Attribute<? super J, ?> typeAttribute : managedType.getAttributes() ) {
			if ( typeAttribute.getPersistentAttributeType() != nodeType ) {
				continue;
			}

			findOrCreateAttributeNode( (PersistentAttribute<? super J, ?>) typeAttribute ).markRemoved( true );
		}
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ,?,?> findOrCreateAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
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

	private <K,V> AttributeNodeImplementor<Map<K,V>,V,K> findOrCreateAttributeNode(MapPersistentAttribute<? super J, K, V> attribute) {
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

	@Override
	public <AJ> AttributeNodeImplementor<AJ,?,?> findOrCreateAttributeNode(String attributeName) {
		final var attribute = getAttribute( attributeName );
		@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
		final var persistentAttribute = (PersistentAttribute<? super J, AJ>) attribute;
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
	public <T> AttributeNodeImplementor<T,?,?> findNode(String attributeName) {
		//noinspection unchecked
		final var attribute = (PersistentAttribute<? super J,T>) findAttributeInSupertypes( attributeName );
		if ( attribute != null ) {
			final var node = getExistingNode( attribute );
			if ( node != null ) {
				return node;
			}
		}

		if ( treatedSubgraphs != null ) {
			for ( var subgraph : treatedSubgraphs.values() ) {
				final AttributeNodeImplementor<T,?,?> subgraphNode = subgraph.getExistingNode( attributeName );
				if ( subgraphNode != null ) {
					return subgraphNode;
				}
			}
		}

		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> AttributeNodeImplementor<T,?,?> getExistingNode(PersistentAttribute<?, ? extends T> attribute) {
		if ( attributeNodes == null ) {
			return null;
		}
		return (AttributeNodeImplementor<T,?,?>) attributeNodes.get( attribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> AttributeNodeImplementor<T,?,?> getExistingNode(String attributeName) {
		if ( attributeNodes == null ) {
			return null;
		}
		return (AttributeNodeImplementor<T,?,?>) attributeNodes.get( managedType.getAttribute( attributeName ) );
	}

	@SuppressWarnings("unchecked")
	private <T> AttributeNodeImplementor<T,?,?> getActiveNode(PersistentAttribute<?, ? extends T> attribute) {
		if ( attributeNodes == null ) {
			return null;
		}
		final AttributeNodeImplementor<?, ?, ?> node = attributeNodes.get( attribute );
		if ( node == null || node.isRemoved() ) {
			return null;
		}
		return (AttributeNodeImplementor<T,?,?>) node;
	}

	@SuppressWarnings("unchecked")
	private <T, E> AttributeNodeImplementor<T,E,?> getActiveNode(PluralPersistentAttribute<?, T, E> attribute) {
		return (AttributeNodeImplementor<T, E, ?>) getActiveNode( (PersistentAttribute<?,? extends T>) attribute );
	}

	@SuppressWarnings("unchecked")
	private <K, V> AttributeNodeImplementor<Map<K,V>, V, K> getActiveNode(MapPersistentAttribute<?, K, V> attribute) {
		return (AttributeNodeImplementor<Map<K, V>, V, K>) getActiveNode( (PersistentAttribute<?,? extends V>) attribute );
	}

	private <AJ> AttributeNodeImplementor<AJ,?,?> getNodeForPut(PersistentAttribute<?, AJ> attribute) {
		if ( attributeNodes == null ) {
			attributeNodes = new HashMap<>();
			return null;
		}
		else {
			return getActiveNode( attribute );
		}
	}

	private <C, E> AttributeNodeImplementor<C,E,?> getNodeForPut(PluralPersistentAttribute<?, C, E> attribute) {
		if ( attributeNodes == null ) {
			attributeNodes = new HashMap<>();
			return null;
		}
		else {
			return getActiveNode( attribute );
		}
	}

	private <V, K> AttributeNodeImplementor<Map<K,V>, V, K> getNodeForPut(MapPersistentAttribute<?, K, V> attribute) {
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



	@Override @Deprecated(forRemoval = true)
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		if ( getGraphedType() instanceof EntityDomainType ) {
			return new RootGraphImpl<>( name, this, mutable);
		}
		else {
			throw new CannotBecomeEntityGraphException( "Graph cannot be a root graph because '"
														+ getGraphedType() + "' is not an entity type" );
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



	@Override @SuppressWarnings("unchecked") // The JPA API is unsafe by nature
	public <X> SubGraphImplementor<X> addSubgraph(String attributeName) {
		return (SubGraphImplementor<X>) findOrCreateAttributeNode( attributeName ).addValueSubgraph();
	}

	@Override
	public <X> SubGraphImplementor<X> addSubgraph(String attributeName, Class<X> type) {
		return addSubgraph( attributeName ).addTreatedSubgraph( type );
	}

	@Override
	public <X> SubGraphImplementor<X> addSubgraph(Attribute<? super J, X> attribute) {
		return findOrCreateAttributeNode( (PersistentAttribute<? super J, X>) attribute ).addSingularSubgraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName)  {
		return addSubgraph( attributeName );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName, Class<AJ> subtype) {
		return addSubGraph( attributeName ).addTreatedSubgraph( subtype );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute) {
		return addSubgraph( attribute );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, Class<AJ> subtype) {
		return addTreatedSubgraph( attribute, subtype );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addTreatedSubgraph(Attribute<? super J, ? super AJ> attribute, ManagedType<AJ> type) {
		return addSubgraph( attribute ).addTreatedSubgraph( type );
	}

	@Override @SuppressWarnings("unchecked") // The JPA API is unsafe by nature
	public <X> SubGraphImplementor<X> addElementSubgraph(String attributeName) {
		return (SubGraphImplementor<X>) findOrCreateAttributeNode( attributeName ).addElementSubgraph();
	}

	@Override
	public <X> SubGraphImplementor<X> addElementSubgraph(String attributeName, Class<X> type) {
		return addElementSubgraph( attributeName ).addTreatedSubgraph( type );
	}

	@Override
	public <E> SubGraphImplementor<E> addElementSubgraph(PluralAttribute<? super J, ?, E> attribute) {
		return findOrCreateAttributeNode( (PluralPersistentAttribute<? super J, ?, E>) attribute ).addElementSubgraph();
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
	@SuppressWarnings("unchecked") // The API is unsafe by nature
	public <X> SubGraphImplementor<X> addKeySubgraph(String attributeName) {
		return (SubGraphImplementor<X>) findOrCreateAttributeNode( attributeName ).addKeySubgraph();
	}

	@Override
	public <X> SubGraphImplementor<X> addKeySubgraph(String attributeName, Class<X> type) {
		return addKeySubgraph( attributeName ).addTreatedSubgraph( type );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName) {
		return addKeySubgraph( attributeName );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName, Class<AJ> subtype) {
		return addKeySubGraph( attributeName ).addTreatedSubgraph( subtype );
	}

	@Override
	public <K> SubGraphImplementor<K> addMapKeySubgraph(MapAttribute<? super J, K, ?> attribute) {
		return findOrCreateAttributeNode( (MapPersistentAttribute<? super J, K, ?>) attribute ).addKeySubgraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, Class<AJ> subtype) {
		return addTreatedMapKeySubgraph( attribute, subtype );
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
