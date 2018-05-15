/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.Map;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * Represents the ENTRY() function for obtaining the map entries from a {@code Map}-typed association.
 *
 * @author Gunnar Morling
 * @author Steve Ebersole
 */
public class SqmMapEntryBinding implements SqmExpression, ExpressableType {
	private final SqmPluralAttributeReference attributeBinding;
	private final BasicJavaDescriptor<Map.Entry> mapEntryTypeDescriptor;

	public SqmMapEntryBinding(
			SqmPluralAttributeReference attributeBinding,
			BasicJavaDescriptor<Map.Entry> mapEntryTypeDescriptor) {
		this.attributeBinding = attributeBinding;
		this.mapEntryTypeDescriptor = mapEntryTypeDescriptor;
	}

	public SqmPluralAttributeReference getAttributeAttributeReference() {
		return attributeBinding;
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return mapEntryTypeDescriptor;
	}

	@Override
	public ExpressableType getExpressableType() {
		return this;
	}

	@Override
	public ExpressableType getInferableType() {
		return this;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMapEntryFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "MAP_ENTRY(" + attributeBinding.asLoggableText() + ")";
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}
}
