/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.enumerated.mapkey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * @author Dmitry Spikhalskiy
 * @author Steve Ebersole
 */
@Entity
@Table(name = "social_network_profile", uniqueConstraints = {@UniqueConstraint(columnNames = {"social_network", "network_id"})})
public class SocialNetworkProfile {
	@javax.persistence.Id
	@javax.persistence.GeneratedValue(generator = "system-uuid")
	@org.hibernate.annotations.GenericGenerator(name = "system-uuid", strategy = "uuid2")
	@javax.persistence.Column(name = "id", unique = true)
	private java.lang.String id;

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
		this.user = user;
		this.socialNetworkType = socialNetworkType;
		this.networkId = networkId;
	}
}
