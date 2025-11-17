/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOnDeleteEnum;
import org.hibernate.envers.internal.tools.StringTools;

/**
 * Represents a plural attribute mapping of a {@code many-to-one} or {@code key-many-to-one}.
 *
 * @author Chris Cranford
 */
public class ManyToOneAttribute implements PluralAttribute, Keyable {

	private final List<Column> columns;

	private final String type;
	private final boolean insertable;
	private final boolean updatable;
	private boolean key;
	private final String clazz;
	private String onDelete;
	private String name;
	private String foreignKey;

	public ManyToOneAttribute(String name, String type, boolean insertable, boolean updatable, boolean key, String explicitType) {
		this.name = name;
		this.type = type;
		this.insertable = insertable;
		this.updatable = updatable;
		this.key = key;
		this.clazz = explicitType;
		this.columns = new ArrayList<>();
	}

	public ManyToOneAttribute(ManyToOneAttribute other) {
		this.name = other.name;
		this.type = other.type;
		this.insertable = other.insertable;
		this.updatable = other.updatable;
		this.key = other.key;
		this.clazz = other.clazz;
		this.onDelete = other.onDelete;
		this.foreignKey = other.foreignKey;

		this.columns = new ArrayList<>();
		for ( Column column : other.columns ) {
			this.columns.add( column.deepCopy() );
		}
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

	public String getOnDelete() {
		return onDelete;
	}

	public void setOnDelete(String onDelete) {
		this.onDelete = onDelete;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.unmodifiableList( columns );
	}

	@Override
	public void addColumn(Column column) {
		this.columns.add( column );
	}

	public void setForeignKey(String foreignKey) {
		this.foreignKey = foreignKey;
	}

	@Override
	public ManyToOneAttribute deepCopy() {
		return new ManyToOneAttribute( this );
	}

	@Override
	public Serializable build() {
		if ( key ) {
			return buildKeyManyToOne();
		}
		return buildManyToOne();
	}

	private JaxbHbmCompositeKeyManyToOneType buildKeyManyToOne() {
		final JaxbHbmCompositeKeyManyToOneType manyToOne = new JaxbHbmCompositeKeyManyToOneType();
		manyToOne.setName( name );
		manyToOne.setClazz( clazz );
		manyToOne.setOnDelete( getOnDeleteEnum() );

		if ( !StringTools.isEmpty( foreignKey ) ) {
			manyToOne.setForeignKey(foreignKey);
		}

		for ( Column column : columns ) {
			manyToOne.getColumn().add( column.build() );
		}

		return manyToOne;
	}

	private JaxbHbmManyToOneType buildManyToOne() {
		final JaxbHbmManyToOneType manyToOne = new JaxbHbmManyToOneType();
		manyToOne.setName( name );
		manyToOne.setClazz( clazz );
		manyToOne.setInsert( insertable );
		manyToOne.setUpdate( updatable );
		manyToOne.setOnDelete( getOnDeleteEnum() );

		if ( !StringTools.isEmpty( foreignKey ) ) {
			manyToOne.setForeignKey(foreignKey);
		}

		for ( Column column : columns ) {
			manyToOne.getColumnOrFormula().add( column.build() );
		}

		return manyToOne;
	}

	private JaxbHbmOnDeleteEnum getOnDeleteEnum() {
		if ( onDelete != null ) {
			return JaxbHbmOnDeleteEnum.fromValue( onDelete );
		}
		return null;
	}
}
