//$Id$
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


	private static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "DTYPE";
	private static final String DEFAULT_DISCRIMINATOR_TYPE = "string";

	private String discriminatorTypeName;
	private static final int DEFAULT_DISCRIMINATOR_LENGTH = 31;

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
			DiscriminatorType type, DiscriminatorColumn discAnn, DiscriminatorFormula discFormulaAnn,
			ExtendedMappings mappings
	) {
		Ejb3DiscriminatorColumn discriminatorColumn = new Ejb3DiscriminatorColumn();
		discriminatorColumn.setMappings( mappings );
		discriminatorColumn.setImplicit( true );
		if ( discFormulaAnn != null ) {
			discriminatorColumn.setImplicit( false );
			discriminatorColumn.setFormula( discFormulaAnn.value() );
		}
		else if ( discAnn != null ) {
			discriminatorColumn.setImplicit( false );
			if ( !BinderHelper.isDefault( discAnn.columnDefinition() ) ) {
				discriminatorColumn.setSqlType(
						discAnn.columnDefinition()
				);
			}
			if ( !BinderHelper.isDefault( discAnn.name() ) ) {
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
}
