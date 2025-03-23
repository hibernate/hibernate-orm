/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "USER_SETTING")
public class UserSetting {
	@Id
	@GeneratedValue
	@Column(name = "USER_SETTING_ID")
	public long id;

	@OneToOne
	@JoinColumn(name = "USER_ID", foreignKey = @ForeignKey(name = "FK_TO_USER"))
	private User user;
}
