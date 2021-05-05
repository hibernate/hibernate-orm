@FetchProfile(name = "mappedBy-package-profile-1", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Address.class, association = "customer", mode = FetchMode.JOIN)
})
@FetchProfile(name = "mappedBy-package-profile-2", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Customer6.class, association = "address", mode = FetchMode.JOIN)
})
package org.hibernate.orm.test.annotations.fetchprofile.mappedby;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.orm.test.annotations.fetchprofile.Customer6;
