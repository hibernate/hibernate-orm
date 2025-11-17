/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.xml2;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class NamedAnyContainer {
	private Integer id;
	private String name;
	private NamedProperty specificProperty;
	private Set<NamedProperty> generalProperties;

	public NamedAnyContainer() {
	}

	public NamedAnyContainer(Integer id, String name) {
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

	public NamedProperty getSpecificProperty() {
		return specificProperty;
	}

	public void setSpecificProperty(NamedProperty specificProperty) {
		this.specificProperty = specificProperty;
	}

	public Set<NamedProperty> getGeneralProperties() {
		return generalProperties;
	}

	public void setGeneralProperties(Set<NamedProperty> generalProperties) {
		this.generalProperties = generalProperties;
	}

	public void addGeneralProperty(NamedProperty property) {
		if ( generalProperties == null ) {
			generalProperties = new LinkedHashSet<>();
		}
		generalProperties.add( property );
	}
}
