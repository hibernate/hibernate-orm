/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.columnoptions;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "TEST_ENTITY")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = ColumnOptionsTest.DISCRIMINATOR_COLUMN_NAME, options = ColumnOptionsTest.DISCRIMINATOR_COLUMN_OPTIONS)
public class TestEntity {
	@Id
	private int id;

	@Column(name = ColumnOptionsTest.COLUMN_NAME, options = ColumnOptionsTest.COLUMN_OPTIONS)
	private String name;

	@ManyToOne
	@JoinColumns(
			value = @JoinColumn(name = ColumnOptionsTest.JOIN_COLUMN_NAME, options = ColumnOptionsTest.JOIN_COLUMN_OPTIONS)

	)
	private TestEntity testEntity;

	@MapKeyTemporal(TemporalType.DATE)
	@ElementCollection
	@MapKeyColumn(name = ColumnOptionsTest.ELEMENT_COLLECTION_MAP_KEY_COLUMN_NAME, options = ColumnOptionsTest.ELEMENT_COLLECTION_MAP_KEY_COLUMN_OPTIONS)
	private Map<Date, Integer> colorPerDate = new HashMap<>();

	@OneToMany
	@MapKeyColumn(name = ColumnOptionsTest.ONE_TO_MANY_MAP_KEY_COLUMN_NAME, options = ColumnOptionsTest.ONE_TO_MANY_MAP_KEY_COLUMN_OPTIONS)
	private Map<Date, TestEntity> testEntityMap = new HashMap<>();

	@ManyToMany
	@MapKeyColumn(name = ColumnOptionsTest.MANY_TO_MANY_MAP_KEY_COLUMN_NAME, options = ColumnOptionsTest.MANY_TO_MANY_MAP_KEY_COLUMN_OPTIONS)
	private Map<Date, TestEntity> testEntityMap2 = new HashMap<>();

	@ElementCollection
	@CollectionTable(
			name = "ELEMENT_COLLECTION_TABLE")
	@MapKeyJoinColumn(name = ColumnOptionsTest.ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_NAME, insertable = false, options = ColumnOptionsTest.ELEMENT_COLLECTION_MAP_KEY_JOIN_COLUMN_OPTIONS)
	private Map<AnotherTestEntity, String> stringMap = new HashMap<>();

	@OneToMany
	@JoinTable(name = "TEST_ENTITY_3")
	@MapKeyJoinColumn(name = ColumnOptionsTest.ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME, options = ColumnOptionsTest.ONE_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS)
	private Map<TestEntity, TestEntity> testEntityMap3 = new HashMap<>();

	@ManyToMany
	@JoinTable(name = "TEST_ENTITY_4")
	@MapKeyJoinColumn(name = ColumnOptionsTest.MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_NAME, options = ColumnOptionsTest.MANY_TO_MANY_MAP_KEY_JOIN_COLUMN_OPTIONS)
	private Map<TestEntity, TestEntity> testEntityMap4 = new HashMap<>();

	@ElementCollection
	@OrderColumn(name = ColumnOptionsTest.ORDER_COLUMN_NAME, options = ColumnOptionsTest.ORDER_COLUMN_OPTIONS)
	int[] theNumbers;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TestEntity getTestEntity() {
		return testEntity;
	}

	public void setTestEntity(TestEntity testEntity) {
		this.testEntity = testEntity;
	}

	public Map<Date, Integer> getColorPerDate() {
		return colorPerDate;
	}

	public void setColorPerDate(Map<Date, Integer> colorPerDate) {
		this.colorPerDate = colorPerDate;
	}

	public Map<Date, TestEntity> getTestEntityMap() {
		return testEntityMap;
	}

	public void setTestEntityMap(Map<Date, TestEntity> testEntityMap) {
		this.testEntityMap = testEntityMap;
	}

	public Map<Date, TestEntity> getTestEntityMap2() {
		return testEntityMap2;
	}

	public void setTestEntityMap2(Map<Date, TestEntity> testEntityMap2) {
		this.testEntityMap2 = testEntityMap2;
	}

	public Map<AnotherTestEntity, String> getStringMap() {
		return stringMap;
	}

	public void setStringMap(Map<AnotherTestEntity, String> stringMap) {
		this.stringMap = stringMap;
	}

	public Map<TestEntity, TestEntity> getTestEntityMap3() {
		return testEntityMap3;
	}

	public void setTestEntityMap3(Map<TestEntity, TestEntity> testEntityMap3) {
		this.testEntityMap3 = testEntityMap3;
	}

	public Map<TestEntity, TestEntity> getTestEntityMap4() {
		return testEntityMap4;
	}

	public void setTestEntityMap4(Map<TestEntity, TestEntity> testEntityMap4) {
		this.testEntityMap4 = testEntityMap4;
	}

	public int[] getTheNumbers() {
		return theNumbers;
	}

	public void setTheNumbers(int[] theNumbers) {
		this.theNumbers = theNumbers;
	}

//	public AnotherTestEntity getAnotherTestEntity() {
//		return anotherTestEntity;
//	}
//
//	public void setAnotherTestEntity(AnotherTestEntity anotherTestEntity) {
//		this.anotherTestEntity = anotherTestEntity;
//	}
}
