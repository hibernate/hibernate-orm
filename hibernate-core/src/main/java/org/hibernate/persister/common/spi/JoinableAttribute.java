/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

import java.util.List;

/**
 * Specialization of Attributes that are joinable.
 *
 * @author Steve Ebersole
 */
public interface JoinableAttribute extends Attribute {
	// todo : possibly a JoinMetadata contract encapsulating:
	//		1) "join direction" (ala ForeignKeyDirection
	//		2) JoinColumnMappings (relative to direction)
	//		3) ? JoinableAttributeContainer ?
	//		4) ? JoinableAttribute ?
	List<JoinColumnMapping> getJoinColumnMappings();
}
