/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import java.util.List;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

/**
 * @author Luis Barreiro
 */
@JiraKey("HHH-11050")
@BytecodeEnhanced
public class InheritedAttributeAssociationTest {

	@Test
	public void test() {
		// The mapping is wrong but the point is that the enhancement phase does not need to fail. See JIRA for further detail

		// If enhancement of 'items' attribute fails, 'name' won't be enhanced
		Author author = new Author();
		author.name = "Bernardo Soares";
		EnhancerTestUtils.checkDirtyTracking( author, "name" );
	}

	// --- //

	@Entity
	private static class Author {

		@Id
		@GeneratedValue
		Long id;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
		List<ChildItem> items;

		// keep this field after 'items'
		String name;
	}

	@MappedSuperclass
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
	private static abstract class Item {

		@Id
		@GeneratedValue
		Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		Author author;
	}

	@Entity
	@DiscriminatorValue("child")
	private static class ChildItem extends Item {
	}
}
