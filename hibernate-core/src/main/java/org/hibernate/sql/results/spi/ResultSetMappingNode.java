/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.query.NavigablePath;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Marker for all object types that can be part of a ResultSetMapping.
 *
 * Both {@link DomainResult} and {@link Fetch} are ResultSetMappingNode sub-types.
 *
 * @author Steve Ebersole
 */
public interface ResultSetMappingNode {
	// todo (6.0) : result variable (selection alias)?  - even fetches can have alias

	JavaTypeDescriptor getJavaTypeDescriptor();

	/**
	 * The NavigablePath for this node (if one!).  Certain nodes will not
	 * have a NavigablePath, namely those not associated with a Navigable
	 */
	default NavigablePath getNavigablePath() {
		// by default these nodes would not have a path.  those that do explicitly
		// override this already to return it
		return null;
	}
}
