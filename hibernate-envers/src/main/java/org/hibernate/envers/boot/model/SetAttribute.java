/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.io.Serializable;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleWithSubselectEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyWithExtraEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.envers.boot.EnversMappingException;

/**
 * A plural attribute that represents a {@code set}.
 *
 * This attribute binds Hibernate's persistence model to a {@link JaxbHbmSetType} mapping that
 * will be contributed by Envers back to Hibernate for the audit entity mappings.  This does
 * not bind all JAXB model attributes, only those which are applicable to Envers's metamodel.
 *
 * @author Chris Cranford
 */
public class SetAttribute implements PluralAttribute {

	private final String tableName;
	private final String schemaName;
	private final String catalogName;

	private String name;
	private String cascade;
	private String fetch;
	private String keyColumn;
	private String elementType;
	private String columnName;
	private String lazy;

	public SetAttribute(String name, String tableName, String schemaName, String catalogName) {
		this.name = name;
		this.tableName = tableName;
		this.schemaName = schemaName;
		this.catalogName = catalogName;
	}

	public void setCascade(String cascade) {
		this.cascade = cascade;
	}

	public void setFetch(String fetch) {
		this.fetch = fetch;
	}

	public void setKeyColumn(String keyColumn) {
		this.keyColumn = keyColumn;
	}

	public void setElementType(String elementType) {
		this.elementType = elementType;
	}

	public void setLazy(String lazy) {
		this.lazy = lazy;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	// todo: can these be removed somehow?

	@Override
	public List<Column> getColumns() {
		throw new EnversMappingException( "Operation not supported" );
	}

	@Override
	public void addColumn(Column column) {
		throw new EnversMappingException( "Operation not supported" );
	}

	@Override
	public Attribute deepCopy() {
		throw new EnversMappingException( "Operation not supported" );
	}

	@Override
	public Serializable build() {
		// Initialize the set
		final JaxbHbmSetType set = new JaxbHbmSetType();
		set.setName( name );
		set.setTable( tableName );
		set.setSchema( schemaName );
		set.setCatalog( catalogName );
		set.setCascade( cascade );
		set.setFetch( JaxbHbmFetchStyleWithSubselectEnum.fromValue( fetch ) );
		set.setLazy( JaxbHbmLazyWithExtraEnum.fromValue( lazy ) );

		// Initialize the set's key
		final JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute( keyColumn );
		set.setKey( key );

		// Initialize the set's element
		final JaxbHbmBasicCollectionElementType element = new JaxbHbmBasicCollectionElementType();
		element.setTypeAttribute( elementType );
		element.setColumnAttribute( columnName );
		set.setElement( element );

		return set;
	}
}
