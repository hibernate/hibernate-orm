/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;

import org.hibernate.metamodel.model.domain.internal.DomainModelHelper;
import org.jboss.logging.Logger;

import static java.util.Collections.emptyMap;
import static org.hibernate.metamodel.model.domain.internal.DomainModelHelper.findSubType;

/**
 * Implementation of {@link jakarta.persistence.AttributeNode}.
 *
 * @author Steve Ebersole
 */
public class AttributeNodeImpl<J>
		extends AbstractGraphNode<J>
		implements AttributeNodeImplementor<J> {
	private final PersistentAttribute<?, J> attribute;

	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> subgraphMap;
	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> keySubgraphMap;

	public <X> AttributeNodeImpl(PersistentAttribute<X, J> attribute, boolean mutable) {
		this(attribute, null, null, mutable);
	}

	/**
	 * Intended only for use from making a copy
	 */
	private AttributeNodeImpl(
			PersistentAttribute<?, J> attribute,
			Map<Class<? extends J>, SubGraphImplementor<? extends J>> subgraphMap,
			Map<Class<? extends J>, SubGraphImplementor<? extends J>> keySubgraphMap,
			boolean mutable) {
		super( mutable );
		this.attribute = attribute;
		this.subgraphMap = subgraphMap;
		this.keySubgraphMap = keySubgraphMap;
	}

	@Override
	public String getAttributeName() {
		return getAttributeDescriptor().getName();
	}

	@Override
	public PersistentAttribute<?, J> getAttributeDescriptor() {
		return attribute;
	}

	@Override
	public Map<Class<? extends J>, SubGraphImplementor<? extends J>> getSubGraphMap() {
		return subgraphMap == null ? emptyMap() : subgraphMap;
	}

	@Override
	public Map<Class<? extends J>, SubGraphImplementor<? extends J>> getKeySubGraphMap() {
		return keySubgraphMap == null ? emptyMap() : keySubgraphMap;
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph() {
		return internalMakeSubgraph( (Class<J>) null );
	}

	@Override
	public <S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subtype) {
		return internalMakeSubgraph( subtype );
	}

	@Override
	public <S extends J> SubGraphImplementor<S> makeSubGraph(ManagedDomainType<S> subtype) {
		return internalMakeSubgraph( subtype );
	}

	private <S extends J> SubGraphImplementor<S> internalMakeSubgraph(ManagedDomainType<S> type) {
		assert type != null;
		log.debugf( "Making subgraph : ( (%s) %s )", type.getTypeName(), getAttributeName() );
		final SubGraphImplementor<S> subGraph = DomainModelHelper.makeSubGraph( type, type.getBindableJavaType() );
		internalAddSubGraph( subGraph );
		return subGraph;
	}

	@SuppressWarnings("unchecked")
	private <T extends J> ManagedDomainType<T> valueGraphTypeAsManaged() {
		final DomainType<?> valueGraphType = getAttributeDescriptor().getValueGraphType();
		if ( valueGraphType instanceof ManagedDomainType ) {
			return (ManagedDomainType<T>) valueGraphType;
		}
		else {
			throw new CannotContainSubGraphException(
					String.format(
							Locale.ROOT,
							"Attribute [%s] (%s) cannot contain value subgraphs",
							getAttributeName(),
							getAttributeDescriptor().getPersistentAttributeType().name()
					)
			);
		}
	}

	private static final Logger log = Logger.getLogger( AttributeNodeImpl.class );

	private <S extends J> SubGraphImplementor<S> internalMakeSubgraph(Class<S> subType) {
		verifyMutability();
		final ManagedDomainType<S> managedType = valueGraphTypeAsManaged();
		return internalMakeSubgraph( findSubType( managedType, subType == null ? managedType.getJavaType() : subType ) );
	}

	protected void internalAddSubGraph(SubGraphImplementor<? extends J> subGraph) {
		log.tracef( "Adding subgraph : ( (%s) %s )", subGraph.getGraphedType().getTypeName(), getAttributeName() );
		if ( subgraphMap == null ) {
			subgraphMap = new HashMap<>();
		}
		final SubGraphImplementor<? extends J> previous = subgraphMap.put( subGraph.getClassType(), subGraph );
		if ( previous != null ) {
			log.debugf( "Adding subgraph [%s] over-wrote existing [%s]", subGraph, previous );
		}
	}

	@Override
	public <S extends J> void addSubGraph(Class<S> subType, SubGraph<S> subGraph) {
		verifyMutability();
		assert subGraph.getClassType() == subType;
		internalAddSubGraph( (SubGraphImplementor<S>) subGraph );
	}

	@Override
	public void addSubGraph(SubGraphImplementor<? extends J> subgraph) {
		internalAddSubGraph( subgraph );
	}

	@Override
	public SubGraphImplementor<J> makeKeySubGraph() {
		return internalMakeKeySubgraph( (Class<J>) null );
	}

	@Override
	public <S extends J> SubGraphImplementor<S> makeKeySubGraph(Class<S> subtype) {
		return internalMakeKeySubgraph( subtype );
	}

	@Override
	public <S extends J> SubGraphImplementor<S> makeKeySubGraph(ManagedDomainType<S> subtype) {
		return internalMakeKeySubgraph( subtype );
	}

	private <S extends J> SubGraphImplementor<S> internalMakeKeySubgraph(ManagedDomainType<S> type) {
		assert type != null;
		log.debugf( "Making key subgraph : ( (%s) %s )", type.getTypeName(), getAttributeName() );
		final SubGraphImplementor<S> subGraph = DomainModelHelper.makeSubGraph( type, type.getBindableJavaType() );
		internalAddKeySubGraph( subGraph );
		return subGraph;
	}

	private <S extends J> SubGraphImplementor<S> internalMakeKeySubgraph(Class<S> type) {
		verifyMutability();
		final ManagedDomainType<S> managedType = keyGraphTypeAsManaged();
		return internalMakeKeySubgraph( type == null ? managedType : findSubType( managedType, type ) );
	}

	protected void internalAddKeySubGraph(SubGraph<? extends J> subGraph) {
		log.tracef( "Adding key subgraph : ( (%s) %s )", subGraph.getClassType().getName(), getAttributeName() );
		if ( keySubgraphMap == null ) {
			keySubgraphMap = new HashMap<>();
		}
		final SubGraphImplementor<? extends J> previous =
				keySubgraphMap.put( subGraph.getClassType(), (SubGraphImplementor<? extends J>) subGraph );
		if ( previous != null ) {
			log.debugf( "Adding key subgraph [%s] overwrote existing [%]", subGraph, previous );
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends J> ManagedDomainType<T> keyGraphTypeAsManaged() {
		final SimpleDomainType<?> keyGraphType = getAttributeDescriptor().getKeyGraphType();
		if ( keyGraphType instanceof ManagedDomainType ) {
			return (ManagedDomainType<T>) keyGraphType;
		}
		else {
			throw new CannotContainSubGraphException(
					String.format(
							Locale.ROOT,
							"Attribute [%s#%s] (%s) cannot contain key subgraphs - %s",
							getAttributeDescriptor().getDeclaringType().getTypeName(),
							getAttributeName(),
							getAttributeDescriptor().getPersistentAttributeType().name(),
							keyGraphType
					)
			);
		}
	}

	@Override
	public <S extends J> void addKeySubGraph(Class<S> subType, SubGraph<S> subGraph) {
		assert subGraph.getClassType() == subType;
		internalAddKeySubGraph( subGraph );
	}

	@Override
	public AttributeNodeImplementor<J> makeCopy(boolean mutable) {
		return new AttributeNodeImpl<>(
				this.attribute, makeMapCopy( mutable, subgraphMap ), makeMapCopy( mutable, keySubgraphMap ), mutable
		);
	}

	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> makeMapCopy(
			boolean mutable,
			Map<Class<? extends J>, SubGraphImplementor<? extends J>> nodeMap) {
		if ( nodeMap == null ) {
			return null;
		}
		else {
			return nodeMap.entrySet().stream()
					.map(entry -> Map.entry( entry.getKey(), entry.getValue().makeCopy( mutable ) ))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}
	}

	@Override
	public void merge(AttributeNodeImplementor<J> other) {
		other.getSubGraphMap().values().forEach( this::mergeToSubgraph );
		other.getKeySubGraphMap().values().forEach( this::mergeToKeySubgraph );
	}

	private <T extends J> void mergeToKeySubgraph(SubGraphImplementor<T> subgraph) {
		final SubGraphImplementor<T> existing = getKeySubgraphForPut( subgraph );
		if ( existing != null ) {
			existing.merge( subgraph );
		}
		else {
			internalAddKeySubGraph( subgraph.makeCopy( true ) );
		}
	}

	private <T extends J> void mergeToSubgraph(SubGraphImplementor<T> subgraph) {
		final SubGraphImplementor<T> existing = getSubgraphForPut( subgraph );
		if ( existing != null ) {
			existing.merge( subgraph );
		}
		else {
			internalAddSubGraph( subgraph.makeCopy( true ) );
		}
	}

	private <T> SubGraphImplementor<T> getSubgraphForPut(SubGraphImplementor<T> subgraph) {
		final SubGraphImplementor<T> existing;
		if ( subgraphMap == null ) {
			subgraphMap = new HashMap<>();
			existing = null;
		}
		else {
			existing = getSubgraph( subgraph.getClassType() );
		}
		return existing;
	}

	private <T> SubGraphImplementor<T> getKeySubgraphForPut(SubGraphImplementor<T> subgraph) {
		if ( keySubgraphMap == null ) {
			keySubgraphMap = new HashMap<>();
			return null;
		}
		else {
			return getKeySubgraph( subgraph.getClassType() );
		}
	}

	@SuppressWarnings("unchecked")
	private <T> SubGraphImplementor<T> getSubgraph(Class<T> incomingSubtype) {
		return (SubGraphImplementor<T>) subgraphMap.get( incomingSubtype );
	}

	@SuppressWarnings("unchecked")
	private <T> SubGraphImplementor<T> getKeySubgraph(Class<T> incomingSubtype) {
		return (SubGraphImplementor<T>) keySubgraphMap.get( incomingSubtype );
	}
}
