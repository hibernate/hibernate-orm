// $Id: Property.java 5814 2005-02-21 02:08:46Z oneovthafew $
package org.hibernate.tuple;

import org.hibernate.type.Type;

import java.io.Serializable;

/**
 * Defines the basic contract of a Property within the runtime metamodel.
 *
 * @author Steve Ebersole
 */
public abstract class Property implements Serializable {
	private String name;
	private String node;
	private Type type;

	/**
	 * Constructor for Property instances.
	 *
	 * @param name The name by which the property can be referenced within
	 * its owner.
	 * @param node The node name to use for XML-based representation of this
	 * property.
	 * @param type The Hibernate Type of this property.
	 */
	protected Property(String name, String node, Type type) {
		this.name = name;
		this.node = node;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getNode() {
		return node;
	}

	public Type getType() {
		return type;
	}
	
	public String toString() {
		return "Property(" + name + ':' + type.getName() + ')';
	}

}
