/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
