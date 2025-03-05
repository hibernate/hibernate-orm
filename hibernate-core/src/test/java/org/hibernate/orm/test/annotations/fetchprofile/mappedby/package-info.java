/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
@FetchProfile(name = "mappedBy-package-profile-1", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Address.class, association = "customer")
})
@FetchProfile(name = "mappedBy-package-profile-2", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Customer6.class, association = "address")
})
package org.hibernate.orm.test.annotations.fetchprofile.mappedby;

import org.hibernate.annotations.FetchProfile;
import org.hibernate.orm.test.annotations.fetchprofile.Customer6;
