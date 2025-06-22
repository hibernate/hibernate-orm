/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType;

/**
 * Contract for a basic, singular attribute.
 *
 * @author Chris Cranford
 */
public class BasicAttribute implements SingularAttribute, Keyable {

	private final List<Column> columns;
	private final boolean updatable;
	private final String explicitType;

	private String name;
	private final String type;
	private boolean insertable;
	private boolean key;
	private TypeSpecification typeDefinition;

	/**
	 * Create a basic attribute
	 *
	 * @param name the attribute name
	 * @param type the attribute type
	 * @param insertable whether the attribute is insertable
	 * @param key whether the attribute participates in a key
	 */
	public BasicAttribute(String name, String type, boolean insertable, boolean key) {
		this( name, type, insertable, false, key );
	}

	/**
	 * Create a basic attribute
	 *
	 * @param name the attribute name
	 * @param type the attribute type
	 * @param insertable whether the attribute is insertable
	 * @param key whether the attribute participates in a key
	 * @param explicitType the explicit class type
	 */
	public BasicAttribute(String name, String type, boolean insertable, boolean key, String explicitType) {
		this( name, type, insertable, false, key, explicitType );
	}

	/**
	 * Create a basic attribute
	 *
	 * @param name the attribute name
	 * @param type the attribute type
	 * @param insertable whether the attribute is insertable
	 * @param updatable whether the attribute is updatable
	 * @param key whether the attribute participates in a key
	 */
	public BasicAttribute(String name, String type, boolean insertable, boolean updatable, boolean key) {
		this( name, type, insertable, updatable, key, null );
	}

	/**
	 * Creates a basic attribute
	 *
	 * @param name the attribute name
	 * @param type the attribute type
	 * @param insertable whether the attribute is insertable
	 * @param updatable whether the attribute is updatable
	 * @param key whether the attribute participates in a key
	 * @param explicitType the explicit class type
	 */
	public BasicAttribute(String name, String type, boolean insertable, boolean updatable, boolean key, String explicitType) {
		this.name = name;
		this.type = type;
		this.insertable = insertable;
		this.updatable = updatable;
		this.key = key;
		this.explicitType = explicitType;
		this.columns = new ArrayList<>();
	}

	/**
	 * A copy constructor that performs a deep-copy.
	 *
	 * @param other the object to be copied
	 */
	public BasicAttribute(BasicAttribute other) {
		this.name = other.name;
		this.type = other.type;
		this.explicitType = other.explicitType;
		this.insertable = other.insertable;
		this.updatable = other.updatable;
		this.key = other.key;
		if ( other.typeDefinition != null ) {
			this.typeDefinition = new TypeSpecification( other.typeDefinition );
		}

		this.columns = new ArrayList<>();
		other.columns.forEach( c -> this.columns.add( c.deepCopy() ) );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setKey(boolean key) {
		this.key = key;
	}

	@Override
	public boolean isKey() {
		return key;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.unmodifiableList( columns );
	}

	@Override
	public void addColumn(Column column) {
		this.columns.add( column );
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public TypeSpecification getType() {
		return typeDefinition;
	}

	public void setType(TypeSpecification type) {
		this.typeDefinition = type;
	}

	@Override
	public BasicAttribute deepCopy() {
		return new BasicAttribute( this );
	}

	@Override
	public Serializable build() {
		if ( key ) {
			return buildCompositeKeyBasicAttribute();
		}
		return buildBasicAttribute();
	}

	private JaxbHbmCompositeKeyBasicAttributeType buildCompositeKeyBasicAttribute() {
		// Define key-based, basic attribute.
		// Attribute is always assumed insertable and updatable.
		final JaxbHbmCompositeKeyBasicAttributeType basic = new JaxbHbmCompositeKeyBasicAttributeType();
		basic.setName( name );
		basic.setTypeAttribute( resolveType() );

		for ( Column column : columns ) {
			basic.getColumn().add( column.build() );
		}

		if ( typeDefinition != null ) {
			basic.setType( typeDefinition.build() );
		}

		return basic;
	}

	private JaxbHbmBasicAttributeType buildBasicAttribute() {
		// Define basic, non-key attribute.
		final JaxbHbmBasicAttributeType basic = new JaxbHbmBasicAttributeType();
		basic.setName( name );
		basic.setTypeAttribute( resolveType() );
		basic.setInsert( insertable );
		basic.setUpdate( updatable );

		for ( Column column : columns ) {
			basic.getColumnOrFormula().add( column.build() );
		}

		if ( typeDefinition != null ) {
			basic.setType( typeDefinition.build() );
		}

		return basic;
	}

	private String resolveType() {
		return explicitType == null ? type : explicitType;
	}
}
