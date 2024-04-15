/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Cranford
 */
public class EntityOfDynamicComponent {
	private Long id;
	private String note;
	private Map values = new HashMap<>();
	private Map valuesWithProperties = new HashMap<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public Map getValues() {
		return values;
	}

	public void setValues(Map values) {
		this.values = values;
	}

	public Map getValuesWithProperties() {
		return valuesWithProperties;
	}

	public void setValuesWithProperties(Map valuesWithProperties) {
		this.valuesWithProperties = valuesWithProperties;
	}

	@Override
	public String toString() {
		return "EntityOfDynamicComponent{" +
				"id=" + id +
				", note='" + note + '\'' +
				", values=" + values +
				", valuesWithProperties=" + valuesWithProperties +
				'}';
	}
}
