/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Publisher")
@Table(name = "Publisher")
public class Publisher {
	@Id
	private Integer id;
	private String registrationId;

	@ManyToOne
	@JoinColumn(name = "person_data_fk")
	private Person personDetails;

	@ManyToOne
	@JoinColumn(name = "pub_house_fk")
	private PublishingHouse publishingHouse;

}
