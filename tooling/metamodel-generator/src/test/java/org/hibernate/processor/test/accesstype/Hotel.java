/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;

/**
 * @author gsmet
 */
@Embeddable
public class Hotel {

	@OneToOne
	private User webDomainExpert;

	public User getWebDomainExpert() {
		return webDomainExpert;
	}

	public void setWebDomainExpert(User webDomainExpert) {
		this.webDomainExpert = webDomainExpert;
	}
}
