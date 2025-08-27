/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;

/**
 * Contract that represents a secondary table join that is part of an entity hierarchy.
 *
 * @author Chris Cranford
 */
public class Join implements AttributeContainer, Bindable<JaxbHbmSecondaryTableType> {

	private final List<Column> keyColumns;
	private final List<Attribute> attributes;

	private boolean inverse;
	private boolean optional;
	private String tableName;
	private String schema;
	private String catalog;

	public Join(String catalogName, String schemaName, String tableName) {
		this.catalog = catalogName;
		this.schema = schemaName;
		this.tableName = tableName;
		this.keyColumns = new ArrayList<>();
		this.attributes = new ArrayList<>();
	}

	@Override
	public void addAttribute(Attribute attribute) {
		this.attributes.add( attribute );
	}

	public void setTable(String tableName) {
		this.tableName = tableName;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public void setInverse(boolean inverse) {
		this.inverse = inverse;
	}

	public void addKeyColumn(Column keyColumn) {
		this.keyColumns.add( keyColumn );
	}

	public void addKeyColumnsFromValue(Value value) {
		final List<Selectable> selectables = value.getSelectables();
		for ( Selectable s : selectables ) {
			keyColumns.add( Column.from( s ) );
		}
	}

	@Override
	public JaxbHbmSecondaryTableType build() {
		final JaxbHbmSecondaryTableType join = new JaxbHbmSecondaryTableType();

		if ( !StringTools.isEmpty( catalog ) ) {
			join.setCatalog( catalog );
		}

		if ( !StringTools.isEmpty( schema ) ) {
			join.setSchema( schema );
		}

		join.setTable( tableName );
		join.setOptional( optional );
		join.setInverse( inverse );

		final JaxbHbmKeyType key = new JaxbHbmKeyType();
		join.setKey( key );

		for ( Column keyColumn : keyColumns ) {
			key.getColumn().add( keyColumn.build() );
		}

		for ( Attribute attribute : attributes ) {
			join.getAttributes().add( attribute.build() );
		}

		return join;
	}
}
