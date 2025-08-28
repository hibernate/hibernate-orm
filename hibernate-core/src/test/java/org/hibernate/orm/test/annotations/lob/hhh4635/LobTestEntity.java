/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob.hhh4635;

import java.sql.Blob;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table( name = "lob_test" )
public class LobTestEntity {

	@Id
	private Long id;

	@Lob
	private Blob lobValue;

	@Column( name = "qwerty", length = 4000 )
	private String qwerty;

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setLobValue(Blob lobValue) {
		this.lobValue = lobValue;
	}

	public Blob getLobValue() {
		return lobValue;
	}

	public void setQwerty(String qwerty) {
		this.qwerty = qwerty;
	}

	public String getQwerty() {
		return qwerty;
	}

}
