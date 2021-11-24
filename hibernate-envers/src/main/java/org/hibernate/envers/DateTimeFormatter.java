/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
