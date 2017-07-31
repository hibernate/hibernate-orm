/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.envers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.RevisionType;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class EntityTypeChangeAuditTrackingRevisionListenerTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Customer.class,
			CustomTrackingRevisionEntity.class,
			EntityType.class
		};
	}

	@Test
	public void testLifecycle() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();
			customer.setId( 1L );
			customer.setFirstName( "John" );
			customer.setLastName( "Doe" );

			entityManager.persist( customer );
		} );

		EntityManagerFactory entityManagerFactory = null;
		try {
			Map settings = buildSettings();
			settings.put(
				org.hibernate.jpa.AvailableSettings.LOADED_CLASSES,
				Arrays.asList(
					ApplicationCustomer.class,
					CustomTrackingRevisionEntity.class,
					EntityType.class
				)
			);
			settings.put(
					AvailableSettings.HBM2DDL_AUTO,
					"update"
			);
			entityManagerFactory =  Bootstrap.getEntityManagerFactoryBuilder(
					new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
					settings
			).build().unwrap( SessionFactoryImplementor.class );

			final EntityManagerFactory emf = entityManagerFactory;

			doInJPA( () -> emf, entityManager -> {
				ApplicationCustomer customer = new ApplicationCustomer();
				customer.setId( 2L );
				customer.setFirstName( "John" );
				customer.setLastName( "Doe Jr." );

				entityManager.persist( customer );
			} );

			doInJPA( () -> emf, entityManager -> {
				//tag::envers-tracking-modified-entities-revchanges-query-example[]
				AuditReader auditReader = AuditReaderFactory.get( entityManager );

				List<Number> revisions = auditReader.getRevisions(
					ApplicationCustomer.class,
					1L
				);

				CustomTrackingRevisionEntity revEntity = auditReader.findRevision(
					CustomTrackingRevisionEntity.class,
					revisions.get( 0 )
				);

				Set<EntityType> modifiedEntityTypes = revEntity.getModifiedEntityTypes();
				assertEquals( 1, modifiedEntityTypes.size() );

				EntityType entityType = modifiedEntityTypes.iterator().next();
				assertEquals(
					Customer.class.getName(),
					entityType.getEntityClassName()
				);
				//end::envers-tracking-modified-entities-revchanges-query-example[]
			} );
		}
		finally {
			if ( entityManagerFactory != null ) {
				entityManagerFactory.close();
			}
		}
	}

	@Audited
	@Entity(name = "Customer")
	public static class Customer {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Temporal( TemporalType.TIMESTAMP )
		@Column(name = "created_on")
		@CreationTimestamp
		private Date createdOn;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}
	}

	@Audited
	@Entity(name = "Customer")
	public static class ApplicationCustomer {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Temporal( TemporalType.TIMESTAMP )
		@Column(name = "created_on")
		@CreationTimestamp
		private Date createdOn;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}
	}

	//tag::envers-tracking-modified-entities-revchanges-EntityTrackingRevisionListener-example[]
	public static class CustomTrackingRevisionListener implements EntityTrackingRevisionListener {

		@Override
		public void entityChanged(Class entityClass,
								  String entityName,
								  Serializable entityId,
								  RevisionType revisionType,
								  Object revisionEntity ) {
			String type = entityClass.getName();
			( (CustomTrackingRevisionEntity) revisionEntity ).addModifiedEntityType( type );
		}

		@Override
		public void newRevision( Object revisionEntity ) {
		}
	}
	//end::envers-tracking-modified-entities-revchanges-EntityTrackingRevisionListener-example[]

	//tag::envers-tracking-modified-entities-revchanges-RevisionEntity-example[]
	@Entity(name = "CustomTrackingRevisionEntity")
	@Table(name = "TRACKING_REV_INFO")
	@RevisionEntity( CustomTrackingRevisionListener.class )
	public static class CustomTrackingRevisionEntity {

		@Id
		@GeneratedValue
		@RevisionNumber
		private int customId;

		@RevisionTimestamp
		private long customTimestamp;

		@OneToMany(
			mappedBy="revision",
			cascade={
				CascadeType.PERSIST,
				CascadeType.REMOVE
			}
		)
		private Set<EntityType> modifiedEntityTypes = new HashSet<>();

		public Set<EntityType> getModifiedEntityTypes() {
			return modifiedEntityTypes;
		}

		public void addModifiedEntityType(String entityClassName ) {
			modifiedEntityTypes.add( new EntityType( this, entityClassName ) );
		}
	}
	//end::envers-tracking-modified-entities-revchanges-RevisionEntity-example[]

	//tag::envers-tracking-modified-entities-revchanges-EntityType-example[]
	@Entity(name = "EntityType")
	public static class EntityType {

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private CustomTrackingRevisionEntity revision;

		private String entityClassName;

		private EntityType() {
		}

		public EntityType(CustomTrackingRevisionEntity revision, String entityClassName) {
			this.revision = revision;
			this.entityClassName = entityClassName;
		}

		//Getters and setters are omitted for brevity
	//end::envers-tracking-modified-entities-revchanges-EntityType-example[]

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public CustomTrackingRevisionEntity getRevision() {
			return revision;
		}

		public void setRevision(CustomTrackingRevisionEntity revision) {
			this.revision = revision;
		}

		public String getEntityClassName() {
			return entityClassName;
		}

		public void setEntityClassName(String entityClassName) {
			this.entityClassName = entityClassName;
		}
	//tag::envers-tracking-modified-entities-revchanges-EntityType-example[]
	}
	//end::envers-tracking-modified-entities-revchanges-EntityType-example[]
}
