/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.xml;

import java.io.Serializable;

/**
 * Describes the origin of an xml document
 *
 * @author Steve Ebersole
 */
public interface Origin extends Serializable {
	/**
	 * Retrieve the type of origin.  This is not a discrete set, but might be something like
	 * {@code file} for file protocol URLs, or {@code resource} for classpath resource lookups.
	 *
	 * @return The origin type.
	 */
	public String getType();

	/**
	 * The name of the document origin.  Interpretation is relative to the type, but might be the
	 * resource name or file URL.
	 *
	 * @return The name.
	 */
	public String getName();
}
