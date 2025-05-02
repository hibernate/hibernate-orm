/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.xml;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class PropertySet {
	private Long id;
	private String name;
	private PropertyValue someSpecificProperty;
	private Map generalProperties = new HashMap();

	public PropertySet() {
	}

	public PropertySet(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PropertyValue getSomeSpecificProperty() {
		return someSpecificProperty;
	}

	public void setSomeSpecificProperty(PropertyValue someSpecificProperty) {
		this.someSpecificProperty = someSpecificProperty;
	}

	public Map getGeneralProperties() {
		return generalProperties;
	}

	public void setGeneralProperties(Map generalProperties) {
		this.generalProperties = generalProperties;
	}
}
