package org.hibernate.metamodel.source.annotations.attribute;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.source.annotations.JPADotNames;

/**
 * Contains the information about a single {@link javax.persistence.AttributeOverride}. Instances of this class
 * are creating during annotation processing and then applied onto the persistence attributes.
 *
 * @author Hardy Ferentschik
 */
public class AttributeOverride {
	private final ColumnValues columnValues;
	private final String attributePath;

	public AttributeOverride(AnnotationInstance attributeOverrideAnnotation) {
		this(null, attributeOverrideAnnotation);
	}

	public AttributeOverride(String prefix, AnnotationInstance attributeOverrideAnnotation) {
		if ( attributeOverrideAnnotation != null && !JPADotNames.ATTRIBUTE_OVERRIDE.equals( attributeOverrideAnnotation.name() ) ) {
			throw new AssertionFailure( "A @Column annotation needs to be passed to the constructor" );
		}




		columnValues = null;
		attributePath = "";
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AttributeOverride" );
		sb.append( "{columnValues=" ).append( columnValues );
		sb.append( ", attributePath='" ).append( attributePath ).append( '\'' );
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

		AttributeOverride that = (AttributeOverride) o;

		if ( attributePath != null ? !attributePath.equals( that.attributePath ) : that.attributePath != null ) {
			return false;
		}
		if ( columnValues != null ? !columnValues.equals( that.columnValues ) : that.columnValues != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = columnValues != null ? columnValues.hashCode() : 0;
		result = 31 * result + ( attributePath != null ? attributePath.hashCode() : 0 );
		return result;
	}
}


