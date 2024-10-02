/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.manytomany.generic;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;

@Jpa(
		annotatedClasses = {
				ManyToManyNonGenericTest.NodeTree.class,
				ManyToManyNonGenericTest.Node.class
		}
)
public class ManyToManyNonGenericTest {

	@Test
	void testSelfReferencingGeneric(final EntityManagerFactoryScope scope) {
		final UUID treeId = scope.fromTransaction(em -> {
			final NodeTree tree = new NodeTree();
			final Node root = new Node();
			root.tree = tree;
			final Node branch = new Node();
			branch.tree = tree;
			tree.nodes.add(root);
			tree.nodes.add(branch);
			root.children.add(branch);
			em.persist(tree);
			return tree.id;
		});

		final NodeTree nodeTree = scope.fromEntityManager(em -> em.find(NodeTree.class, treeId));

		assertThat(nodeTree, is(notNullValue()));
		assertThat(nodeTree.id, is(treeId));
		assertThat(nodeTree.nodes, iterableWithSize(2));
		assertThat(nodeTree.nodes, containsInAnyOrder(List.of(
				hasProperty("children", iterableWithSize(1)),
				hasProperty("children", emptyIterable())
		)));
	}

	@Entity(name = "tree")
	public static class NodeTree {
		@Id
		@GeneratedValue(strategy = GenerationType.UUID)
		public UUID id;

		@OneToMany(mappedBy = "tree", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		public Set<Node> nodes = new HashSet<>();
	}

	@Entity(name = "node")
	public static class Node {

		@Id
		@GeneratedValue(strategy = GenerationType.UUID)
		public UUID id;

		@ManyToOne(optional = false)
		@JoinColumn(name = "TREE_ID")
		public NodeTree tree;

		@ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.DETACH})
		@JoinTable(name = "NODE_CHILDREN",
				joinColumns = {@JoinColumn(name = "TREE_ID", referencedColumnName = "TREE_ID"), @JoinColumn(name = "NODE_ID", referencedColumnName = "ID")},
				inverseJoinColumns = {@JoinColumn(name = "CHILD_ID", referencedColumnName = "ID")}
		)
		private final Set<Node> children = new HashSet<>();

		public Set<Node> getChildren() {
			return children;
		}

		@Override
		public String toString() {
			return String.format("node [%s] parent of %s", id, children.stream().map(n -> n.id).collect(Collectors.toList()));
		}
	}
}
