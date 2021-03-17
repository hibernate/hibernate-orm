/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditoverride;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.UnknownEntityTypeException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.envers.test.tools.TestTools.checkCollection;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12913")
public class AuditOverrideAuditJoinTableTest extends BaseEnversJPAFunctionalTestCase {
	private Long entityId;
	private Long overrideEntityId;
	private Long auditParentsEntityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				OtherAuditedEntity.class,
				OtherOverrideAuditedEntity.class,
				OtherAuditParentsAuditEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1 - Persist audited subclass with non-audited super-type
		entityId = doInJPA( this::entityManagerFactory, entityManager -> {
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

			entityManager.persist( entity );

			return entity.getId();
		} );

		// Revision 2 - Persist audited subclass with audit-override non-audited super-type
		overrideEntityId = doInJPA( this::entityManagerFactory, entityManager -> {
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

			entityManager.persist( entity );

			return entity.getId();
		} );

		// Revision 3 - Persist audited subclass with audit-parents on non-audited super-type
		auditParentsEntityId = doInJPA( this::entityManagerFactory, entityManager -> {
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

			entityManager.persist( entity );

			return entity.getId();
		} );
	}

	@Test
	public void testMetadataAuditSuperClassWithAuditJoinTable() {
		try {
			entityManagerFactory().unwrap( SessionFactoryImplementor.class )
					.getMetamodel()
					.locateEntityPersister( "SuperClass_StringList" );
		}
		catch ( UnknownEntityTypeException e ) {
			fail( "Expected to find an entity-persister for the string-list in the super audit type" );
		}
	}

	@Test
	public void testMetadataNonAuditedSuperClassWithOverrideAuditJoinTable() {
		try {
			entityManagerFactory().unwrap( SessionFactoryImplementor.class )
					.getMetamodel()
					.locateEntityPersister( "OOAE_StringList" );
		}
		catch ( UnknownEntityTypeException e ) {
			fail( "Expected to find an entity-persister for the string-list in the super audit type" );
		}
	}

	@Test
	public void testMetadataNonAuditedSuperClassWithAuditParentsOverrideAuditJoinTable() {
		try {
			entityManagerFactory().unwrap( SessionFactoryImplementor.class )
					.getMetamodel()
					.locateEntityPersister( "OAPAE_StringList" );
		}
		catch ( UnknownEntityTypeException e ) {
			fail( "Expected to find an entity-persister for the string-list in the super audit type" );
		}
	}

	@Test
	public void testNonAuditedSuperclassAuditJoinTableHistory() {
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( OtherAuditedEntity.class,  entityId ) );

		OtherAuditedEntity rev = getAuditReader().find( OtherAuditedEntity.class, entityId, 1 );
		assertNotNull( rev );
		assertEquals( 2, rev.getSuperStringList().size() );
		checkCollection( rev.getSuperStringList(), "Entry1", "Entry2" );
	}

	@Test
	public void testNonAuditedSuperclassWithOverrideAuditJoinTableHistory() {
		assertEquals( Arrays.asList( 2 ), getAuditReader().getRevisions( OtherOverrideAuditedEntity.class, overrideEntityId ) );

		OtherOverrideAuditedEntity rev = getAuditReader().find( OtherOverrideAuditedEntity.class, overrideEntityId, 2 );
		assertNotNull( rev );
		assertEquals( 2, rev.getSuperStringList().size() );
		checkCollection( rev.getSuperStringList(), "Entry1", "Entry2" );
	}

	@Test
	public void testNonAuditedSuperclassWithAuditParentsOverrideAuditJoinTableHistory() {
		assertEquals( Arrays.asList( 3 ), getAuditReader().getRevisions( OtherAuditParentsAuditEntity.class, auditParentsEntityId ) );

		OtherAuditParentsAuditEntity rev = getAuditReader().find( OtherAuditParentsAuditEntity.class, auditParentsEntityId, 3 );
		assertNotNull( rev );
		assertEquals( 2, rev.getSuperStringList().size() );
		checkCollection( rev.getSuperStringList(), "Entry1", "Entry2" );
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
