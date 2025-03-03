/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.checkconstraint.table;

import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyDiscriminatorValues;
import org.hibernate.annotations.AnyKeyJavaClass;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SecondaryTable;

@Entity(name = "TEST_ENTITY_2")
@SecondaryTable(
		name = TableCheckConstraintTest.SECONDARY_TABLE_NAME,
		check = {
				@CheckConstraint(
						name = "TABLE_CONSTRAINT",
						constraint = TableCheckConstraintTest.SECONDARY_TABLE_CONSTRAINTS
				)
		}
)
public class EntityWithSecondaryTables {
	@Id
	private Long id;

	@Column(name = "NAME_COLUMN")
	private String name;

	@Column(name = "SECOND_NAME", table = TableCheckConstraintTest.SECONDARY_TABLE_NAME)
	private String secondName;

	@OneToOne
	@JoinTable(
			name = TableCheckConstraintTest.ONE_TO_ONE_JOIN_TABLE_NAME,
			check = {
					@CheckConstraint(
							name = "ONE_TO_ONE_JOIN_TABLE_CONSTRAINT",
							constraint = TableCheckConstraintTest.ONE_TO_ONE_JOIN_TABLE_CONSTRAINTS
					)
			}
	)
	private AnotherTestEntity entity;

	@ManyToOne
	@JoinTable(
			name = TableCheckConstraintTest.MANY_TO_ONE_JOIN_TABLE_NAME,
			check = {
					@CheckConstraint(
							name = "MANY_TO_ONE_JOIN_TABLE_CONSTRAINT",
							constraint = TableCheckConstraintTest.MANY_TO_ONE_JOIN_TABLE_CONSTRAINTS
					)
			}
	)
	private AnotherTestEntity testEntity;

	@OneToMany
	@JoinTable(
			name = TableCheckConstraintTest.ONE_TO_MANY_JOIN_TABLE_NAME,
			check = {
					@CheckConstraint(
							name = "ONE_TO_MANY_JOIN_TABLE_CONSTRAINT",
							constraint = TableCheckConstraintTest.ONE_TO_MANY_JOIN_TABLE_CONSTRAINTS
					)
			}
	)
	private List<AnotherTestEntity> testEntities;

	@ManyToMany
	@JoinTable(
			name = TableCheckConstraintTest.MANY_TO_MANY_JOIN_TABLE_NAME,
			check = {
					@CheckConstraint(
							name = "MANY_TO_MANY_JOIN_TABLE_CONSTRAINT",
							constraint = TableCheckConstraintTest.MANY_TO_MANY_JOIN_TABLE_CONSTRAINTS
					)
			}
	)
	private List<AnotherTestEntity> testEntities2;

	@Any
	@AnyDiscriminator(DiscriminatorType.STRING)
	@AnyDiscriminatorValues({
			@AnyDiscriminatorValue(discriminator = "S", entity = AnotherTestEntity.class),
	})
	@AnyKeyJavaClass(Long.class)
	@Column(name = "another_type")
	@JoinColumn(name = "another_id")
	@JoinTable(
			name = TableCheckConstraintTest.ANY_JOIN_TABLE_NAME,
			check = {
					@CheckConstraint(
							name = "ANY_JOIN_TABLE_CONSTRAINT",
							constraint = TableCheckConstraintTest.ANY_JOIN_TABLE_CONSTRAINTS
					)
			}
	)
	private Another another;

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

	public AnotherTestEntity getEntity() {
		return entity;
	}

	public void setEntity(AnotherTestEntity entity) {
		this.entity = entity;
	}

	public AnotherTestEntity getTestEntity() {
		return testEntity;
	}

	public void setTestEntity(AnotherTestEntity testEntity) {
		this.testEntity = testEntity;
	}

	public List<AnotherTestEntity> getTestEntities() {
		return testEntities;
	}

	public void setTestEntities(List<AnotherTestEntity> testEntities) {
		this.testEntities = testEntities;
	}

	public List<AnotherTestEntity> getTestEntities2() {
		return testEntities2;
	}

	public void setTestEntities2(List<AnotherTestEntity> testEntities2) {
		this.testEntities2 = testEntities2;
	}

	public Another getAnother() {
		return another;
	}

	public void setAnother(Another another) {
		this.another = another;
	}
}
