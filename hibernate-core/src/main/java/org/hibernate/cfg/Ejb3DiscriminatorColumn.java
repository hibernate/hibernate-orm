/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.DiscriminatorFormula;

/**
 * Discriminator column
 *
 * @author Emmanuel Bernard
 */
public class Ejb3DiscriminatorColumn extends Ejb3Column {
	public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "DTYPE";
	public static final String DEFAULT_DISCRIMINATOR_TYPE = "string";
	private static final int DEFAULT_DISCRIMINATOR_LENGTH = 31;

	private String discriminatorTypeName;

	public Ejb3DiscriminatorColumn() {
		//discriminator default value
		super();
		setLogicalColumnName( DEFAULT_DISCRIMINATOR_COLUMN_NAME );
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

	public static Ejb3DiscriminatorColumn buildDiscriminatorColumn(
			DiscriminatorType type, DiscriminatorColumn discAnn,
			DiscriminatorFormula discFormulaAnn,
			Mappings mappings) {
		Ejb3DiscriminatorColumn discriminatorColumn = new Ejb3DiscriminatorColumn();
		discriminatorColumn.setMappings( mappings );
		discriminatorColumn.setImplicit( true );
		if ( discFormulaAnn != null ) {
			discriminatorColumn.setImplicit( false );
			discriminatorColumn.setFormula( discFormulaAnn.value() );
		}
		else if ( discAnn != null ) {
			discriminatorColumn.setImplicit( false );
			if ( !BinderHelper.isEmptyAnnotationValue( discAnn.columnDefinition() ) ) {
				discriminatorColumn.setSqlType(
						discAnn.columnDefinition()
				);
			}
			if ( !BinderHelper.isEmptyAnnotationValue( discAnn.name() ) ) {
				discriminatorColumn.setLogicalColumnName( discAnn.name() );
			}
			discriminatorColumn.setNullable( false );
		}
		if ( DiscriminatorType.CHAR.equals( type ) ) {
			discriminatorColumn.setDiscriminatorTypeName( "character" );
			discriminatorColumn.setImplicit( false );
		}
		else if ( DiscriminatorType.INTEGER.equals( type ) ) {
			discriminatorColumn.setDiscriminatorTypeName( "integer" );
			discriminatorColumn.setImplicit( false );
		}
		else if ( DiscriminatorType.STRING.equals( type ) || type == null ) {
			if ( discAnn != null ) discriminatorColumn.setLength( discAnn.length() );
			discriminatorColumn.setDiscriminatorTypeName( "string" );
		}
		else {
			throw new AssertionFailure( "Unknown discriminator type: " + type );
		}
		discriminatorColumn.bind();
		return discriminatorColumn;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Ejb3DiscriminatorColumn" );
		sb.append( "{logicalColumnName'" ).append( getLogicalColumnName() ).append( '\'' );
		sb.append( ", discriminatorTypeName='" ).append( discriminatorTypeName ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}
