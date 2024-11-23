/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

@FetchProfile(name = "package-profile-1", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Customer.class, association = "orders")
})
@FetchProfile(name = "package-profile-2", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Customer.class, association = "tickets")
})
package org.hibernate.orm.test.annotations.fetchprofile;

import org.hibernate.annotations.FetchProfile;
