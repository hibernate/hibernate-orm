/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Yanming Zhou
 */
@BaseUnitTest
public class CommentsTest {

	private static final String TABLE_NAME = "TestEntity";
	private static final String SEC_TABLE_NAME = "TestEntity2";
	private static final String TABLE_COMMENT = "I am a table";
	private static final String SEC_TABLE_COMMENT = "I am a table too";

	@Test
	@JiraKey(value = "HHH-4369")
	public void testComments() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();
		Metadata metadata = new MetadataSources(ssr).addAnnotatedClass(TestEntity.class).buildMetadata();
		org.hibernate.mapping.Table table = StreamSupport.stream(metadata.getDatabase().getNamespaces().spliterator(), false)
				.flatMap(namespace -> namespace.getTables().stream()).filter(t -> t.getName().equals(TABLE_NAME))
				.findFirst().orElse(null);
		assertThat(table.getComment(), is(TABLE_COMMENT));
		assertThat(table.getColumns().size(), is(6));
		for (org.hibernate.mapping.Column col : table.getColumns()) {
			assertThat(col.getComment(), is("I am " + col.getName()));
		}
		table = StreamSupport.stream(metadata.getDatabase().getNamespaces().spliterator(), false)
				.flatMap(namespace -> namespace.getTables().stream()).filter(t -> t.getName().equals(SEC_TABLE_NAME))
				.findFirst().orElse(null);
		assertThat(table.getComment(), is(SEC_TABLE_COMMENT));
		assertThat(table.getColumns().size(), is(2));
		long count = table.getColumns().stream().filter(col -> "This is a date".equalsIgnoreCase(col.getComment())).count();
		assertThat(count, is(1L));
	}

	@Entity(name = "Person")
	@Table(name = TABLE_NAME)
	@SecondaryTable(name = SEC_TABLE_NAME)
	@Comment(TABLE_COMMENT)
	@Comment(value = SEC_TABLE_COMMENT, on = SEC_TABLE_NAME)
	public static class TestEntity {

		@Id
		@GeneratedValue
		@Comment("I am id")
		private Long id;

		@Comment(on = "firstName", value = "I am firstName")
		@Comment(on = "lastName", value = "I am lastName")
		private Name name;

		private Money money;

		@Column(table = SEC_TABLE_NAME)
		@Comment("This is a date")
		private LocalDate localDate;

		@ManyToOne
		@JoinColumn(name = "other")
		@Comment("I am other")
		private TestEntity other;

	}

	@Embeddable
	public static class Name {
		private String firstName;
		private String lastName;
	}

	@Embeddable
	public static class Money {
		@Comment("I am amount")
		private BigDecimal amount;
		@Comment("I am currency")
		private Currency currency;
	}

}
