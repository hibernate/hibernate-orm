/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;
import java.util.Objects;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.SpecialOneToOneType;

/**
 * A mapping model object representing a {@linkplain jakarta.persistence.OneToOne many-to-one association}.
 *
 * @author Gavin King
 */
public final class OneToOne extends ToOne {

	private boolean constrained;
	private ForeignKeyDirection foreignKeyType;
	private KeyValue identifier;
	private String propertyName;
	private final String entityName;
	private String mappedByProperty;

	public OneToOne(MetadataBuildingContext buildingContext, Table table, PersistentClass owner) throws MappingException {
		super( buildingContext, table );
		this.identifier = owner.getKey();
		this.entityName = owner.getEntityName();
	}

	private OneToOne(OneToOne original) {
		super( original );
		this.constrained = original.constrained;
		this.foreignKeyType = original.foreignKeyType;
		this.identifier = original.identifier == null ? null : (KeyValue) original.identifier.copy();
		this.propertyName = original.propertyName;
		this.entityName = original.entityName;
		this.mappedByProperty = original.mappedByProperty;
	}

	@Override
	public OneToOne copy() {
		return new OneToOne( this );
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName==null ? null : propertyName.intern();
	}

	public String getEntityName() {
		return entityName;
	}

	public OneToOneType getType() throws MappingException {
		if ( getColumnSpan()>0 ) {
			return new SpecialOneToOneType(
					getTypeConfiguration(),
					getReferencedEntityName(),
					getForeignKeyType(),
					isReferenceToPrimaryKey(),
					getReferencedPropertyName(),
					isLazy(),
					isUnwrapProxy(),
					getEntityName(),
					getPropertyName(),
					isConstrained()
			);
		}
		else {
			return new OneToOneType(
					getTypeConfiguration(),
					getReferencedEntityName(),
					getForeignKeyType(),
					isReferenceToPrimaryKey(),
					getReferencedPropertyName(),
					isLazy(),
					isUnwrapProxy(),
					entityName,
					propertyName,
					isConstrained()
			);
		}
	}

	@Override
	public void createUniqueKey(MetadataBuildingContext context) {
		if ( !hasFormula() && getColumnSpan()>0  ) {
			getTable().createUniqueKey( getConstraintColumns(), context );
		}
	}

	@Override
	public List<Selectable> getVirtualSelectables() {
		final var selectables = super.getVirtualSelectables();
		return selectables.isEmpty() ? identifier.getSelectables() : selectables;
	}

	public List<Column> getConstraintColumns() {
		final var columns = super.getColumns();
		return columns.isEmpty() ? identifier.getColumns() : columns;
	}

	/**
	 * Returns the constrained.
	 * @return boolean
	 */
	public boolean isConstrained() {
		return constrained;
	}

	/**
	 * Returns the foreignKeyType.
	 * @return AssociationType.ForeignKeyType
	 */
	public ForeignKeyDirection getForeignKeyType() {
		return foreignKeyType;
	}

	/**
	 * Returns the identifier.
	 * @return Value
	 */
	public KeyValue getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the constrained.
	 * @param constrained The constrained to set
	 */
	public void setConstrained(boolean constrained) {
		this.constrained = constrained;
	}

	/**
	 * Sets the foreignKeyType.
	 * @param foreignKeyType The foreignKeyType to set
	 */
	public void setForeignKeyType(ForeignKeyDirection foreignKeyType) {
		this.foreignKeyType = foreignKeyType;
	}

	/**
	 * Sets the identifier.
	 * @param identifier The identifier to set
	 */
	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public boolean isNullable() {
		return !constrained;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public boolean isSame(ToOne other) {
		return other instanceof OneToOne oneToOne
			&& isSame( oneToOne );
	}

	public boolean isSame(OneToOne other) {
		return super.isSame( other )
			&& Objects.equals( foreignKeyType, other.foreignKeyType )
			&& isSame( identifier, other.identifier )
			&& Objects.equals( propertyName, other.propertyName )
			&& Objects.equals( entityName, other.entityName )
			&& constrained == other.constrained;
	}

	public String getMappedByProperty() {
		return mappedByProperty;
	}

	public void setMappedByProperty(String mappedByProperty) {
		this.mappedByProperty = mappedByProperty;
	}
}
