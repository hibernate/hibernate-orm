/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.internal.AnnotatedJoinColumns;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.EntityType;
import org.hibernate.type.MappingContext;

import java.util.Objects;

import static org.hibernate.boot.model.internal.BinderHelper.findReferencedColumnOwner;
import static org.hibernate.internal.util.ReflectHelper.reflectedPropertyClass;

/**
 * A mapping model object representing an association where the target side has cardinality one.
 *
 * @author Gavin King
 */
public abstract sealed class ToOne
		extends SimpleValue implements Fetchable, SortableValue
		permits OneToOne, ManyToOne {

	private FetchMode fetchMode;
	protected String referencedPropertyName;
	private String referencedEntityName;
	private String propertyName;
	private boolean lazy = true;
	private boolean sorted;
	private boolean unwrapProxy;
	private boolean unwrapProxyImplicit;
	private boolean referenceToPrimaryKey = true;
	private boolean hasMapsId = false;

	protected ToOne(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
	}

	protected ToOne(ToOne original) {
		super( original );
		this.fetchMode = original.fetchMode;
		this.referencedPropertyName = original.referencedPropertyName;
		this.referencedEntityName = original.referencedEntityName;
		this.propertyName = original.propertyName;
		this.lazy = original.lazy;
		this.sorted = original.sorted;
		this.unwrapProxy = original.unwrapProxy;
		this.unwrapProxyImplicit = original.unwrapProxyImplicit;
		this.referenceToPrimaryKey = original.referenceToPrimaryKey;
	}

	@Override
	public FetchMode getFetchMode() {
		return fetchMode;
	}

	@Override
	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode=fetchMode;
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String name) {
		referencedPropertyName = name==null ? null : name.intern();
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ?
				null : referencedEntityName.intern();
	}

	public boolean hasMapsId() {
		return hasMapsId;
	}

	public void setHasMapsId(boolean hasMapsId) {
		this.hasMapsId = hasMapsId;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName==null ?
				null : propertyName.intern();
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		if ( referencedEntityName == null ) {
			final var classLoaderService = getBootstrapContext().getClassLoaderService();
			referencedEntityName = reflectedPropertyClass( className, propertyName, classLoaderService ).getName();
		}
	}

	@Override
	public boolean isTypeSpecified() {
		return referencedEntityName!=null;
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof ToOne toOne && isSame( toOne );
	}

	public boolean isSame(ToOne other) {
		return super.isSame( other )
			&& Objects.equals( referencedPropertyName, other.referencedPropertyName )
			&& Objects.equals( referencedEntityName, other.referencedEntityName );
	}

	@Override
	public boolean isValid(MappingContext mappingContext) throws MappingException {
		if ( referencedEntityName==null ) {
			throw new MappingException("association must specify the referenced entity");
		}
		return super.isValid( mappingContext );
	}

	@Override
	public boolean isLazy() {
		return lazy;
	}

	@Override
	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public boolean isUnwrapProxy() {
		return unwrapProxy;
	}

	public void setUnwrapProxy(boolean unwrapProxy) {
		this.unwrapProxy = unwrapProxy;
	}

	public boolean isUnwrapProxyImplicit() {
		return unwrapProxyImplicit;
	}

	/**
	 * Related to HHH-13658 - keep track of whether `unwrapProxy` is an implicit value
	 * for reference later
	 */
	public void setUnwrapProxyImplicit(boolean unwrapProxyImplicit) {
		this.unwrapProxyImplicit = unwrapProxyImplicit;
	}

	public boolean isReferenceToPrimaryKey() {
		return referenceToPrimaryKey;
	}

	public void setReferenceToPrimaryKey(boolean referenceToPrimaryKey) {
		this.referenceToPrimaryKey = referenceToPrimaryKey;
	}

	@Override
	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	@Override
	public int[] sortProperties() {
		final var entityBinding = getMetadata().getEntityBinding( referencedEntityName );
		if ( entityBinding != null ) {
			final var value =
					referencedPropertyName == null
							? entityBinding.getIdentifier()
							: entityBinding.getRecursiveProperty( referencedPropertyName ).getValue();
			if ( value instanceof Component component ) {
				final int[] originalPropertyOrder = component.sortProperties();
				if ( !sorted ) {
					if ( originalPropertyOrder != null ) {
						sortColumns( originalPropertyOrder );
					}
					sorted = true;
				}
				return originalPropertyOrder;
			}
			else {
				sorted = true;
			}
		}
		return null;
	}

	@Override
	public void createForeignKey(PersistentClass referencedEntity, AnnotatedJoinColumns joinColumns) {
		// Ensure properties are sorted before we create a foreign key
		sortProperties();
		if ( isForeignKeyEnabled()
				&& referencedPropertyName == null
				&& !hasFormula()
				&& isConstrained() ) {
			final var firstColumn = joinColumns.getJoinColumns().get( 0 );
			final Object owner = findReferencedColumnOwner( referencedEntity, firstColumn, getBuildingContext() );
			if ( owner instanceof Join join ) {
				// Here we handle the case of a foreign key that refers to the
				// primary key of a secondary table of the referenced entity
				final var foreignKey = getTable().createForeignKey(
						getForeignKeyName(),
						getConstraintColumns(),
						referencedEntity.getEntityName(),
						getForeignKeyDefinition(),
						getForeignKeyOptions(),
						join.getKey().getColumns()
				);
				foreignKey.setOnDeleteAction( getOnDeleteAction() );
				foreignKey.setReferencedTable( join.getTable() );
			}
			else {
				// it's just a reference to the primary key of the main table
				createForeignKeyOfEntity( referencedEntity.getEntityName() );
			}
		}
	}

	@Override
	public void createForeignKey() {
		// Ensure properties are sorted before we create a foreign key
		sortProperties();
		// A non-null referencedPropertyName tells us that the foreign key
		// does not reference the primary key, but some other unique key of
		// the referenced table. We do not handle this case here:
		// - For ManyToOne, the case of a foreign key to something other than
		//   the primary key is handled in createPropertyRefConstraints()
		// - For OneToOne, we still need to add some similar logic somewhere
		//   (for now, no foreign key constraint is created)
		if ( isForeignKeyEnabled() && referencedPropertyName==null && !hasFormula() ) {
			createForeignKeyOfEntity( ( (EntityType) getType() ).getAssociatedEntityName() );
		}
	}
}
