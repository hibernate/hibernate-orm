/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone.hhh4851;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A group of {@link LogicalTerminal logical terminals}. Used to group them for Configuration purpose. That's why a
 * LogicalTerminal can only have one TerminalGroup.
 */
@Entity
@Table
public class DeviceGroupConfig extends BaseEntity {

	private String name = null;

	public DeviceGroupConfig() {

	}

	/**
	 * Not unique, because we could use the same name in two different organizations.
	 *
	 * @return
	 */
	@Column(nullable = false)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
