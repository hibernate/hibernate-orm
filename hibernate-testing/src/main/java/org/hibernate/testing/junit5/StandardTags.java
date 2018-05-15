/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.junit5;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class StandardTags {
	public static final String FAILURE_EXPECTED = "failure-expected";
	public static final String PERF = "perf";
	public static final String QUERY = "query";
	public static final String SQM = "sqm";
	public static final String UNIT = "unit";
	public static final String ENVERS = "envers";

	private StandardTags() {
	}
}
