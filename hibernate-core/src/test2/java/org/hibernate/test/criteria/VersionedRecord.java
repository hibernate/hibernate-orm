/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import javax.persistence.MappedSuperclass;

/**
* @author Strong Liu <stliu@hibernate.org>
*/
@MappedSuperclass
abstract class VersionedRecord implements java.io.Serializable {
	Long recordVersion;
	Boolean isDeleted;
}
