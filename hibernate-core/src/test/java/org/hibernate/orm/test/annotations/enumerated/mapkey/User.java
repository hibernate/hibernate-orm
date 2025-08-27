/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.enumerated.mapkey;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Dmitry Spikhalskiy
 * @author Steve Ebersole
 */
@Entity
@Table( name = "USER_TABLE" )
public class User {
	@jakarta.persistence.Id
	@jakarta.persistence.Column(name = "id", unique = true)
	private String id;

	@MapKeyEnumerated( EnumType.STRING )
	@MapKeyColumn(name = "social_network")
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Map<SocialNetwork, SocialNetworkProfile> socialNetworkProfiles = new EnumMap<SocialNetwork, SocialNetworkProfile>(SocialNetwork.class);

	protected User() {
	}

	public User(String id, SocialNetwork sn, String socialNetworkId) {
		this.id = id;
		SocialNetworkProfile profile = new SocialNetworkProfile(this, sn, socialNetworkId);
		socialNetworkProfiles.put(sn, profile);
	}

	public SocialNetworkProfile getSocialNetworkProfile(SocialNetwork socialNetwork) {
		return socialNetworkProfiles.get(socialNetwork);
	}

	public String getId() {
		return id;
	}
}
