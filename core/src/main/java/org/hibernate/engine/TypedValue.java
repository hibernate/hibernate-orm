//$Id: TypedValue.java 6638 2005-05-02 14:34:17Z oneovthafew $
package org.hibernate.engine;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.type.Type;

/**
 * An ordered pair of a value and its Hibernate type.
 * 
 * @see org.hibernate.type.Type
 * @author Gavin King
 */
public final class TypedValue implements Serializable {
	private final Type type;
	private final Object value;
	private final EntityMode entityMode;

	public TypedValue(Type type, Object value, EntityMode entityMode) {
		this.type = type;
		this.value=value;
		this.entityMode = entityMode;
	}

	public Object getValue() {
		return value;
	}

	public Type getType() {
		return type;
	}

	public String toString() {
		return value==null ? "null" : value.toString();
	}

	public int hashCode() {
		//int result = 17;
		//result = 37 * result + type.hashCode();
		//result = 37 * result + ( value==null ? 0 : value.hashCode() );
		//return result;
		return value==null ? 0 : type.getHashCode(value, entityMode);
	}

	public boolean equals(Object other) {
		if ( !(other instanceof TypedValue) ) return false;
		TypedValue that = (TypedValue) other;
		/*return that.type.equals(type) && 
			EqualsHelper.equals(that.value, value);*/
		return type.getReturnedClass() == that.type.getReturnedClass() &&
			type.isEqual(that.value, value, entityMode);
	}

}





