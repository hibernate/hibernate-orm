/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditoverride;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.UnknownEntityTypeException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.envers.tools.TestTools.checkCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12913")
@EnversTest
@Jpa(annotatedClasses = {
		AuditOverrideAuditJoinTableTest.OtherAuditedEntity.class,
		AuditOverrideAuditJoinTableTest.OtherOverrideAuditedEntity.class,
		AuditOverrideAuditJoinTableTest.OtherAuditParentsAuditEntity.class
})
public class AuditOverrideAuditJoinTableTest {
	private Long entityId;
	private Long overrideEntityId;
	private Long auditParentsEntityId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - Persist audited subclass with non-audited super-type
		entityId = scope.fromTransaction( em -> {
			final OtherAuditedEntity entity = new OtherAuditedEntity();
			entity.setId( 1 );
			entity.setVersion( Timestamp.valueOf( LocalDateTime.now() ) );
			entity.setSuperValue( "SuperValue" );
			entity.setValue( "Value" );
			entity.setNotAuditedValue( "NotAuditedValue" );

			List<String> list = new ArrayList<>();
			list.add( "Entry1" );
			list.add( "Entry2" );
			entity.setSuperStringList( list );

			em.persist( entity );

			return entity.getId();
		} );

		// Revision 2 - Persist audited subclass with audit-override non-audited super-type
		overrideEntityId = scope.fromTransaction( em -> {
			final OtherOverrideAuditedEntity entity = new OtherOverrideAuditedEntity();
			entity.setId( 1 );
			entity.setVersion( Timestamp.valueOf( LocalDateTime.now() ) );
			entity.setSuperValue( "SuperValue" );
			entity.setValue( "Value" );
			entity.setNotAuditedValue( "NotAuditedValue" );

			List<String> list = new ArrayList<>();
			list.add( "Entry1" );
			list.add( "Entry2" );
			entity.setSuperStringList( list );

			em.persist( entity );

			return entity.getId();
		} );

		// Revision 3 - Persist audited subclass with audit-parents on non-audited super-type
		auditParentsEntityId = scope.fromTransaction( em -> {
			final OtherAuditParentsAuditEntity entity = new OtherAuditParentsAuditEntity();
			entity.setId( 1 );
			entity.setVersion( Timestamp.valueOf( LocalDateTime.now() ) );
			entity.setSuperValue( "SuperValue" );
			entity.setValue( "Value" );
			entity.setNotAuditedValue( "NotAuditedValue" );

			List<String> list = new ArrayList<>();
			list.add( "Entry1" );
			list.add( "Entry2" );
			entity.setSuperStringList( list );

			em.persist( entity );

			return entity.getId();
		} );
	}

	@Test
	public void testMetadataAuditSuperClassWithAuditJoinTable(EntityManagerFactoryScope scope) {
		try {
			scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
					.getMappingMetamodel()
					.getEntityDescriptor( "SuperClass_StringList" );
		}
		catch ( UnknownEntityTypeException e ) {
			fail( "Expected to find an entity-persister for the string-list in the super audit type" );
		}
	}

	@Test
	public void testMetadataNonAuditedSuperClassWithOverrideAuditJoinTable(EntityManagerFactoryScope scope) {
		try {
			scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
					.getMappingMetamodel()
					.getEntityDescriptor( "OOAE_StringList" );
		}
		catch ( UnknownEntityTypeException e ) {
			fail( "Expected to find an entity-persister for the string-list in the super audit type" );
		}
	}

	@Test
	public void testMetadataNonAuditedSuperClassWithAuditParentsOverrideAuditJoinTable(EntityManagerFactoryScope scope) {
		try {
			scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
					.getMappingMetamodel()
					.getEntityDescriptor( "OAPAE_StringList" );
		}
		catch ( UnknownEntityTypeException e ) {
			fail( "Expected to find an entity-persister for the string-list in the super audit type" );
		}
	}

	@Test
	public void testNonAuditedSuperclassAuditJoinTableHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1 ), auditReader.getRevisions( OtherAuditedEntity.class,  entityId ) );

			OtherAuditedEntity rev = auditReader.find( OtherAuditedEntity.class, entityId, 1 );
			assertNotNull( rev );
			assertEquals( 2, rev.getSuperStringList().size() );
			checkCollection( rev.getSuperStringList(), "Entry1", "Entry2" );
		} );
	}

	@Test
	public void testNonAuditedSuperclassWithOverrideAuditJoinTableHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 2 ), auditReader.getRevisions( OtherOverrideAuditedEntity.class, overrideEntityId ) );

			OtherOverrideAuditedEntity rev = auditReader.find( OtherOverrideAuditedEntity.class, overrideEntityId, 2 );
			assertNotNull( rev );
			assertEquals( 2, rev.getSuperStringList().size() );
			checkCollection( rev.getSuperStringList(), "Entry1", "Entry2" );
		} );
	}

	@Test
	public void testNonAuditedSuperclassWithAuditParentsOverrideAuditJoinTableHistory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 3 ), auditReader.getRevisions( OtherAuditParentsAuditEntity.class, auditParentsEntityId ) );

			OtherAuditParentsAuditEntity rev = auditReader.find( OtherAuditParentsAuditEntity.class, auditParentsEntityId, 3 );
			assertNotNull( rev );
			assertEquals( 2, rev.getSuperStringList().size() );
			checkCollection( rev.getSuperStringList(), "Entry1", "Entry2" );
		} );
	}

	@MappedSuperclass
	@Audited
	public static class SuperClass {
		@Id
		private long id;
		private Timestamp version;
		private String superValue;
		@ElementCollection
		@AuditJoinTable(name = "SuperClass_StringList")
		private List<String> superStringList;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Timestamp getVersion() {
			return version;
		}

		public void setVersion(Timestamp version) {
			this.version = version;
		}

		public String getSuperValue() {
			return superValue;
		}

		public void setSuperValue(String superValue) {
			this.superValue = superValue;
		}

		public List<String> getSuperStringList() {
			return superStringList;
		}

		public void setSuperStringList(List<String> superStringList) {
			this.superStringList = superStringList;
		}
	}

	@Entity(name = "OOE")
	@Audited
	public static class OtherAuditedEntity extends SuperClass {
		@Column(name = "val")
		private String value;
		@NotAudited
		private String notAuditedValue;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getNotAuditedValue() {
			return notAuditedValue;
		}

		public void setNotAuditedValue(String notAuditedValue) {
			this.notAuditedValue = notAuditedValue;
		}
	}

	@MappedSuperclass
	public static class NonAuditedSuperClass {
		@Id
		private long id;
		private Timestamp version;
		private String superValue;
		@ElementCollection
		@AuditJoinTable(name = "NASC_StringList")
		private List<String> superStringList;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public Timestamp getVersion() {
			return version;
		}

		public void setVersion(Timestamp version) {
			this.version = version;
		}

		public String getSuperValue() {
			return superValue;
		}

		public void setSuperValue(String superValue) {
			this.superValue = superValue;
		}

		public List<String> getSuperStringList() {
			return superStringList;
		}

		public void setSuperStringList(List<String> superStringList) {
			this.superStringList = superStringList;
		}
	}

	@Entity(name = "OOAE")
	@Audited
	@AuditOverrides({
			@AuditOverride(
					forClass = NonAuditedSuperClass.class,
					name = "superStringList",
					auditJoinTable = @AuditJoinTable(name = "OOAE_StringList")
			)
	})
	public static class OtherOverrideAuditedEntity extends NonAuditedSuperClass {
		@Column(name = "val")
		private String value;
		@NotAudited
		private String notAuditedValue;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getNotAuditedValue() {
			return notAuditedValue;
		}

		public void setNotAuditedValue(String notAuditedValue) {
			this.notAuditedValue = notAuditedValue;
		}
	}

	@Entity(name = "OAPAE")
	@Audited(auditParents = NonAuditedSuperClass.class)
	@AuditOverrides({
			@AuditOverride(
					forClass = NonAuditedSuperClass.class,
					name = "superStringList",
					auditJoinTable = @AuditJoinTable(name = "OAPAE_StringList")
			)
	})
	public static class OtherAuditParentsAuditEntity extends NonAuditedSuperClass {
		@Column(name = "val")
		private String value;
		@NotAudited
		private String notAuditedValue;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getNotAuditedValue() {
			return notAuditedValue;
		}

		public void setNotAuditedValue(String notAuditedValue) {
			this.notAuditedValue = notAuditedValue;
		}
	}
}
