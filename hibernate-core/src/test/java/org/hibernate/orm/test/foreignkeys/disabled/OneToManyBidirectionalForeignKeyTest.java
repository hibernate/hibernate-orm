/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.foreignkeys.disabled;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.StreamSupport;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

/**
 * {@inheritDoc}
 *
 * @author Yanming Zhou
 */
@JiraKey(value = "HHH-14229")
public class OneToManyBidirectionalForeignKeyTest {

	private static final String TABLE_NAME_PLAIN = "plain";
	private static final String TABLE_NAME_WITH_ON_DELETE = "cascade_delete";

	@Test
	public void testForeignKeyShouldNotBeCreated() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( PlainTreeEntity.class ).addAnnotatedClass( TreeEntityWithOnDelete.class )
					.buildMetadata();
			assertTrue( findTable( metadata, TABLE_NAME_PLAIN ).getForeignKeyCollection().isEmpty() );
			assertFalse( findTable( metadata, TABLE_NAME_WITH_ON_DELETE ).getForeignKeyCollection().isEmpty() );
		}
	}

	private static Table findTable(Metadata metadata, String tableName) {
		return StreamSupport.stream(metadata.getDatabase().getNamespaces().spliterator(), false)
				.flatMap(namespace -> namespace.getTables().stream()).filter(t -> t.getName().equals(tableName))
				.findFirst().orElse(null);
	}

	@Entity
	@jakarta.persistence.Table(name = TABLE_NAME_PLAIN)
	public static class PlainTreeEntity {

		@Id
		private Long id;

		@ManyToOne
		@JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
		private PlainTreeEntity parent;

		@OneToMany(mappedBy = "parent")
		// workaround
		// @org.hibernate.annotations.ForeignKey(name = "none")
		private Collection<PlainTreeEntity> children = new ArrayList<>(0);
	}

	@Entity
	@jakarta.persistence.Table(name = TABLE_NAME_WITH_ON_DELETE)
	public static class TreeEntityWithOnDelete {

		@Id
		private Long id;

		@ManyToOne
		@JoinColumn(foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
		private TreeEntityWithOnDelete parent;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE)
		@OnDelete(action = OnDeleteAction.CASCADE)
		private Collection<TreeEntityWithOnDelete> children = new ArrayList<>(0);

	}

}
