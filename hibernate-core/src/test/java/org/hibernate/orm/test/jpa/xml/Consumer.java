/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;

import jakarta.persistence.Id;
import jakarta.persistence.Version;

import java.util.List;

public class Consumer {

	@Id
	private int id;

	private List<ConsumerItem> consumerItems;

	@Version
	private int version;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<ConsumerItem> getConsumerItems() {
		return consumerItems;
	}

	public void setConsumerItems(List<ConsumerItem> consumerItems) {
		this.consumerItems = consumerItems;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}
}
