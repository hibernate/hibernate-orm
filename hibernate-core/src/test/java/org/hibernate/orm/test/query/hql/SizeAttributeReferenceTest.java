/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Set;

/**
 * Historically HQL allowed syntax like {@code `... where someCollectionPath.size > 1`} where
 * size refers to the collection size.  However that disallows references to properties named
 * size.
 *
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-10024")
@DomainModel(
		annotatedClasses = {
				SizeAttributeReferenceTest.EntityWithAttributeNamedSize.class
		}
)
@SessionFactory
public class SizeAttributeReferenceTest {

	@Test
	public void controlGroup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityWithAttributeNamedSize e join e.children c where size(c) > 1",
							EntityWithAttributeNamedSize.class ).list();
				}
		);
	}

	@Test
	public void testSizeAttributeReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityWithAttributeNamedSize e join e.children c where c.size = 'abc'",
							EntityWithAttributeNamedSize.class ).list();
				}
		);
	}

	@Entity(name = "EntityWithAttributeNamedSize")
	@Table(name = "EntityWithAttributeNamedSize")
	public static class EntityWithAttributeNamedSize {
		@Id
		public Integer id;
		@ManyToOne
		public EntityWithAttributeNamedSize parent;
		@OneToMany(mappedBy = "parent")
		public Set<EntityWithAttributeNamedSize> children;
		@Column(name = "`size`")
		private String size;
	}

}
