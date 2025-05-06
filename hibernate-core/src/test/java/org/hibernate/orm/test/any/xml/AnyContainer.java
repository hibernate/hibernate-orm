/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.xml;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public class AnyContainer {
	private Integer id;
	private String name;
	private PropertyValue someSpecificProperty;
	private Map<String,PropertyValue> generalProperties;

	public AnyContainer() {
	}

	public AnyContainer(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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

	public Map<String, PropertyValue> getGeneralProperties() {
		return generalProperties;
	}

	public void setGeneralProperties(Map<String, PropertyValue> generalProperties) {
		this.generalProperties = generalProperties;
	}
}
