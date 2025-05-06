/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Thing {
	private Double weight;
	private Long version;

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	@Version
	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}
}
