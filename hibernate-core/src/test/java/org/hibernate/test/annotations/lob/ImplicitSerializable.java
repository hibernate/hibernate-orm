/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;

import java.io.Serializable;

import org.hibernate.test.annotations.lob.EntitySerialize.CommonSerializable;

/**
 * @author Janario Oliveira
 */
public class ImplicitSerializable implements Serializable, CommonSerializable {
	String defaultValue;
	String value;

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
