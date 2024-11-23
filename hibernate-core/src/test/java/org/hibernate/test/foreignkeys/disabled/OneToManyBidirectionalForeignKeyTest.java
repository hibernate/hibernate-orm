/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.foreignkeys.disabled;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.StreamSupport;

import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Table;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * {@inheritDoc}
 *
 * @author Yanming Zhou
 */
@TestForIssue(jiraKey = "HHH-14229")
public class OneToManyBidirectionalForeignKeyTest {

	private static final String TABLE_NAME_PLAIN = "plain";
	private static final String TABLE_NAME_WITH_ON_DELETE = "cascade_delete";

	@Test
	public void testForeignKeyShouldNotBeCreated() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( PlainTreeEntity.class ).addAnnotatedClass( TreeEntityWithOnDelete.class )
					.buildMetadata();
			assertTrue( findTable( metadata, TABLE_NAME_PLAIN ).getForeignKeys().isEmpty() );
			assertFalse( findTable( metadata, TABLE_NAME_WITH_ON_DELETE ).getForeignKeys().isEmpty() );
		}
	}

	private static Table findTable(Metadata metadata, String tableName) {
		return StreamSupport.stream(metadata.getDatabase().getNamespaces().spliterator(), false)
				.flatMap(namespace -> namespace.getTables().stream()).filter(t -> t.getName().equals(tableName))
				.findFirst().orElse(null);
	}

	@Entity
	@javax.persistence.Table(name = TABLE_NAME_PLAIN)
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
	@javax.persistence.Table(name = TABLE_NAME_WITH_ON_DELETE)
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
