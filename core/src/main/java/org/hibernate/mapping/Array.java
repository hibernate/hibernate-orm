//$Id: Array.java 5793 2005-02-20 03:34:50Z oneovthafew $
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.type.CollectionType;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ReflectHelper;

/**
 * An array mapping has a primary key consisting of
 * the key columns + index column.
 * @author Gavin King
 */
public class Array extends List {

	private String elementClassName;

	/**
	 * Constructor for Array.
	 * @param owner
	 */
	public Array(PersistentClass owner) {
		super(owner);
	}

	public Class getElementClass() throws MappingException {
		if (elementClassName==null) {
			org.hibernate.type.Type elementType = getElement().getType();
			return isPrimitiveArray() ?
				( (PrimitiveType) elementType ).getPrimitiveClass() :
				elementType.getReturnedClass();
		}
		else {
			try {
				return ReflectHelper.classForName(elementClassName);
			}
			catch (ClassNotFoundException cnfe) {
				throw new MappingException(cnfe);
			}
		}
	}

	public CollectionType getDefaultCollectionType() throws MappingException {
		return TypeFactory.array( getRole(), getReferencedPropertyName(), isEmbedded(), getElementClass() );
	}

	public boolean isArray() {
		return true;
	}

	/**
	 * @return Returns the elementClassName.
	 */
	public String getElementClassName() {
		return elementClassName;
	}
	/**
	 * @param elementClassName The elementClassName to set.
	 */
	public void setElementClassName(String elementClassName) {
		this.elementClassName = elementClassName;
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
