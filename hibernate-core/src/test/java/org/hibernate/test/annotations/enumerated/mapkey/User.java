/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.enumerated.mapkey;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Dmitry Spikhalskiy
 * @author Steve Ebersole
 */
@Entity
@Table( name = "USER_TABLE" )
public class User {
	@javax.persistence.Id
	@javax.persistence.GeneratedValue(generator = "system-uuid")
	@org.hibernate.annotations.GenericGenerator(name = "system-uuid", strategy = "uuid2")
	@javax.persistence.Column(name = "id", unique = true)
	private java.lang.String id;

	@MapKeyEnumerated( EnumType.STRING )
	@MapKeyColumn(name = "social_network")
	@OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Map<SocialNetwork, SocialNetworkProfile> socialNetworkProfiles = new EnumMap<SocialNetwork, SocialNetworkProfile>(SocialNetwork.class);

	protected User() {
	}

	public User(SocialNetwork sn, String socialNetworkId) {
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
