/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;
import java.util.Objects;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;
import org.hibernate.type.MappingContext;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_BOOLEAN_ARRAY;

/**
 * A mapping model object representing a {@linkplain jakarta.persistence.OneToMany many-to-one association}.
 *
 * @author Gavin King
 */
public class OneToMany implements Value {
	private final MetadataBuildingContext buildingContext;
	private final Table referencingTable;

	private String referencedEntityName;
	private PersistentClass associatedClass;
	private NotFoundAction notFoundAction;

	public OneToMany(MetadataBuildingContext buildingContext, PersistentClass owner) throws MappingException {
		this.buildingContext = buildingContext;
		this.referencingTable = owner == null ? null : owner.getTable();
	}

	private OneToMany(OneToMany original) {
		this.buildingContext = original.buildingContext;
		this.referencingTable = original.referencingTable;
		this.referencedEntityName = original.referencedEntityName;
		this.associatedClass = original.associatedClass;
		this.notFoundAction = original.notFoundAction;
	}

	@Override
	public Value copy() {
		return new OneToMany( this );
	}

	@Override
	public MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return buildingContext.getBuildingOptions().getServiceRegistry();
	}

	public PersistentClass getAssociatedClass() {
		return associatedClass;
	}

	/**
	 * Associated entity on the many side
	 */
	public void setAssociatedClass(PersistentClass associatedClass) {
		this.associatedClass = associatedClass;
	}

	public void createForeignKey() {
		// no foreign key element for a one-to-many
	}

	@Override
	public void createUniqueKey(MetadataBuildingContext context) {
	}

	@Override
	public List<Selectable> getSelectables() {
		return associatedClass.getKey().getSelectables();
	}

	@Override
	public List<Column> getColumns() {
		return associatedClass.getKey().getColumns();
	}

	@Override
	public int getColumnSpan() {
		return associatedClass.getKey().getColumnSpan();
	}

	@Override
	public FetchMode getFetchMode() {
		return FetchMode.JOIN;
	}

	/**
	 * Table of the owner entity (the "one" side)
	 */
	@Override
	public Table getTable() {
		return referencingTable;
	}

	@Override
	public Type getType() {
		return new ManyToOneType(
				buildingContext.getBootstrapContext().getTypeConfiguration(),
				getReferencedEntityName(),
				true,
				null,
				null,
				false,
				isIgnoreNotFound(),
				false,
				false
		);
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isSimpleValue() {
		return false;
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return false;
	}

	@Override
	public boolean hasFormula() {
		return false;
	}

	@Override
	public boolean isValid(MappingContext mappingContext) throws MappingException {
		if ( referencedEntityName == null ) {
			throw new MappingException( "one to many association must specify the referenced entity" );
		}
		return true;
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	/**
	 * Associated entity on the "many" side
	 */
	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName == null ? null : referencedEntityName.intern();
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) {
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept( this );
	}

	@Override
	public boolean isSame(Value other) {
		return this == other
			|| other instanceof OneToMany oneToMany && isSame( oneToMany );
	}

	public boolean isSame(OneToMany other) {
		return Objects.equals( referencingTable, other.referencingTable )
			&& Objects.equals( referencedEntityName, other.referencedEntityName )
			&& Objects.equals( associatedClass, other.associatedClass );
	}

	@Override
	public boolean[] getColumnInsertability() {
		//TODO: we could just return all false...
		return EMPTY_BOOLEAN_ARRAY;
	}

	@Override
	public boolean hasAnyInsertableColumns() {
		return false;
	}

	@Override
	public boolean[] getColumnUpdateability() {
		//TODO: we could just return all false...
		return EMPTY_BOOLEAN_ARRAY;
	}

	@Override
	public boolean hasAnyUpdatableColumns() {
		return false;
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
		notFoundAction = ignoreNotFound ? NotFoundAction.IGNORE : null;
	}

	@Override
	public boolean isColumnInsertable(int index) {
		return false;
	}

	@Override
	public boolean isColumnUpdateable(int index) {
		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public boolean isPartitionKey() {
		return false;
	}
}
