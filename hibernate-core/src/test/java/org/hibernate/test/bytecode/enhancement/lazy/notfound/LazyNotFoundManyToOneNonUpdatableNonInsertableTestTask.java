/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.notfound;

/**
 * @author Gail Badner
 */

import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@TestForIssue( jiraKey = "HHH-12226")
public class LazyNotFoundManyToOneNonUpdatableNonInsertableTestTask extends AbstractEnhancerTestTask {
	private static int ID = 1;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				User.class,
				Lazy.class
		};
	}

	@Override
	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session session = getFactory().openSession();
		session.beginTransaction();
		{
					Lazy p = new Lazy();
					p.id = ID;
					User u = new User();
					u.id = ID;
					u.setLazy( p );
					session.persist( u );
		}
		session.getTransaction().commit();
		session.close();

		session = getFactory().openSession();
		session.beginTransaction();
		{
					session.delete( session.get( Lazy.class, ID ) );
		}
		session.getTransaction().commit();
		session.close();
	}

	@Override
	public void execute() {
		Session session = getFactory().openSession();
		session.beginTransaction();
		{
					User user = session.get( User.class, ID );
					assertFalse( Hibernate.isPropertyInitialized( user, "lazy" ) );
					assertNull( user.getLazy() );
		}
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected void cleanup() {
	}

	@Entity(name="User")
	@Table(name = "USER_TABLE")
	public static class User {

		@Id
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
		@LazyToOne(value = LazyToOneOption.NO_PROXY)
		@NotFound(action = NotFoundAction.IGNORE)
		@JoinColumn(
				foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT),
				name = "id",
				referencedColumnName = "id",
				insertable = false,
				updatable = false
		)
		private Lazy lazy;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Lazy getLazy() {
			return lazy;
		}

		public void setLazy(Lazy lazy) {
			this.lazy = lazy;
		}

	}

	@Entity(name = "Lazy")
	@Table(name = "LAZY")
	public static class Lazy {
		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
