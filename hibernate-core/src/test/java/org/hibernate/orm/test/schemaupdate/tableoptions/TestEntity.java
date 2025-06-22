/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.tableoptions;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(
		name = TableOptionsTest.TABLE_NAME,
		options = TableOptionsTest.TABLE_OPTIONS
)
@SecondaryTable(
		name = TableOptionsTest.SECONDARY_TABLE_NAME,
		options = TableOptionsTest.SECONDARY_TABLE_OPTIONS
)
public class TestEntity {
	@Id
	@TableGenerator(
			name = "id-table-generator",
			table = TableOptionsTest.TABLE_GENERATOR_NAME,
			options = TableOptionsTest.TABLE_GENERATOR_OPTIONS
	)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "id-table-generator")
	private Long id;

	@Column(name = "NAME_COLUMN")
	private String name;

	@Column(name = "SECOND_NAME", table = TableOptionsTest.SECONDARY_TABLE_NAME)
	private String secondName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinTable(
			name = TableOptionsTest.JOIN_TABLE_NAME,
			options = TableOptionsTest.JOIN_TABLE_OPTIONS
	)
	private TestEntity testEntity;

	@ElementCollection
	@CollectionTable(
			name = TableOptionsTest.COLLECTION_TABLE_NAME,
			options = TableOptionsTest.COLLECTION_TABLE_OPTIONS
	)
	private List<String> stringFields;

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

	public List<String> getStringFields() {
		return stringFields;
	}

	public void setStringFields(List<String> stringFields) {
		this.stringFields = stringFields;
	}
}
