/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.dirty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderService;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Christian Beikov
 */
@TestForIssue(jiraKey = "HHH-14360")
@RunWith(BytecodeEnhancerRunner.class)
public class DirtyTrackingPersistTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { HotherEntity.class, Hentity.class };
	}

	@Override
	protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addService(
				SessionFactoryBuilderService.class,
				(SessionFactoryBuilderService) (metadata, bootstrapContext) -> {
					SessionFactoryOptionsBuilder optionsBuilder = new SessionFactoryOptionsBuilder(
							metadata.getMetadataBuildingOptions().getServiceRegistry(),
							bootstrapContext
					);
					optionsBuilder.enableCollectionInDefaultFetchGroup( true );
					return new SessionFactoryBuilderImpl( metadata, optionsBuilder );
				}
		);
	}

	@Test
	public void test() {
		Hentity hentity = new Hentity();
		HotherEntity hotherEntity = new HotherEntity();
		hentity.setLineItems( new ArrayList<>( Collections.singletonList( hotherEntity ) ) );
		hentity.setNextRevUNs( new ArrayList<>( Collections.singletonList( "something" ) ) );
		doInHibernate( this::sessionFactory, session -> {
			session.persist( hentity );
		} );
		doInHibernate( this::sessionFactory, session -> {
			hentity.bumpNumber();
			session.saveOrUpdate( hentity );
		} );
	}

	// --- //

	@Entity(name = "HotherEntity")
	public static class HotherEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		@Basic
		private Long clicId;

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public Long getClicId() {
			return clicId;
		}

		public void setClicId(Long clicId) {
			this.clicId = clicId;
		}
	}

	@Entity(name = "Hentity")
	public static class Hentity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@ElementCollection
		@OrderColumn(name = "nextRevUN_index")
		private List<String> nextRevUNs;

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@JoinColumn(name = "clicId")
		@OrderBy("id asc")
		protected List<HotherEntity> lineItems;

		@Basic
		private Long aNumber;

		@Temporal(value = TemporalType.TIMESTAMP)
		private Date createDate;

		@Temporal(value = TemporalType.TIMESTAMP)
		private Date deleteDate;

		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public List<String> getNextRevUNs() {
			return nextRevUNs;
		}

		public void setNextRevUNs(List<String> nextRevUNs) {
			this.nextRevUNs = nextRevUNs;
		}

		public List<HotherEntity> getLineItems() {
			return lineItems;
		}

		public void setLineItems(List<HotherEntity> lineItems) {
			this.lineItems = lineItems;
		}

		public Date getCreateDate() {
			return createDate;
		}

		public void setCreateDate(Date createDate) {
			this.createDate = createDate;
		}

		public Date getDeleteDate() {
			return deleteDate;
		}

		public void setDeleteDate(Date deleteDate) {
			this.deleteDate = deleteDate;
		}

		public void bumpNumber() {
			aNumber = aNumber == null ? 0 : aNumber++;
		}
	}
}
