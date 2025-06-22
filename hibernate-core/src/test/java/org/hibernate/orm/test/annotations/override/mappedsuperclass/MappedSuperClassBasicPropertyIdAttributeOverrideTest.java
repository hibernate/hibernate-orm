/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.override.mappedsuperclass;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.junit.jupiter.api.Test;

/**
 * @author Stanislav Gubanov
 */
@JiraKey(value = "HHH-11771")
@DomainModel(
		annotatedClasses = {
				MappedSuperClassBasicPropertyIdAttributeOverrideTest.BaseMappedSuperClass.class,
				MappedSuperClassBasicPropertyIdAttributeOverrideTest.ExtendBase.class
		}
)
public class MappedSuperClassBasicPropertyIdAttributeOverrideTest {

	@Test
	public void test() {
	}

	@MappedSuperclass
	@Access(AccessType.FIELD)
	public static class BaseMappedSuperClass {

		@Id
		Long uid;

		public Long getUid() {
			return uid;
		}

		public void setUid(Long uid) {
			this.uid = uid;
		}
	}

	@Entity
	public static class ExtendBase extends BaseMappedSuperClass {

		@Access(AccessType.PROPERTY)
		@Override
		@AttributeOverride(name = "uid", column = @Column(name = "id_extend_table", insertable = false, updatable = false))
		public Long getUid() {
			return super.getUid();
		}
	}

}
