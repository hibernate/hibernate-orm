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
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Hibernate implementation of the JPA AttributeNode contract
 *
 * @author Steve Ebersole
 */
public class AttributeNodeImpl<J>
		extends AbstractGraphNode<J>
		implements AttributeNodeImplementor<J> {
	private final PersistentAttributeDescriptor<?, J> attribute;

	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> subGraphMap;
	private Map<Class<? extends J>, SubGraphImplementor<? extends J>> keySubGraphMap;

	@SuppressWarnings("WeakerAccess")
	public <X> AttributeNodeImpl(
			boolean mutable,
			PersistentAttributeDescriptor<X, J> attribute,
			SessionFactoryImplementor sessionFactory) {
		this( mutable, attribute, null, null, sessionFactory );
	}

	/**
	 * Intended only for use from making a copy
	 */
	private AttributeNodeImpl(
			boolean mutable,
			PersistentAttributeDescriptor<?, J> attribute,
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
	public PersistentAttributeDescriptor<?, J> getAttributeDescriptor() {
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
	public <S extends J> SubGraphImplementor<S> makeSubGraph(ManagedTypeDescriptor<S> subtype) {
		return internalMakeSubgraph( subtype );
	}

	private <S extends J> SubGraphImplementor<S> internalMakeSubgraph(ManagedTypeDescriptor<S> type) {
		assert type != null;

		log.debugf( "Making sub-graph : ( (%s) %s )", type.getName(), getAttributeName() );

		final SubGraphImplementor<S> subGraph = type.makeSubGraph();
		internalAddSubGraph( type.getJavaType(), subGraph );

		return subGraph;
	}

	@SuppressWarnings("unchecked")
	private <T extends J> ManagedTypeDescriptor<T> valueGraphTypeAsManaged() {
		final SimpleTypeDescriptor<J> valueGraphType = (SimpleTypeDescriptor) getAttributeDescriptor().getValueGraphType();

		if ( valueGraphType instanceof ManagedTypeDescriptor ) {
			return (ManagedTypeDescriptor) valueGraphType;
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

		final ManagedTypeDescriptor<S> managedType = valueGraphTypeAsManaged();

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
			log.debugf( "Adding sub-graph [%s] over-wrote existing [%s]", subGraph, previous );
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
	public <S extends J> SubGraphImplementor<S> makeKeySubGraph(ManagedTypeDescriptor<S> subtype) {
		return internalMakeKeySubgraph( subtype );
	}

	private <S extends J> SubGraphImplementor<S> internalMakeKeySubgraph(ManagedTypeDescriptor<S> type) {

		log.debugf( "Making key sub-graph : ( (%s) %s )", type.getName(), getAttributeName() );

		final SubGraphImplementor<S> subGraph = type.makeSubGraph();
		internalAddKeySubGraph( type.getJavaType(), subGraph );

		return subGraph;
	}

	@SuppressWarnings("unchecked")
	private <S extends J> SubGraphImplementor<S> internalMakeKeySubgraph(Class<S> type) {
		verifyMutability();

		final ManagedTypeDescriptor<S> managedType = keyGraphTypeAsManaged();

		final ManagedTypeDescriptor<S> subType;

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
	private <T extends J> ManagedTypeDescriptor<T> keyGraphTypeAsManaged() {
		final SimpleTypeDescriptor<J> keyGraphType = (SimpleTypeDescriptor) getAttributeDescriptor().getKeyGraphType();

		if ( keyGraphType instanceof ManagedTypeDescriptor ) {
			return (ManagedTypeDescriptor) keyGraphType;
		}

		throw new CannotContainSubGraphException(
				String.format(
						Locale.ROOT,
						"Attribute [%s#%s] (%s) cannot contain key sub-graphs - %s",
						getAttributeDescriptor().getDeclaringType().getName(),
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
