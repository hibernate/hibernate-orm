/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import java.io.Serializable;
import java.util.Properties;

/**
 * Placeholder for typedef information
 */
public class TypeDef implements Serializable {

	private String typeClass;
	private Properties parameters;

	public TypeDef(String typeClass, Properties parameters) {
		this.typeClass = typeClass;
		this.parameters = parameters;
	}

	public Properties getParameters() {
		return parameters;
	}
	public String getTypeClass() {
		return typeClass;
	}

}
