/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.testing;

import java.util.Locale;

public class HQLTemplate {

	final private String hqlTemplate;

	public HQLTemplate(String template) {
		this.hqlTemplate = template;
	}

	public static HQLTemplate from(String s) {
		return new HQLTemplate( s );
	}

	public String mkHQLString(Object... params) {
		return String.format( Locale.ROOT, hqlTemplate, params );
	}
}
