/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql;

import java.util.List;

import org.hibernate.LockMode;

/**
 * @author Steve Ebersole
 */
public interface FetchResultRegistration extends ResultRegistration {

	// todo (6.0) - expose the fetch navigable somehow?
	//		NavigableRole perhaps?  NavigableRole would need to move
	//		to API, but that is probably worthwhile anyway

	LockMode getLockMode();
	List<AttributeResultRegistration> getAttributeResultRegistrations();
}
