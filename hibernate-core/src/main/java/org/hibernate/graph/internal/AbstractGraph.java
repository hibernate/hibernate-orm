/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.graph.AttributeNode;
import org.hibernate.graph.CannotBecomeEntityGraphException;
import org.hibernate.graph.CannotContainSubGraphException;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.SubGraph;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

import static java.util.Collections.emptyList;

/**
 *  Base class for {@link RootGraph} and {@link SubGraph} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractGraph<J> extends AbstractGraphNode<J> implements GraphImplementor<J> {
	private final ManagedDomainType<J> managedType;
	private Map<PersistentAttribute<?,?>, AttributeNodeImplementor<?>> attrNodeMap;

	public AbstractGraph(
			ManagedDomainType<J> managedType,
			boolean mutable,
			JpaMetamodel jpaMetamodel) {
		super( mutable, jpaMetamodel );
		this.managedType = managedType;
	}

	protected AbstractGraph(boolean mutable, GraphImplementor<J> original) {
		this( original.getGraphedType(), mutable, original.jpaMetamodel() );

		this.attrNodeMap = new ConcurrentHashMap<>( original.getAttributeNodeList().size() );
		original.visitAttributeNodes(
				node -> attrNodeMap.put(
						node.getAttributeDescriptor(),
						node.makeCopy( mutable )
				)
		);
	}

	@Override
	public JpaMetamodel jpaMetamodel() {
		return super.jpaMetamodel();
	}

	@Override
	public ManagedDomainType<J> getGraphedType() {
		return managedType;
	}

	@Override
	public RootGraphImplementor<J> makeRootGraph(String name, boolean mutable) {
		if ( getGraphedType() instanceof EntityDomainType ) {
			return new RootGraphImpl<>( name, mutable, this );
		}

		throw new CannotBecomeEntityGraphException(
				"Cannot transform Graph to RootGraph - " + getGraphedType() + " is not an EntityType"
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void merge(GraphImplementor<? extends J> other) {
		if ( other == null ) {
			return;
		}

		for ( AttributeNodeImplementor<?> attributeNode : other.getAttributeNodeImplementors() ) {
			final AttributeNodeImplementor<?> localAttributeNode = findAttributeNode(
					(PersistentAttribute<? extends J,?>) attributeNode.getAttributeDescriptor()
			);
			if ( localAttributeNode != null ) {
				// keep the local one, but merge in the incoming one
				localAttributeNode.merge( attributeNode );
			}
			else {
				addAttributeNode( attributeNode.makeCopy( true ) );
			}
		}

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeNode handling

	@Override
	public AttributeNodeImplementor<?> addAttributeNode(AttributeNodeImplementor<?> incomingAttributeNode) {
		verifyMutability();

		AttributeNodeImplementor<?> attributeNode = null;
		if ( attrNodeMap == null ) {
			attrNodeMap = new HashMap<>();
		}
		else {
			attributeNode = attrNodeMap.get( incomingAttributeNode.getAttributeDescriptor() );
		}

		if ( attributeNode == null ) {
			attributeNode = incomingAttributeNode;
			attrNodeMap.put( incomingAttributeNode.getAttributeDescriptor(), attributeNode );
		}
		else {
			@SuppressWarnings("rawtypes")
			final AttributeNodeImplementor attributeNodeFinal = attributeNode;
			incomingAttributeNode.visitSubGraphs(
					// we assume the subGraph has been properly copied if needed
					(subType, subGraph) -> attributeNodeFinal.addSubGraph( subGraph )
			);
		}

		return attributeNode;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <AJ> AttributeNodeImplementor<AJ> findAttributeNode(String attributeName) {
		final PersistentAttribute<? super J, ?> attribute = managedType.findAttributeInSuperTypes( attributeName );
		return attribute == null ? null : findAttributeNode( (PersistentAttribute<? extends J, AJ>) attribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <AJ> AttributeNodeImplementor<AJ> findAttributeNode(PersistentAttribute<? extends J, AJ> attribute) {
		return attrNodeMap == null ? null : (AttributeNodeImplementor<AJ>) attrNodeMap.get( attribute );
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public List<AttributeNode<?>> getGraphAttributeNodes() {
		return (List) getAttributeNodeImplementors();
	}

	@Override
	public List<AttributeNodeImplementor<?>> getAttributeNodeImplementors() {
		return attrNodeMap == null ? emptyList() : new ArrayList<>( attrNodeMap.values() );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> addAttributeNode(String attributeName)
			throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attributeName );
	}

	@Override
	public <AJ> AttributeNodeImplementor<AJ> addAttributeNode(PersistentAttribute<? extends J, AJ> attribute)
			throws CannotContainSubGraphException {
		return findOrCreateAttributeNode( attribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <AJ> AttributeNodeImplementor<AJ> findOrCreateAttributeNode(PersistentAttribute<? extends J, AJ> attribute) {
		verifyMutability();

		AttributeNodeImplementor<AJ> attrNode = null;
		if ( attrNodeMap == null ) {
			attrNodeMap = new HashMap<>();
		}
		else {
			attrNode = (AttributeNodeImplementor<AJ>) attrNodeMap.get( attribute );
		}

		if ( attrNode == null ) {
			attrNode = new AttributeNodeImpl<>( isMutable(), attribute, jpaMetamodel() );
			attrNodeMap.put( attribute, attrNode );
		}

		return attrNode;
	}
}
