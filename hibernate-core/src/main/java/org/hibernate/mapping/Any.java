/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.type.spi.BasicTypeProducer;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.type.spi.Type;

/**
 * A Hibernate "any" type (ie. polymorphic association to
 * one-of-several tables).
 * @author Gavin King
 */
public class Any extends SimpleValue {
	private String identifierTypeName;
	private String metaTypeName = "string";

	private BasicTypeProducer keyTypeProducer;

	private BasicTypeProducer discriminatorTypeProducer;
	private Map<Object,String> discriminatorMap;

	public Any(InFlightMetadataCollector metadata, Table table) {
		super( metadata, table );
	}

	public String getIdentifierType() {
		return identifierTypeName;
	}

	public void setIdentifierType(String identifierType) {
		this.identifierTypeName = identifierType;
	}

	public Type getType() throws MappingException {
		return getMetadata().any(
				keyTypeProducer.produceBasicType(),
				discriminatorTypeProducer.produceBasicType(),
				discriminatorMap
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
		return discriminatorMap;
	}

	public void setMetaValues(Map<Object,String> discriminatorMap) {
		this.discriminatorMap = discriminatorMap;
	}

	public void setTypeUsingReflection(String className, String propertyName)
		throws MappingException {
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public void setIdentifierTypeProducer(BasicTypeProducer keyTypeProducer) {
		this.keyTypeProducer = keyTypeProducer;
	}

	public void setDiscriminatorTypeProducer(BasicTypeProducer discriminatorTypeProducer) {
		this.discriminatorTypeProducer = discriminatorTypeProducer;
	}

	public void addDiscriminatorMapping(Object discriminatorValue, String mappedEntityName) {
		if ( discriminatorMap == null ) {
			discriminatorMap = new HashMap<>();
		}
		discriminatorMap.put( discriminatorValue, mappedEntityName );
	}
}
