/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import java.util.ArrayList;

/**
 * Implementation of our specialized collection contract
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public class DefaultableListImpl extends ArrayList implements DefaultableList {
	private String defaultValue;

	public DefaultableListImpl() {
	}

	public DefaultableListImpl(int anticipatedSize) {
		super( anticipatedSize + ( int ) Math.ceil( anticipatedSize * .75f ) );
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
