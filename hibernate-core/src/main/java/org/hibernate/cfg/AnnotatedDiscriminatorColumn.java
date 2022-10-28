/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.boot.spi.MetadataBuildingContext;

import static org.hibernate.cfg.BinderHelper.isEmptyAnnotationValue;

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

	public AnnotatedDiscriminatorColumn() {
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

	public static AnnotatedDiscriminatorColumn buildDiscriminatorColumn(
			DiscriminatorType type,
			DiscriminatorColumn discAnn,
			DiscriminatorFormula discFormulaAnn,
			MetadataBuildingContext context) {
		final AnnotatedDiscriminatorColumn discriminatorColumn = new AnnotatedDiscriminatorColumn();
		discriminatorColumn.setBuildingContext( context );
		if ( discFormulaAnn != null ) {
			discriminatorColumn.setImplicit( false );
			discriminatorColumn.setFormula( discFormulaAnn.value() );
		}
		else if ( discAnn != null ) {
			discriminatorColumn.setImplicit( false );
			if ( !isEmptyAnnotationValue( discAnn.columnDefinition() ) ) {
				discriminatorColumn.setSqlType( discAnn.columnDefinition() );
			}
			if ( !isEmptyAnnotationValue( discAnn.name() ) ) {
				discriminatorColumn.setLogicalColumnName( discAnn.name() );
			}
			discriminatorColumn.setNullable( false );
		}
		else {
			discriminatorColumn.setImplicit( true );
		}
		setDiscriminatorType( type, discAnn, discriminatorColumn );
		discriminatorColumn.bind();
		return discriminatorColumn;
	}

	private static void setDiscriminatorType(
			DiscriminatorType type,
			DiscriminatorColumn discAnn,
			AnnotatedDiscriminatorColumn discriminatorColumn) {
		if ( type == null ) {
			discriminatorColumn.setDiscriminatorTypeName( "string" );
		}
		else {
			switch ( type ) {
				case CHAR:
					discriminatorColumn.setDiscriminatorTypeName( "character" );
					discriminatorColumn.setImplicit( false );
					break;
				case INTEGER:
					discriminatorColumn.setDiscriminatorTypeName( "integer" );
					discriminatorColumn.setImplicit( false );
					break;
				case STRING:
					discriminatorColumn.setDiscriminatorTypeName( "string" );
					if ( discAnn != null ) {
						discriminatorColumn.setLength( (long) discAnn.length() );
					}
					break;
				default:
					throw new AssertionFailure( "Unknown discriminator type: " + type );
			}
		}
	}
}
