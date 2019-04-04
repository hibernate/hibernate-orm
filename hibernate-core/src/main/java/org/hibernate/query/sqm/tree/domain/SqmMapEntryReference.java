/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Map;

import org.hibernate.metamodel.model.domain.spi.PluralValuedNavigable;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * Represents the reference to a Map attribute's {@link java.util.Map.Entry} entries
 * in a select clause
 *
 * @author Gunnar Morling
 * @author Steve Ebersole
 */
public class SqmMapEntryReference implements SqmExpression, ExpressableType {
	private final SqmPath mapPath;
	private final BasicJavaDescriptor<Map.Entry> mapEntryTypeDescriptor;

	public SqmMapEntryReference(
			SqmPath mapPath,
			BasicJavaDescriptor<Map.Entry> mapEntryTypeDescriptor) {
		this.mapPath = mapPath;
		this.mapEntryTypeDescriptor = mapEntryTypeDescriptor;
	}

	public SqmPath getMapPath() {
		return mapPath;
	}

	public PluralValuedNavigable getMapNavigable() {
		return mapPath.as( PluralValuedNavigable.class );
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
	public void applyInferableType(ExpressableType<?> type) {
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMapEntryFunction( this );
	}

	@Override
	public String asLoggableText() {
		return "MAP_ENTRY(" + getMapNavigable().asLoggableText() + ")";
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
