/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

/**
 * Container for the properties defined by {@link javax.persistence.Column} or {@link javax.persistence.JoinColumn}.
 *
 * @author Hardy Ferentschik
 */
public class Column {
	private String name = "";
	private String table = null;

	private Boolean unique;
	private Boolean nullable;
	private Boolean insertable;
	private Boolean updatable;

	private int length = 255;
	private int precision = 0;
	private int scale = 0;

	private String columnDefinition; // used for DDL creation

	private String referencedColumnName; // from @JoinColumn
	
	public Column() {
		
	}

	public Column(AnnotationInstance columnAnnotation) {
		applyCheck( columnAnnotation );
		applyColumnValues( columnAnnotation );
	}

	// todo : can we call applyCheck from within applyColumnValues itself?
	// 		so that checks are applied when we apply overrides too

	protected void applyCheck(AnnotationInstance columnAnnotation) {
		if ( columnAnnotation != null ) {
			DotName name = columnAnnotation.name();
			if ( !( JPADotNames.COLUMN.equals( name )
					|| JPADotNames.JOIN_COLUMN.equals( name )
					|| JPADotNames.ORDER_COLUMN.equals( name )
					|| JPADotNames.PRIMARY_KEY_JOIN_COLUMN.equals( name )
					|| JPADotNames.MAP_KEY_COLUMN.equals( name )
			) ) {
				throw new AssertionFailure( "A @Column or @JoinColumn annotation needs to be passed to the constructor" );

			}
		}
	}

	public void applyColumnValues(AnnotationInstance columnAnnotation) {
		// if the column annotation is null we don't have to do anything. Everything is already defaulted.
		if ( columnAnnotation == null ) {
			return;
		}

		AnnotationValue nameValue = columnAnnotation.value( "name" );
		if ( nameValue != null ) {
			this.name = nameValue.asString();
		}

		AnnotationValue uniqueValue = columnAnnotation.value( "unique" );
		if ( uniqueValue != null ) {
			this.unique = uniqueValue.asBoolean();
		}

		AnnotationValue nullableValue = columnAnnotation.value( "nullable" );
		if ( nullableValue != null ) {
			this.nullable = nullableValue.asBoolean();
		}

		AnnotationValue insertableValue = columnAnnotation.value( "insertable" );
		if ( insertableValue != null ) {
			this.insertable = insertableValue.asBoolean();
		}

		AnnotationValue updatableValue = columnAnnotation.value( "updatable" );
		if ( updatableValue != null ) {
			this.updatable = updatableValue.asBoolean();
		}

		AnnotationValue columnDefinition = columnAnnotation.value( "columnDefinition" );
		if ( columnDefinition != null ) {
			this.columnDefinition = columnDefinition.asString();
		}

		AnnotationValue tableValue = columnAnnotation.value( "table" );
		if ( tableValue != null ) {
			this.table = tableValue.asString();
		}

		AnnotationValue lengthValue = columnAnnotation.value( "length" );
		if ( lengthValue != null ) {
			this.length = lengthValue.asInt();
		}

		AnnotationValue precisionValue = columnAnnotation.value( "precision" );
		if ( precisionValue != null ) {
			this.precision = precisionValue.asInt();
		}

		AnnotationValue scaleValue = columnAnnotation.value( "scale" );
		if ( scaleValue != null ) {
			this.scale = scaleValue.asInt();
		}

		AnnotationValue referencedColumnNameValue = columnAnnotation.value( "referencedColumnName" );
		if ( referencedColumnNameValue != null ) {
			this.referencedColumnName = referencedColumnNameValue.asString();
		}
	}

	public final String getName() {
		return name;
	}

	public final Boolean isUnique() {
		return unique;
	}

	public final Boolean isNullable() {
		return nullable;
	}

	public final Boolean isInsertable() {
		return insertable;
	}

	public final Boolean isUpdatable() {
		return updatable;
	}

	public final String getColumnDefinition() {
		return columnDefinition;
	}

	public final String getTable() {
		return table;
	}

	public final int getLength() {
		return length;
	}

	public final int getPrecision() {
		return precision;
	}

	public final int getScale() {
		return scale;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setUnique(Boolean unique) {
		this.unique = unique;
	}

	public void setNullable(Boolean nullable) {
		this.nullable = nullable;
	}

	public void setInsertable(Boolean insertable) {
		this.insertable = insertable;
	}

	public void setUpdatable(Boolean updatable) {
		this.updatable = updatable;
	}

	public void setColumnDefinition(String columnDefinition) {
		this.columnDefinition = columnDefinition;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public String getReferencedColumnName() {
		return referencedColumnName;
	}

	public void setReferencedColumnName(String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ColumnValues" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( ", table='" ).append( table ).append( '\'' );
		sb.append( ", unique=" ).append( unique );
		sb.append( ", nullable=" ).append( nullable );
		sb.append( ", insertable=" ).append( insertable );
		sb.append( ", updatable=" ).append( updatable );
		sb.append( ", length=" ).append( length );
		sb.append( ", precision=" ).append( precision );
		sb.append( ", scale=" ).append( scale );
		sb.append( ", columnDefinition='" ).append( columnDefinition ).append( '\'' );
		sb.append( ", referencedColumnName='" ).append( referencedColumnName ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Column that = (Column) o;

		if ( insertable != that.insertable ) {
			return false;
		}
		if ( length != that.length ) {
			return false;
		}
		if ( nullable != that.nullable ) {
			return false;
		}
		if ( precision != that.precision ) {
			return false;
		}
		if ( scale != that.scale ) {
			return false;
		}
		if ( unique != that.unique ) {
			return false;
		}
		if ( updatable != that.updatable ) {
			return false;
		}
		if ( columnDefinition != null ? !columnDefinition.equals( that.columnDefinition ) : that.columnDefinition != null ) {
			return false;
		}
		if ( name != null ? !name.equals( that.name ) : that.name != null ) {
			return false;
		}
		if ( referencedColumnName != null ? !referencedColumnName.equals( that.referencedColumnName ) : that.referencedColumnName != null ) {
			return false;
		}
		if ( table != null ? !table.equals( that.table ) : that.table != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( table != null ? table.hashCode() : 0 );
		result = 31 * result + ( unique ? 1 : 0 );
		result = 31 * result + ( nullable ? 1 : 0 );
		result = 31 * result + ( insertable ? 1 : 0 );
		result = 31 * result + ( updatable ? 1 : 0 );
		result = 31 * result + length;
		result = 31 * result + precision;
		result = 31 * result + scale;
		result = 31 * result + ( columnDefinition != null ? columnDefinition.hashCode() : 0 );
		result = 31 * result + ( referencedColumnName != null ? referencedColumnName.hashCode() : 0 );
		return result;
	}
}


