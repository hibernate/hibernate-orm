/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
