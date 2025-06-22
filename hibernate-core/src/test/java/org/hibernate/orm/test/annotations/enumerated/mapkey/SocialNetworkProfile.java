/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.enumerated.mapkey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * @author Dmitry Spikhalskiy
 * @author Steve Ebersole
 */
@Entity
@Table(name = "social_network_profile", uniqueConstraints = {@UniqueConstraint(columnNames = {"social_network", "network_id"})})
public class SocialNetworkProfile {
	@jakarta.persistence.Id
	@jakarta.persistence.Column(name = "id", unique = true)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(value = EnumType.STRING) //if change type to ordinal - test will not failure
	@Column(name = "social_network", nullable = false)
	private SocialNetwork socialNetworkType;

	@Column(name = "network_id", nullable = false)
	private String networkId;

	protected SocialNetworkProfile() {
	}

	protected SocialNetworkProfile(User user, SocialNetwork socialNetworkType, String networkId) {
		this.id = "snp_" + networkId + "_" + user.getId();
		this.user = user;
		this.socialNetworkType = socialNetworkType;
		this.networkId = networkId;
	}
}
