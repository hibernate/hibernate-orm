/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "DomesticPublishHouse")
@Table(name = "DomesticPublishHouse")
public class DomesticPublishingHouse extends PublishingHouse {
	private String taxIdentifier;
}
