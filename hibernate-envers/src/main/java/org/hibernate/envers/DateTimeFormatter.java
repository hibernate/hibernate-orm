/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import java.text.DateFormat;
import java.util.Locale;

/**
 * Utility class that provides access to a {@link DateFormat} instance.
 *
 * @author Chris Cranford
 */
public class DateTimeFormatter {

	public static DateFormat INSTANCE = DateFormat.getDateTimeInstance(
			DateFormat.DEFAULT,
			DateFormat.DEFAULT,
			Locale.ENGLISH );

	private DateTimeFormatter() {
	}

}
