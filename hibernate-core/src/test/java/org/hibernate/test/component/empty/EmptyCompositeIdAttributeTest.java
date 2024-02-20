/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.empty;

import java.io.Serializable;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Gail Badner
 */
public class EmptyCompositeIdAttributeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				EntityA.class,
				EntityB.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11898" )
	public void testPersistNullAttributeEmptyDisabled() {
		testPersistNullAttribute( false );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11898" )
	public void testPersistNullAttributeEmptyEnabled() {
		testPersistNullAttribute( true );
	}

	private void testPersistNullAttribute(boolean isEmptyEnabled) {
		rebuildSessionFactory( c -> c.setProperty(
									   AvailableSettings.CREATE_EMPTY_COMPOSITES_ENABLED,
									   String.valueOf( isEmptyEnabled )
							   )
		);

		int entityAId = doInHibernate(
				this::sessionFactory,
				session -> {
					final EntityB entityB = new EntityB();
					entityB.bpk = new BPK();
					// explicitly set the default int values to make clear
					// that these are "real" values
					entityB.bpk.intVal1 = 0;
					entityB.bpk.intVal2 = 0;
					session.persist( entityB );

					final EntityA entityA = new EntityA();
					// entityA.selectedEntityBPK is null
					session.persist( entityA );
					return entityA.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final EntityA entityA = session.get( EntityA.class, entityAId );
					if ( isEmptyEnabled ) {
						// entityA.selectedEntityBPK should be an empty composite
						assertNotNull( entityA.selectedEntityBPK );
						assertNull( session.get( EntityB.class, entityA.selectedEntityBPK ) );
					}
					else {
						assertNull( entityA.selectedEntityBPK );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11898" )
	public void testUpdateAttributeEmptyDisabled() {
		testUpdateAttribute( false );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11898" )
	public void testUpdateAttributeEmptyEnabled() {
		testUpdateAttribute( true );
	}

	private void testUpdateAttribute(boolean isEmptyEnabled) {
		rebuildSessionFactory( c -> c.setProperty(
									   AvailableSettings.CREATE_EMPTY_COMPOSITES_ENABLED,
									   String.valueOf( isEmptyEnabled )
							   )
		);

		int entityAId = doInHibernate(
				this::sessionFactory,
				session -> {
					final EntityB entityB = new EntityB();
					entityB.bpk = new BPK();
					// explicitly set the default int values to make clear
					// that these are "real" values
					entityB.bpk.intVal1 = 0;
					entityB.bpk.intVal2 = 0;
					session.persist( entityB );

					final EntityA entityA = new EntityA();
					// entityA.selectedEntityBPK is null
					session.persist( entityA );
					return entityA.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final EntityA entityA = session.get( EntityA.class, entityAId );
					entityA.selectedEntityBPK = new BPK();
					// explicitly set the default int values to make clear
					// that these are "real" values
					entityA.selectedEntityBPK.intVal1 = 0;
					entityA.selectedEntityBPK.intVal2 = 0;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final EntityA entityA = session.get( EntityA.class, entityAId );
					assertNotNull( entityA.selectedEntityBPK );
					assertNotNull( session.get( EntityB.class, entityA.selectedEntityBPK ) );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		@GeneratedValue
		private int id;

		@AttributeOverrides({
				@AttributeOverride(name = "intVal1", column = @Column(nullable = true)),
				@AttributeOverride(name = "intVal2", column = @Column(nullable = true))
		})
		private BPK selectedEntityBPK;
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@EmbeddedId
		private BPK bpk;
	}

	@Embeddable
	public static class BPK implements Serializable{
		private int intVal1;
		private int intVal2;
	}
}
