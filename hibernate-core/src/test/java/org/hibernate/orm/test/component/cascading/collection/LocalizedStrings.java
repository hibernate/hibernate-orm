/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.collection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class LocalizedStrings {
	private final Map<Locale,String> strings = new HashMap<>();

	public void addString(Locale locale, String value) {
		strings.put( locale, value );
	}

	public String getString(Locale locale) {
		return strings.get( locale );
	}

	public Map<Locale,String> makeStringsCopy() {
		return java.util.Collections.unmodifiableMap( strings );
	}
}
