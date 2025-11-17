/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Yoann Rodière
 * @author Marco Belladelli
 */
@BytecodeEnhanced
@EnhancementOptions( inlineDirtyChecking = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16459" )
public class DirtyTrackingInheritanceTest {
	@Test
	public void test() {
		final ChildItem entity = new ChildItem();
		EnhancerTestUtils.checkDirtyTracking( entity );
		entity.setBasicValue( "basic_value" );
		entity.setAssociation( new Other() );
		EnhancerTestUtils.checkDirtyTracking( entity, "basicValue", "association" );
	}

	@Entity( name = "Other" )
	public static class Other {
		@Id
		@GeneratedValue
		private Long id;
	}

	@MappedSuperclass
	public static abstract class Item {
		private String basicValue;
		@ManyToOne
		private Other association;

		public String getBasicValue() {
			return basicValue;
		}

		public void setBasicValue(String basicValue) {
			this.basicValue = basicValue;
		}

		public Other getAssociation() {
			return association;
		}

		public void setAssociation(Other association) {
			this.association = association;
		}
	}

	@Entity( name = "ChildItem" )
	public static class ChildItem extends Item {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
