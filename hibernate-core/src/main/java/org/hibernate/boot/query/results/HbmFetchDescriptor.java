/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results;

/**
 * Describes a fetch defined in {@code hbm.xml} - either via {@code <resultset/>}
 * or implicitly within {@code <sql-query/>}
 */
public interface HbmFetchDescriptor extends FetchDescriptor {
	/**
	 * The fetch path.  May be nested (i.e. a composite name)
	 */
	String getFetchablePath();
}
