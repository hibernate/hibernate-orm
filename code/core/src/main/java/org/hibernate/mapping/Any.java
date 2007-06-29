//$Id: Any.java 4905 2004-12-07 09:59:56Z maxcsaucdk $
package org.hibernate.mapping;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.type.MetaType;
import org.hibernate.type.AnyType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * A Hibernate "any" type (ie. polymorphic association to
 * one-of-several tables).
 * @author Gavin King
 */
public class Any extends SimpleValue {

	private String identifierTypeName;
	private String metaTypeName = "string";
	private Map metaValues;

	public Any(Table table) {
		super(table);
	}

	public String getIdentifierType() {
		return identifierTypeName;
	}

	public void setIdentifierType(String identifierType) {
		this.identifierTypeName = identifierType;
	}

	public Type getType() throws MappingException {
		return new AnyType(
			metaValues==null ?
				TypeFactory.heuristicType(metaTypeName) :
				new MetaType( metaValues, TypeFactory.heuristicType(metaTypeName) ),
				TypeFactory.heuristicType(identifierTypeName)
		);
	}

	public void setTypeByReflection(String propertyClass, String propertyName) {}

	public String getMetaType() {
		return metaTypeName;
	}

	public void setMetaType(String type) {
		metaTypeName = type;
	}

	public Map getMetaValues() {
		return metaValues;
	}

	public void setMetaValues(Map metaValues) {
		this.metaValues = metaValues;
	}

	public void setTypeUsingReflection(String className, String propertyName)
		throws MappingException {
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
