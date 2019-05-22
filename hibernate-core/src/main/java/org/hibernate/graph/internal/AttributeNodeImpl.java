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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;

import org.jboss.logging.Logger;

/**
 * Hibernate implementation of the JPA AttributeNode contract
 *
 * @author Steve Ebersole
 */
public class AttributeNodeImpl<J>
		extends AbstractGraphNode<J>
		implements AttributeNodeImplementor<J> {
	private final PersistentAttribute<?, J> attribute;

	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> subGraphMap;
	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> keySubGraphMap;

	@SuppressWarnings("WeakerAccess")
	public <X> AttributeNodeImpl(
			boolean mutable,
			PersistentAttribute<X, J> attribute,
			SessionFactoryImplementor sessionFactory) {
		this( mutable, attribute, null, null, sessionFactory );
	}

	/**
	 * Intended only for use from making a copy
	 */
	private AttributeNodeImpl(
			boolean mutable,
			PersistentAttribute<?, J> attribute,
			Map<Class<? extends J>, SubGraphImplementor<? extends J>> subGraphMap,
			Map<Class<? extends J>, SubGraphImplementor<? extends J>> keySubGraphMap,
			SessionFactoryImplementor sessionFactory) {
		super( mutable, sessionFactory );
		this.attribute = attribute;
		this.subGraphMap = subGraphMap;
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
	@SuppressWarnings("unchecked")
	public Map<Class<? extends J>, SubGraphImplementor<? extends J>> getSubGraphMap() {
		if ( subGraphMap == null ) {
			return Collections.emptyMap();
		}
		else {
			return (Map) subGraphMap;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Class<? extends J>, SubGraphImplementor<? extends J>> getKeySubGraphMap() {
		if ( keySubGraphMap == null ) {
			return Collections.emptyMap();
		}
		else {
			return keySubGraphMap;
		}
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

		final SubGraphImplementor<S> subGraph = type.makeSubGraph();
		internalAddSubGraph( type.getJavaType(), subGraph );

		return subGraph;
	}

	@SuppressWarnings("unchecked")
	private <T extends J> ManagedDomainType<T> valueGraphTypeAsManaged() {
		final SimpleDomainType<J> valueGraphType = (SimpleDomainType) getAttributeDescriptor().getValueGraphType();

		if ( valueGraphType instanceof ManagedDomainType ) {
			return (ManagedDomainType) valueGraphType;
		}

		throw new CannotContainSubGraphException(
				String.format(
						Locale.ROOT,
						"Attribute [%s] (%s) cannot contain value sub-graphs",
						getAttributeName(),
						getAttributeDescriptor().getPersistentAttributeType().name()
				)
		);
	}

	private static final Logger log = Logger.getLogger( AttributeNodeImpl.class );

	@SuppressWarnings("unchecked")
	private <S extends J> SubGraphImplementor<S> internalMakeSubgraph(Class<S> subType) {
		verifyMutability();

		final ManagedDomainType<S> managedType = valueGraphTypeAsManaged();

		if ( subType == null ) {
			subType = managedType.getJavaType();
		}

		return internalMakeSubgraph( managedType.findSubType( subType ) );
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected <S extends J> void internalAddSubGraph(Class<S> subType, SubGraphImplementor<S> subGraph) {
		log.tracef( "Adding sub-graph : ( (%s) %s )", subGraph.getGraphedType().getName(), getAttributeName() );

		if ( subGraphMap == null ) {
			subGraphMap = new HashMap<>();
		}

		final SubGraphImplementor<? extends J> previous = subGraphMap.put( subType, (SubGraphImplementor) subGraph );
		if ( previous != null ) {
			log.debugf( "Adding sub-graph [%s] over-wrote existing [%]", subGraph, previous );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends J> void addSubGraph(Class<S> subType, SubGraph<S> subGraph) {
		verifyMutability();

		internalAddSubGraph( subType, (SubGraphImplementor) subGraph );
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

		log.debugf( "Making key sub-graph : ( (%s) %s )", type.getTypeName(), getAttributeName() );

		final SubGraphImplementor<S> subGraph = type.makeSubGraph();
		internalAddKeySubGraph( type.getJavaType(), subGraph );

		return subGraph;
	}

	@SuppressWarnings("unchecked")
	private <S extends J> SubGraphImplementor<S> internalMakeKeySubgraph(Class<S> type) {
		verifyMutability();

		final ManagedDomainType<S> managedType = keyGraphTypeAsManaged();

		final ManagedDomainType<S> subType;

		if ( type == null ) {
			subType = managedType;
		}
		else {
			subType = managedType.findSubType( type );
		}

		subType.getJavaType();

		return internalMakeKeySubgraph( subType );
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected <S extends J> void internalAddKeySubGraph(Class<S> subType, SubGraph<S> subGraph) {
		log.tracef( "Adding key sub-graph : ( (%s) %s )", subType.getName(), getAttributeName() );

		if ( keySubGraphMap == null ) {
			keySubGraphMap = new HashMap<>();
		}

		final SubGraphImplementor<? extends J> previous = keySubGraphMap.put( subType, (SubGraphImplementor) subGraph );
		if ( previous != null ) {
			log.debugf( "Adding key sub-graph [%s] over-wrote existing [%]", subGraph, previous );
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends J> ManagedDomainType<T> keyGraphTypeAsManaged() {
		final SimpleDomainType<J> keyGraphType = (SimpleDomainType) getAttributeDescriptor().getKeyGraphType();

		if ( keyGraphType instanceof ManagedDomainType ) {
			return (ManagedDomainType) keyGraphType;
		}

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

	@Override
	@SuppressWarnings("unchecked")
	public <S extends J> void addKeySubGraph(Class<S> subType, SubGraph<S> subGraph) {
		internalAddKeySubGraph( subType, subGraph );
	}

	@Override
	@SuppressWarnings("unchecked")
	public AttributeNodeImplementor<J> makeCopy(boolean mutable) {
		return new AttributeNodeImpl<>(
				mutable,
				this.attribute,
				makeMapCopy( mutable, (Map) subGraphMap ),
				makeMapCopy( mutable, (Map) keySubGraphMap ),
				sessionFactory()
		);
	}

	private <S extends J> Map<Class<S>, SubGraphImplementor<S>> makeMapCopy(
			boolean mutable,
			Map<Class<S>, SubGraphImplementor<S>> nodeMap) {
		if ( nodeMap == null ) {
			return null;
		}

		return CollectionHelper.makeCopy(
				nodeMap,
				type -> type,
				subGraph -> subGraph.makeCopy( mutable )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void merge(AttributeNodeImplementor<?> attributeNode) {
		attributeNode.visitSubGraphs(
				(incomingSubType, incomingGraph) -> {
					SubGraphImplementor existing = null;
					if ( subGraphMap == null ) {
						subGraphMap = new HashMap<>();
					}
					else {
						existing = subGraphMap.get( incomingSubType );
					}

					if ( existing != null ) {
						existing.merge( incomingGraph );
					}
					else {
						internalAddSubGraph( (Class) incomingSubType, (SubGraphImplementor) incomingGraph.makeCopy( true ) );
					}
				}
		);

		attributeNode.visitKeySubGraphs(
				(incomingSubType, incomingGraph) -> {
					SubGraphImplementor existing = null;
					if ( keySubGraphMap == null ) {
						keySubGraphMap = new HashMap<>();
					}
					else {
						existing = keySubGraphMap.get( incomingSubType );
					}

					if ( existing != null ) {
						existing.merge( incomingGraph );
					}
					else {
						internalAddKeySubGraph( (Class) incomingSubType, (SubGraphImplementor) incomingGraph.makeCopy( true ) );
					}
				}
		);
	}
}
