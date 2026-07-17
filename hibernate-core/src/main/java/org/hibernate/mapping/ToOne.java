/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.FetchStyle;
import org.hibernate.type.MappingContext;

import java.util.Objects;
import java.util.function.Function;

import static org.hibernate.internal.util.ReflectHelper.reflectedPropertyClass;

/**
 * A mapping model object representing an association where the target side has cardinality one.
 *
 * @author Gavin King
 */
public abstract sealed class ToOne
		extends SimpleValue implements Fetchable, SortableValue
		permits OneToOne, ManyToOne {

	private FetchStyle fetchStyle;
	protected String referencedPropertyName;
	private String referencedEntityName;
	private String propertyName;
	private boolean lazy = true;
	private boolean sorted;
	private boolean unwrapProxy;
	private boolean unwrapProxyImplicit;
	private boolean referenceToPrimaryKey = true;
	private ForeignKeyColumnMappings foreignKeyColumnMappings;

	protected ToOne(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
	}

	protected ToOne(ToOne original) {
		super( original );
		this.fetchStyle = original.fetchStyle;
		this.referencedPropertyName = original.referencedPropertyName;
		this.referencedEntityName = original.referencedEntityName;
		this.propertyName = original.propertyName;
		this.lazy = original.lazy;
		this.sorted = original.sorted;
		this.unwrapProxy = original.unwrapProxy;
		this.unwrapProxyImplicit = original.unwrapProxyImplicit;
		this.referenceToPrimaryKey = original.referenceToPrimaryKey;
		this.foreignKeyColumnMappings = original.foreignKeyColumnMappings;
	}

	@Override
	public FetchStyle getFetchStyle() {
		return fetchStyle;
	}

	@Override
	public void setFetchStyle(FetchStyle fetchStyle) {
		this.fetchStyle = fetchStyle;
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

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName==null ?
				null : propertyName.intern();
	}

	@Override
	public void setTypeUsingReflection(
			String className,
			String propertyName,
			MetadataBuildingContext buildingContext) throws MappingException {
		if ( referencedEntityName == null ) {
			referencedEntityName = reflectedPropertyClass(
					className,
					propertyName,
					buildingContext.getClassLoaderService()
			).getName();
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

	public ForeignKeyColumnMappings getForeignKeyColumnMappings() {
		return foreignKeyColumnMappings;
	}

	public void setForeignKeyColumnMappings(ForeignKeyColumnMappings foreignKeyColumnMappings) {
		this.foreignKeyColumnMappings = foreignKeyColumnMappings;
	}

	@Override
	public boolean isSorted() {
		return sorted;
	}

	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}

	@Override
	public int[] sortProperties(Function<String, PersistentClass> entityBindingResolver) {
		final var entityBinding = entityBindingResolver == null ? null : entityBindingResolver.apply( referencedEntityName );
		if ( entityBinding != null ) {
			final var value =
					referencedPropertyName == null
							? entityBinding.getIdentifier()
							: entityBinding.getRecursiveProperty( referencedPropertyName ).getValue();
			if ( value instanceof Component component ) {
				final var originalPropertyOrder = component.sortProperties();
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

	boolean isActuallyConstrained() {
		return isConstrained();
	}

}
