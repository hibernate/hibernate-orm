/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.inet;

import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Event")
@Table(name = "event")
public class Event {

	@Id
	private Long id;

	@Column(name = "ip")
	@JdbcTypeCode(SqlTypes.INET)
	@JavaType(InetJavaType.class)
	private Inet ip;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Inet getIp() {
		return ip;
	}

	public void setIp(String address) {
		this.ip = new Inet( address );
	}
}
