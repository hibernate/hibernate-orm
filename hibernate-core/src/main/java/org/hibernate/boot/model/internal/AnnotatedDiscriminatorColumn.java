/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.spi.MetadataBuildingContext;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;

/**
 * A {@link jakarta.persistence.DiscriminatorColumn} annotation
 *
 * @author Emmanuel Bernard
 */
public class AnnotatedDiscriminatorColumn extends AnnotatedColumn {
	public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "DTYPE";
	public static final String DEFAULT_DISCRIMINATOR_TYPE = "string";
	private static final long DEFAULT_DISCRIMINATOR_LENGTH = 31;

	private String discriminatorTypeName;

	public AnnotatedDiscriminatorColumn(String defaultColumnName) {
		//discriminator default value
		super();
		setLogicalColumnName( defaultColumnName );
		setNullable( false );
		setDiscriminatorTypeName( DEFAULT_DISCRIMINATOR_TYPE );
		setLength( DEFAULT_DISCRIMINATOR_LENGTH );
	}

	public String getDiscriminatorTypeName() {
		return discriminatorTypeName;
	}

	public void setDiscriminatorTypeName(String discriminatorTypeName) {
		this.discriminatorTypeName = discriminatorTypeName;
	}

	public static AnnotatedDiscriminatorColumn buildDiscriminatorColumn(
			DiscriminatorColumn discriminatorColumn,
			DiscriminatorFormula discriminatorFormula,
			Column columnOverride,
			String defaultColumnName,
			MetadataBuildingContext context) {
		final AnnotatedColumns parent = new AnnotatedColumns();
		parent.setBuildingContext( context );
		final AnnotatedDiscriminatorColumn column = new AnnotatedDiscriminatorColumn( defaultColumnName );
		final DiscriminatorType discriminatorType;
		if ( discriminatorFormula != null ) {
			final DiscriminatorType type = discriminatorFormula.discriminatorType();
			if ( type == DiscriminatorType.STRING ) {
				discriminatorType = discriminatorColumn == null ? type : discriminatorColumn.discriminatorType();
			}
			else {
				discriminatorType = type;
			}
			column.setImplicit( false );
			column.setFormula( discriminatorFormula.value() );
		}
		else if ( discriminatorColumn != null ) {
			discriminatorType = discriminatorColumn.discriminatorType();
			column.setImplicit( false );
			if ( !discriminatorColumn.columnDefinition().isBlank() ) {
				column.setSqlType( discriminatorColumn.columnDefinition() );
			}
			if ( !discriminatorColumn.name().isBlank() ) {
				column.setLogicalColumnName( discriminatorColumn.name() );
			}
			column.setNullable( false );
			column.setOptions( discriminatorColumn.options() );
		}
		else {
			discriminatorType = DiscriminatorType.STRING;
			column.setImplicit( true );
		}
		if ( columnOverride != null ) {
			column.setLogicalColumnName( columnOverride.name() );

			final String columnDefinition = columnOverride.columnDefinition();
			if ( !columnDefinition.isBlank() ) {
				column.setSqlType( columnDefinition );
			}
		}
		setDiscriminatorType( discriminatorType, discriminatorColumn, columnOverride, column );
		column.setParent( parent );
		column.bind();
		return column;
	}

	private static void setDiscriminatorType(
			DiscriminatorType type,
			DiscriminatorColumn discriminatorColumn,
			Column columnOverride,
			AnnotatedDiscriminatorColumn column) {
		if ( type == null ) {
			column.setDiscriminatorTypeName( "string" );
		}
		else {
			switch ( type ) {
				case CHAR:
					column.setDiscriminatorTypeName( "character" );
					column.setImplicit( false );
					column.setLength( 1L );
					break;
				case INTEGER:
					column.setDiscriminatorTypeName( "integer" );
					column.setImplicit( false );
					break;
				case STRING:
					column.setDiscriminatorTypeName( "string" );
					if ( columnOverride != null ) {
						column.setLength( (long) columnOverride.length() );
					}
					else if ( discriminatorColumn != null ) {
						column.setLength( (long) discriminatorColumn.length() );
					}
					break;
				default:
					throw new AssertionFailure( "Unknown discriminator type: " + type );
			}
		}
	}
}
