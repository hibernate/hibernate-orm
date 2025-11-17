/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.comment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.stream.StreamSupport;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Comment;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Yanming Zhou
 */
@BaseUnitTest
public class CommentTest {

	private static final String TABLE_NAME = "TestEntity";
	private static final String TABLE_COMMENT = "I am table";

	@Test
	@JiraKey(value = "HHH-4369")
	public void testComments() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		Metadata metadata = new MetadataSources(ssr).addAnnotatedClass(TestEntity.class).buildMetadata();
		Table table = StreamSupport.stream(metadata.getDatabase().getNamespaces().spliterator(), false)
				.flatMap(namespace -> namespace.getTables().stream()).filter(t -> t.getName().equals(TABLE_NAME))
				.findFirst().orElse(null);
		assertThat(table.getComment(), is(TABLE_COMMENT));
		for (Column col : table.getColumns()) {
			assertThat(col.getComment(), is("I am " + col.getName()));
		}
	}

	@Entity(name = "Person")
	@jakarta.persistence.Table(name = TABLE_NAME)
	@Comment(TABLE_COMMENT)
	public static class TestEntity {

		@Id
		@GeneratedValue
		@Comment("I am id")
		private Long id;

		@Comment("I am name")
		@jakarta.persistence.Column(length = 50)
		private String name;

		@ManyToOne
		@JoinColumn(name = "other")
		@Comment("I am other")
		private TestEntity other;

	}
}
