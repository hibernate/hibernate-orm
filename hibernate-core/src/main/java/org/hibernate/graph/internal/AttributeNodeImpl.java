/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
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

	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> keySubGraphMap;
	private SubGraphImplementor<J> subgraph;


	public <X> AttributeNodeImpl(PersistentAttribute<X, J> attribute, boolean mutable) {
		this( attribute, null, null, mutable );
	}

	/**
	 * Intended only for use from making a copy
	 */
	private AttributeNodeImpl(
			PersistentAttribute<?, J> attribute,
			SubGraphImplementor<J> subgraph,
			Map<Class<? extends J>, SubGraphImplementor<? extends J>> keySubGraphMap,
			boolean mutable) {
		super( mutable );
		this.attribute = attribute;
		this.subgraph = subgraph;
		this.keySubGraphMap = keySubGraphMap;
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
		if ( this.subgraph == null ) {
			return emptyMap();
		}

		var subclassSubgraphs = subgraph.getSubclassSubgraphs();

		if ( subclassSubgraphs.isEmpty() ) {
			return Collections.singletonMap( subgraph.getClassType(), subgraph );
		}

		subclassSubgraphs.put( subgraph.getClassType(), subgraph );

		return subclassSubgraphs;
	}

	@Override
	public SubGraphImplementor<J> getSubgraph() {
		return subgraph;
	}

	@Override
	public Map<Class<? extends J>, SubGraphImplementor<? extends J>> getKeySubGraphMap() {
		return keySubGraphMap == null ? emptyMap() : keySubGraphMap;
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

		log.debugf( "Making sub-graph : ( (%s) %s )", type.getTypeName(), getAttributeName() );

		if ( this.subgraph == null ) {
			this.subgraph = new SubGraphImpl<>( valueGraphTypeAsManaged(), true );
		}

		if ( type.equals( valueGraphTypeAsManaged() ) ) {
			return (SubGraphImplementor) this.subgraph;
		}

		return this.subgraph.addTreatedSubgraph( type.getJavaType() );
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
							"Attribute [%s] (%s) cannot contain value sub-graphs",
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
		return internalMakeSubgraph( findSubType(
				managedType,
				subType == null ? managedType.getJavaType() : subType
		) );
	}

	protected void internalAddSubGraph(SubGraphImplementor<? extends J> subGraph) {
		log.tracef( "Adding sub-graph : ( (%s) %s )", subGraph.getGraphedType().getTypeName(), getAttributeName() );

		if ( this.subgraph == null ) {
			this.subgraph = new SubGraphImpl<>( valueGraphTypeAsManaged(), true );
		}

		ManagedDomainType<?> incomingSubGraphType = subGraph.getGraphedType();

		if ( incomingSubGraphType.equals( this.subgraph.getGraphedType() ) ) {
			this.subgraph.merge( subGraph );
			return;
		}

		if ( this.subgraph.getGraphedType().getSubTypes().contains( incomingSubGraphType ) ) {
			this.subgraph.addSubclassSubgraph( subGraph );
		}

	}

	@Override
	public <S extends J> void addSubGraph(Class<S> subType, SubGraph<S> subGraph) {
		verifyMutability();
		assert subGraph.getClassType() == subType;
		internalAddSubGraph( (SubGraphImplementor<S>) subGraph );
	}

	@Override
	public void addSubGraph(SubGraphImplementor<? extends J> subGraph) {
		internalAddSubGraph( subGraph );
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
		log.debugf( "Making key sub-graph : ( (%s) %s )", type.getTypeName(), getAttributeName() );
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
		log.tracef( "Adding key sub-graph : ( (%s) %s )", subGraph.getClassType().getName(), getAttributeName() );
		if ( keySubGraphMap == null ) {
			keySubGraphMap = new HashMap<>();
		}
		final SubGraphImplementor<? extends J> previous =
				keySubGraphMap.put( subGraph.getClassType(), (SubGraphImplementor<? extends J>) subGraph );
		if ( previous != null ) {
			log.debugf( "Adding key sub-graph [%s] over-wrote existing [%]", subGraph, previous );
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
							"Attribute [%s#%s] (%s) cannot contain key sub-graphs - %s",
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
				this.attribute,
				this.subgraph == null ? null : this.subgraph.makeCopy( mutable ),
				makeMapCopy( mutable, keySubGraphMap ),
				mutable
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
					.map( entry -> Map.entry( entry.getKey(), entry.getValue().makeCopy( mutable ) ) )
					.collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
		}
	}

	@Override
	public void merge(AttributeNodeImplementor<?> attributeNode) {


		if ( this.subgraph != null ) {
			this.subgraph.merge( (SubGraphImplementor) attributeNode.getSubgraph() );
		}
		else {
			this.subgraph = (SubGraphImplementor) attributeNode.getSubgraph();
		}

		attributeNode.visitKeySubGraphs(
				(incomingSubType, incomingGraph) -> {
					SubGraphImplementor<?> existing;
					if ( keySubGraphMap == null ) {
						keySubGraphMap = new HashMap<>();
						existing = null;
					}
					else {
						existing = keySubGraphMap.get( incomingSubType );
					}

					if ( existing != null ) {
						existing.merge( (GraphImplementor) incomingGraph );
					}
					else {
						internalAddKeySubGraph( (SubGraphImplementor) incomingGraph.makeCopy( true ) );
					}
				}
		);
	}
}
