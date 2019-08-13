/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;


/**
 * Represents the reference to a Map attribute's {@link Map.Entry} entries
 * in a select clause
 *
 * @author Gunnar Morling
 * @author Steve Ebersole
 */
public class SqmMapEntryReference<K,V>
		implements SqmSelectableNode<Map.Entry<K,V>>, Expression<Map.Entry<K,V>>, DomainResultProducer<Map.Entry<K,V>> {
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private final SqmPath<?> mapPath;
	private final NodeBuilder nodeBuilder;

	private final JavaTypeDescriptor<Map.Entry<K,V>> mapEntryTypeDescriptor;

	private String explicitAlias;

	public SqmMapEntryReference(
			SqmPath<?> mapPath,
			NodeBuilder nodeBuilder) {
		this.mapPath = mapPath;
		this.nodeBuilder = nodeBuilder;

		//noinspection unchecked
		this.mapEntryTypeDescriptor = (JavaTypeDescriptor) nodeBuilder.getDomainModel()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( Map.Entry.class );
	}

	@Override
	public String getAlias() {
		return explicitAlias;
	}

	public SqmPath<?> getMapPath() {
		return mapPath;
	}

	@Override
	public JpaSelection<Map.Entry<K, V>> alias(String name) {
		this.explicitAlias = name;
		return this;
	}

	@Override
	public JavaTypeDescriptor<Map.Entry<K, V>> getJavaTypeDescriptor() {
		return mapEntryTypeDescriptor;
	}

	@Override
	public JavaTypeDescriptor<Map.Entry<K, V>> getNodeJavaTypeDescriptor() {
		return mapEntryTypeDescriptor;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitMapEntryFunction( this );
	}

	@Override
	public DomainResultProducer<Map.Entry<K, V>> getDomainResultProducer() {
		return this;
	}

	@Override
	public void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer) {
	}

	@Override
	public boolean isCompoundSelection() {
		return false;
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		return Collections.emptyList();
	}

	@Override
	public SqmExpressable<Map.Entry<K, V>> getNodeType() {
		return null;
	}

	@Override
	public NodeBuilder nodeBuilder() {
		return nodeBuilder;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA (ugh)

	@Override
	public Predicate isNull() {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate isNotNull() {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate in(Object... values) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate in(Expression<?>... values) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate in(Collection<?> values) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public Predicate in(Expression<Collection<?>> values) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}

	@Override
	public <X> Expression<X> as(Class<X> type) {
		throw new UnsupportedOperationException( "Whatever JPA" );
	}
}
