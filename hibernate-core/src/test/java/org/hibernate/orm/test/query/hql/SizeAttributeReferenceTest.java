/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Historically HQL allowed syntax like {@code `... where someCollectionPath.size > 1`} where
 * size refers to the collection size.  However that disallows references to properties named
 * size.
 *
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-10024" )
public class SizeAttributeReferenceTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void controlGroup() {
		Session session = openSession();
		session.getTransaction().begin();
		session.createQuery( "from EntityWithAttributeNamedSize e join e.children c where size(c) > 1", EntityWithAttributeNamedSize.class ).list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testSizeAttributeReference() {
		Session session = openSession();
		session.getTransaction().begin();
		session.createQuery( "from EntityWithAttributeNamedSize e join e.children c where c.size = 'abc'", EntityWithAttributeNamedSize.class ).list();
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithAttributeNamedSize.class };
	}

	@Entity( name = "EntityWithAttributeNamedSize" )
	@Table( name = "EntityWithAttributeNamedSize" )
	public static class EntityWithAttributeNamedSize {
		@Id
		public Integer id;
		@ManyToOne
		public EntityWithAttributeNamedSize parent;
		@OneToMany( mappedBy = "parent" )
		public Set<EntityWithAttributeNamedSize> children;
		@Column(name="`size`")
		private String size;
	}

}
