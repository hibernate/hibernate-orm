/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "USER_CONFS")
@IdClass(UserConfId.class)
public class UserConfEntity implements Serializable{

	private static final long serialVersionUID = 9153314908821604322L;

	@Id
	@ManyToOne
	@JoinColumn(name="user_id", nullable = false)
	private UserEntity user;

	@Id
	@ManyToOne
	@JoinColumn(name="cnf_key", referencedColumnName="confKey")
	@JoinColumn(name="cnf_value", referencedColumnName="confValue")
	private ConfEntity conf;

	public ConfEntity getConf() {
		return conf;
	}

	public void setConf(ConfEntity conf) {
		this.conf = conf;
	}


	public UserEntity getUser() {
		return user;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}
}
