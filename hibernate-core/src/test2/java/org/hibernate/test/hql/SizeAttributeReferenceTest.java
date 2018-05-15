/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Historically HQL allowed syntax like {@code `... where someCollectionPath.size > 1`} where
 * size refers to the collection size.  However that disallows references to properties named
 * size.
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-10024" )
public class SizeAttributeReferenceTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void controlGroup() {
		Session session = openSession();
		session.getTransaction().begin();
		session.createQuery( "from EntityWithAttributeNamedSize e join e.children c where size(c) > 1" ).list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testSizeAttributeReference() {
		Session session = openSession();
		session.getTransaction().begin();
		session.createQuery( "from EntityWithAttributeNamedSize e join e.children c where c.size = 'abc'" ).list();
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
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
