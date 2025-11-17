/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.mapping.Selectable;

/**
 * An implementation of {@link Selection} that represents a logical column.
 *
 * @author Chris Cranford
 */
public class Column extends Selection<JaxbHbmColumnType> implements Cloneable<Column> {

	private final Long length;
	private final Integer scale;
	private final Integer precision;
	private final String sqlType;
	private final String read;
	private final String write;
	private String name;

	/**
	 * Create a column with just a name.
	 *
	 * @param name the name of the column
	 */
	public Column(String name) {
		this( name, false );
	}

	/**
	 * Create a column with just a name.
	 *
	 * @param name the name of the column
	 * @param quoted whether the name is to be quoted or not
	 */
	public Column(String name, boolean quoted) {
		this( name, null, null, null, null, null, null, quoted );
	}

	/**
	 * Creates a column without a non-quoted name.
	 *
	 * @param name the name of the column, never {@code null}
	 * @param length the length, may be {@code null}
	 * @param scale the scale, may be {@code null}
	 * @param precision the precision, may be {@code null}
	 * @param sqlType the sql-type, may be {@code null}
	 * @param read the custom read, may be {@code null}
	 * @param write the custom write, may be {@code null}
	 */
	public Column(String name, Long length, Integer scale, Integer precision, String sqlType, String read, String write) {
		this( name, length, scale, precision, sqlType, read, write, false );
	}

	/**
	 * Creates a column
	 *
	 * @param name the name of the column, never {@code null}
	 * @param length the length, may be {@code null}
	 * @param scale the scale, may be {@code null}
	 * @param precision the precision, may be {@code null}
	 * @param sqlType the sql-type, may be {@code null}
	 * @param read the custom read, may be {@code null}
	 * @param write the custom write, may be {@code null}
	 * @param quoted whether to quote the column name or not
	 */
	public Column(String name, Long length, Integer scale, Integer precision, String sqlType, String read, String write, boolean quoted) {
		super ( SelectionType.COLUMN );
		this.name = quoted ? getQuotedName( name ) : name;
		this.length = length;
		this.scale = scale;
		this.precision = precision;
		this.sqlType = sqlType;
		this.read = read;
		this.write = write;
	}

	/**
	 * Copy constructor that performs a deep-copy.
	 *
	 * @param other the column to copy
	 */
	public Column(Column other) {
		super( other.getSelectionType() );
		this.name = other.name;
		this.length = other.length;
		this.scale = other.scale;
		this.precision = other.precision;
		this.sqlType = other.sqlType;
		this.read = other.read;
		this.write = other.write;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Column deepCopy() {
		return new Column( this );
	}

	@Override
	public JaxbHbmColumnType build() {
		final JaxbHbmColumnType column = new JaxbHbmColumnType();
		column.setName( name );

		if ( length != null ) {
			column.setLength( length.intValue() );
		}

		if ( scale != null ) {
			column.setScale( scale );
		}

		if ( precision != null ) {
			column.setPrecision( precision );
		}

		if ( !StringTools.isEmpty( sqlType ) ) {
			column.setSqlType( sqlType );
		}

		if ( !StringTools.isEmpty( read ) ) {
			column.setRead( read );
		}

		if ( !StringTools.isEmpty( write ) ) {
			column.setWrite( write );
		}

		return column;
	}

	/**
	 * Creates an Envers column mapping from an Hibernate ORM column mapping.
	 *
	 * @param selectable the ORM column mapping
	 * @return the envers column mapping
	 */
	public static Column from(Selectable selectable) {
		if ( !( selectable instanceof org.hibernate.mapping.Column ) ) {
			throw new EnversMappingException( "Cannot create audit column mapping from " + selectable.getClass().getName() );
		}
		final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) selectable;
		return new Column(
				column.getName(),
				column.getLength(),
				column.getScale(),
				column.getPrecision(),
				column.getSqlType(),
				column.getCustomRead(),
				column.getCustomWrite(),
				column.isQuoted()
		);
	}

	/**
	 * Returns the specified name quoted.
	 *
	 * @param name the name to be quoted, never {@code null}
	 * @return the quoted column name
	 */
	private static String getQuotedName(String name) {
		return "`" + name + "`";
	}
}
