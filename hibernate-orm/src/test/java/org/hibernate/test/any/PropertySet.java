/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.any;
import java.util.HashMap;
import java.util.Map;

/**
 * todo: describe PropertySet
 *
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
