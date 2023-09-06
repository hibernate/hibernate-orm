/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@JiraKey("HHH-17173")
@RunWith(BytecodeEnhancerRunner.class)
public class OneToOnEnhancedEntityLoadedAsReferenceTest extends BaseCoreFunctionalTestCase {

	private long entityId;
	private long containedEntityId;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ContainingEntity.class, ContainedEntity.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "10" );
		configuration.setProperty( AvailableSettings.MAX_FETCH_DEPTH, "0" );
	}

	@Before
	public void prepare() {
		doInJPA( this::sessionFactory, em -> {
			ContainingEntity entity = new ContainingEntity();
			ContainedEntity containedEntity = new ContainedEntity();
			containedEntity.setValue( "value" );
			entity.setOneToOneMappedBy( containedEntity );
			containedEntity.setOneToOne( entity );

			em.persist( entity );
			em.persist( containedEntity );
			entityId = entity.getId();
			containedEntityId = containedEntity.getId();
		} );
	}

	@Test
	public void test() {
		doInJPA( this::sessionFactory, em -> {
			ContainingEntity entity = em.getReference( ContainingEntity.class, entityId );
			ContainedEntity containedEntity = em.getReference( ContainedEntity.class, containedEntityId );
			// We're working on an uninitialized proxy.
			assertThat( entity ).returns( false, Hibernate::isInitialized );
			// The above should have persisted a value that passes the assertion.
			assertThat( entity.getOneToOneMappedBy() ).isEqualTo( containedEntity );
			assertThat( entity.getOneToOneMappedBy().getValue() ).isEqualTo( "value" );
			// Accessing the value should trigger initialization of the proxy.
			assertThat( entity ).returns( true, Hibernate::isInitialized );
		} );
	}

	@Entity
	public static class ContainingEntity {

		@Id
		@GeneratedValue
		public long id;

		@OneToOne(mappedBy = "oneToOne")
		private ContainedEntity oneToOneMappedBy;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public ContainedEntity getOneToOneMappedBy() {
			return oneToOneMappedBy;
		}

		public void setOneToOneMappedBy(ContainedEntity oneToOneMappedBy) {
			this.oneToOneMappedBy = oneToOneMappedBy;
		}
	}

	@Entity
	public static class ContainedEntity {

		@Id
		@GeneratedValue
		private long id;

		@Column(name = "value_")
		private String value;

		@OneToOne
		private ContainingEntity oneToOne;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public ContainingEntity getOneToOne() {
			return oneToOne;
		}

		public void setOneToOne(ContainingEntity oneToOne) {
			this.oneToOne = oneToOne;
		}

	}
}
