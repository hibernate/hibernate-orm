/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;

/**
 * @author Chris Cranford
 */
@MappedSuperclass
public class SubjectAdvertisement {
	@OneToOne
	private Advertisement advertisement;

	public Advertisement getAdvertisement() {
		return advertisement;
	}

	public void setAdvertisement(Advertisement advertisement) {
		this.advertisement = advertisement;
	}
}
