/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A mapping model object representing a {@linkplain jakarta.persistence.ManyToOne many-to-one association}.
 *
 * @author Gavin King
 */
public final class ManyToOne extends ToOne {
	private boolean isLogicalOneToOne;
	private NotFoundAction notFoundAction;
	private Boolean nullable;
	private final TypeConfiguration typeConfiguration;

	private transient ManyToOneType resolvedType;

	public ManyToOne(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
		this.typeConfiguration = buildingContext.getTypeConfiguration();
	}

	private ManyToOne(ManyToOne original) {
		super( original );
		this.typeConfiguration = original.typeConfiguration;
		this.notFoundAction = original.notFoundAction;
		this.isLogicalOneToOne = original.isLogicalOneToOne;
		this.nullable = original.nullable;
	}

	@Override
	public ManyToOne copy() {
		return new ManyToOne( this );
	}

	public ManyToOneType getType() throws MappingException {
		if ( resolvedType == null ) {
			resolvedType = new ManyToOneType(
					typeConfiguration,
					getReferencedEntityName(),
					isReferenceToPrimaryKey(),
					getReferencedPropertyName(),
					getPropertyName(),
					isLazy(),
					isUnwrapProxy(),
					isIgnoreNotFound(),
					isLogicalOneToOne()
			);
		}
		return resolvedType;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public NotFoundAction getNotFoundAction() {
		return notFoundAction;
	}

	public void setNotFoundAction(NotFoundAction notFoundAction) {
		this.notFoundAction = notFoundAction;
	}

	public boolean isIgnoreNotFound() {
		return notFoundAction == NotFoundAction.IGNORE;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.notFoundAction = ignoreNotFound
				? NotFoundAction.IGNORE
				: null;
	}

	public void markAsLogicalOneToOne() {
		this.isLogicalOneToOne = true;
	}

	public boolean isLogicalOneToOne() {
		return isLogicalOneToOne;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	@Override
	public boolean isNullable() {
		return nullable != null ? nullable : getReferencedPropertyName() != null || super.isNullable();
	}
}
