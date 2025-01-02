/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;
import org.hibernate.AssertionFailure;
import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.CannotContainSubGraphException;
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

import jakarta.persistence.metamodel.Attribute;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 *  Base class for {@link RootGraph} and {@link SubGraph} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGraph<J> extends AbstractGraphNode<J> implements GraphImplementor<J> {

	private final ManagedDomainType<J> managedType;
	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> treatedSubgraphs;
	private Map<PersistentAttribute<? super J,?>, AttributeNodeImplementor<?>> attributeNodes;

	public AbstractGraph(ManagedDomainType<J> managedType, boolean mutable) {
		super( mutable );
		this.managedType = managedType;
	}

	protected AbstractGraph(ManagedDomainType<J> managedType, GraphImplementor<J> graph, boolean mutable) {
		super( mutable );
		this.managedType = managedType;
		var attributeNodesByAttribute = graph.getNodes();
		var subGraphMap = graph.getSubGraphs();
		attributeNodes = attributeNodesByAttribute.isEmpty() ? null : new HashMap<>( attributeNodesByAttribute.size() );
		treatedSubgraphs = subGraphMap.isEmpty() ? null : new HashMap<>( subGraphMap.size() );
		mergeInternal( graph );
	}

	protected AbstractGraph(GraphImplementor<J> graph, boolean mutable) {
		this( graph.getGraphedType(), graph, mutable );
	}

	@Override
	public final ManagedDomainType<J> getGraphedType() {
		return managedType;
	}

	@SuppressWarnings("unchecked")
	private <S extends J> SubGraphImplementor<S> getTreatedSubgraph(Class<S> javaType) {
		return treatedSubgraphs == null ? null : (SubGraphImplementor<S>) treatedSubgraphs.get( javaType );
	}

	@SuppressWarnings("unchecked")
	private <T> AttributeNodeImplementor<T> getNode(PersistentAttribute<?, ? extends T> attribute) {
		return attributeNodes == null ? null : (AttributeNodeImplementor<T>) attributeNodes.get( attribute );
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

	private <AJ> AttributeNodeImplementor<AJ> getNodeForPut(PersistentAttribute<?, AJ> attribute) {
		if ( attributeNodes == null ) {
			attributeNodes = new HashMap<>();
			return null;
		}
		else {
			return getNode( attribute );
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
		graph.getSubGraphs().values().forEach( this::mergeGraph );
	}

	private void mergeNode(PersistentAttribute<? super J, ?> attribute, AttributeNodeImplementor<?> node) {
		final AttributeNodeImplementor<?> existingNode = getNodeForPut( attribute );
		if ( existingNode == null ) {
			attributeNodes.put( attribute, node.makeCopy( isMutable() ) );
		}
		else {
			// keep the local one, but merge in the incoming one
			mergeNode( node, existingNode );
		}
	}

	private <T extends J> void mergeGraph(SubGraphImplementor<T> subgraph) {
		final Class<T> javaType = subgraph.getClassType();
		final SubGraphImplementor<T> existing = getTreatedSubgraphForPut( javaType );
		if ( existing == null ) {
			treatedSubgraphs.put( javaType, subgraph.makeCopy( isMutable() ) );
		}
		else {
			// even if immutable, we need to merge here
			existing.mergeInternal( subgraph );
		}
	}

	private static <T> void mergeNode(
			AttributeNodeImplementor<?> node, AttributeNodeImplementor<T> existingNode) {
		if ( existingNode.getAttributeDescriptor() == node.getAttributeDescriptor() ) {
			@SuppressWarnings("unchecked") // safe, we just checked
			final AttributeNodeImplementor<T> castNode = (AttributeNodeImplementor<T>) node;
			existingNode.merge( castNode );
		}
		else {
			throw new AssertionFailure( "Attributes should have been identical" );
		}
	}

	@Override
	public List<AttributeNodeImplementor<?>> getAttributeNodeList() {
		return attributeNodes == null ? emptyList() : new ArrayList<>( attributeNodes.values() );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> findAttributeNode(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = findAttributeInSupertypes( attributeName );
		@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
		final PersistentAttribute<? super J, AJ> persistentAttribute = (PersistentAttribute<? super J, AJ>) attribute;
		final AttributeNodeImplementor<AJ> node = attribute == null ? null : findAttributeNode( persistentAttribute );
		if ( node == null && treatedSubgraphs != null ) {
			for ( SubGraphImplementor<?> subgraph : treatedSubgraphs.values() ) {
				final AttributeNodeImplementor<AJ> subgraphNode = subgraph.findAttributeNode( attributeName );
				if ( subgraphNode != null ) {
					return subgraphNode;
				}
			}
			return null;
		}
		else {
			return node;
		}
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> findAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		return getNode( attribute );
	}

	@Override
	public List<jakarta.persistence.AttributeNode<?>> getAttributeNodes() {
		return attributeNodes == null ? emptyList() : new ArrayList<>( attributeNodes.values() );
	}

	@Override
	public Map<PersistentAttribute<? super J, ?>, AttributeNodeImplementor<?>> getNodes() {
		return attributeNodes == null ? emptyMap() : unmodifiableMap( attributeNodes );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> addAttributeNode(String attributeName) {
		return findOrCreateAttributeNode( attributeName );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> addAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		return findOrCreateAttributeNode( attribute );
	}

	@Override
	public <Y> AttributeNodeImplementor<Y> addAttributeNode(Attribute<? super J, Y> attribute) {
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
		for ( Attribute<? super J, ?> attribute : attributes ) {
			addAttributeNode( attribute );
		}
	}

	@Override
	public void removeAttributeNode(String attributeName) {
		verifyMutability();
		attributeNodes.remove( managedType.findAttribute( attributeName ) );
	}

	@Override
	public void removeAttributeNode(Attribute<? super J, ?> attribute) {
		verifyMutability();
		attributeNodes.remove( (PersistentAttribute<? super J, ?>) attribute );
	}

	@Override
	public void removeAttributeNodes(Attribute.PersistentAttributeType nodeType) {
		verifyMutability();
		attributeNodes.keySet().removeIf( entry -> entry.getPersistentAttributeType() == nodeType );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(PersistentAttribute<? super J, AJ> attribute) {
		verifyMutability();
		final AttributeNodeImplementor<AJ> node = getNodeForPut( attribute );
		if ( node == null ) {
			final AttributeNodeImplementor<AJ> newAttrNode = AttributeNodeImpl.create( attribute, isMutable() );
			attributeNodes.put( attribute, newAttrNode );
			return newAttrNode;
		}
		else {
			return node;
		}
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = getAttribute( attributeName );
		@SuppressWarnings("unchecked") // The JPA API is unsafe by nature
		final PersistentAttribute<? super J, AJ> persistentAttribute = (PersistentAttribute<? super J, AJ>) attribute;
		return findOrCreateAttributeNode( persistentAttribute );
	}

	private PersistentAttribute<? super J, ?> findAttributeInSupertypes(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = managedType.findAttributeInSuperTypes( attributeName );
		return attribute instanceof SqmPathSource<?> sqmPathSource && sqmPathSource.isGeneric()
				? managedType.findConcreteGenericAttribute( attributeName )
				: attribute;
	}

	private PersistentAttribute<? super J, ?> getAttribute(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = managedType.getAttribute( attributeName );
		return attribute instanceof SqmPathSource<?> sqmPathSource && sqmPathSource.isGeneric()
				? managedType.findConcreteGenericAttribute( attributeName )
				: attribute;
	}

	@Override
	@SuppressWarnings("unchecked") // The API is unsafe by nature
	public <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName)  {
		return (SubGraphImplementor<AJ>) findOrCreateAttributeNode( attributeName ).makeSubGraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(String attributeName, Class<AJ> subtype) {
		return findOrCreateAttributeNode( attributeName ).makeSubGraph( subtype );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, AJ> attribute) {
		return findOrCreateAttributeNode( attribute ).makeSubGraph( attribute.getJavaType() );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, Class<AJ> subtype) {
		return findOrCreateAttributeNode( attribute ).makeSubGraph( subtype );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addSubGraph(PersistentAttribute<? super J, ? super AJ> attribute, ManagedDomainType<AJ> subtype) {
		return findOrCreateAttributeNode( attribute ).makeSubGraph( subtype );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addElementSubGraph(PluralPersistentAttribute<? super J, ?, ? super AJ> attribute, Class<AJ> type) {
		return findOrCreateAttributeNode( attribute ).makeSubGraph( type );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addElementSubGraph(PluralPersistentAttribute<? super J, ?, ? super AJ> attribute, ManagedDomainType<AJ> type) {
		return findOrCreateAttributeNode( attribute ).makeSubGraph( type );
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(MapPersistentAttribute<? super J, ? super AJ, ?> attribute, ManagedDomainType<AJ> subtype) {
		return findOrCreateAttributeNode( attribute ).makeKeySubGraph( subtype );
	}

	@Override
	@SuppressWarnings("unchecked") // The API is unsafe by nature
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName) {
		return (SubGraphImplementor<AJ>) findOrCreateAttributeNode( attributeName ).makeKeySubGraph();
	}

	@Override
	public <AJ> SubGraphImplementor<AJ> addKeySubGraph(String attributeName, Class<AJ> subtype) {
		return findOrCreateAttributeNode( attributeName ).makeKeySubGraph( subtype );
	}

	@Override
	public <K> SubGraphImplementor<K> addMapKeySubgraph(MapAttribute<? super J, K, ?> attribute) {
		return findOrCreateAttributeNode( attribute.getName() ).makeKeySubGraph( attribute.getKeyJavaType() );
	}

	@Override
	public <K> SubGraphImplementor<K> addTreatedMapKeySubgraph(
			MapAttribute<? super J, ? super K, ?> attribute,
			Class<K> type) {
		return addMapKeySubgraph( attribute ).addTreatedSubGraph( type );
	}

	@Override @SuppressWarnings("unchecked") // The JPA API is unsafe by nature
	public <X> SubGraphImplementor<X> addElementSubgraph(String attributeName) {
		return (SubGraphImplementor<X>) findOrCreateAttributeNode( attributeName ).makeSubGraph();
	}

	@Override
	public <E> SubGraphImplementor<E> addElementSubgraph(PluralAttribute<? super J, ?, E> attribute) {
		return findOrCreateAttributeNode( attribute.getName() )
				.makeSubGraph( asManagedType( attribute.getElementType() ) );
	}

	@Override
	public <X> SubGraphImplementor<X> addElementSubgraph(String attributeName, Class<X> type) {
		return findOrCreateAttributeNode( attributeName ).makeSubGraph( type );
	}

	@Override
	public <E> SubGraphImplementor<E> addTreatedElementSubgraph(
			PluralAttribute<? super J, ?, ? super E> attribute,
			Class<E> type) {
		return addElementSubgraph( attribute ).addTreatedSubGraph( type );
	}

	@Override
	public <S extends J> SubGraphImplementor<S> addTreatedSubGraph(ManagedDomainType<S> type) {
		verifyMutability();
		if ( getGraphedType().equals( type ) ) {
			//noinspection unchecked
			return (SubGraphImplementor<S>) this;
		}
		else {
			final Class<S> javaType = type.getJavaType();
			final SubGraphImplementor<S> castSubgraph = getTreatedSubgraphForPut( javaType );
			if ( castSubgraph == null ) {
				final SubGraphImpl<S> subgraph = new SubGraphImpl<>( type, true );
				treatedSubgraphs.put( javaType, subgraph );
				return subgraph;
			}
			else {
				return castSubgraph;
			}
		}
	}

	@Override
	public <S extends J> SubGraphImplementor<S> addTreatedSubGraph(Class<S> type) {
		return addTreatedSubGraph( getGraphedType().getMetamodel().managedType( type ) );
	}

	@Override
	public Map<Class<? extends J>, SubGraphImplementor<? extends J>> getSubGraphs() {
		return treatedSubgraphs == null ? emptyMap() : unmodifiableMap( treatedSubgraphs );
	}

	static <T> ManagedDomainType<T> asManagedType(Type<T> domainType) {
		if ( domainType instanceof ManagedDomainType<T> managedDomainType ) {
			return managedDomainType;
		}
		else {
			throw new CannotContainSubGraphException( "Type '" + domainType.getJavaType().getName()
														+ "' is not a managed type" );
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder( "Graph[" ).append( managedType.getTypeName() );
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
