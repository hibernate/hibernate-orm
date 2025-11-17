/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.tablecomment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

@Entity
@Table(
		name = JpaTableCommentTest.TABLE_NAME,
		comment = JpaTableCommentTest.TABLE_COMMENT
)
@SecondaryTable(
		name = JpaTableCommentTest.SECONDARY_TABLE_NAME,
		comment = JpaTableCommentTest.SECONDARY_TABLE_COMMENT
)
public class TestEntity {
	@Id
	private Long id;

	@Column(name = "NAME_COLUMN")
	private String name;

	@Column(name = "SECOND_NAME", table = JpaTableCommentTest.SECONDARY_TABLE_NAME)
	private String secondName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinTable(
			name = JpaTableCommentTest.JOIN_TABLE_NAME,
			comment = JpaTableCommentTest.JOIN_TABLE_COMMENT
	)
	private TestEntity testEntity;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSecondName() {
		return secondName;
	}

	public void setSecondName(String secondName) {
		this.secondName = secondName;
	}

	public TestEntity getTestEntity() {
		return testEntity;
	}

	public void setTestEntity(TestEntity testEntity) {
		this.testEntity = testEntity;
	}
}
