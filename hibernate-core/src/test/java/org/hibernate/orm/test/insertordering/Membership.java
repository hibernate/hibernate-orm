/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.Date;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "INS_ORD_MEM")
public class Membership {
	@Id
	@GeneratedValue
	private Long id;
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "USR_ID")
	private User user;
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "GRP_ID")
	private Group group;
	@Column(name = "JN_DT")
	private Date activationDate;

	/**
	 * For persistence
	 */
	Membership() {
	}

	public Membership(User user, Group group) {
		this( user, group, new Date() );
	}

	public Membership(User user, Group group, Date activationDate) {
		this.user = user;
		this.group = group;
		this.activationDate = activationDate;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Group getGroup() {
		return group;
	}

	public Date getActivationDate() {
		return activationDate;
	}
}
