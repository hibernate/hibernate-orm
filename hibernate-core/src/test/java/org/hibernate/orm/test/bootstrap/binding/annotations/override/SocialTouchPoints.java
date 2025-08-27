/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToMany;

@Embeddable
public class SocialTouchPoints {

	// owning side of many to many
	@ManyToMany(cascade= CascadeType.ALL)
	List<SocialSite> website;

	public List<SocialSite> getWebsite() {
		return website;
	}

	public void setWebsite(List<SocialSite> website) {
		this.website = website;
	}
}
