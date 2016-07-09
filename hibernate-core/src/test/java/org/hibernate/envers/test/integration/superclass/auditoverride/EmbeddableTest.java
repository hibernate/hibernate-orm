/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditoverride;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This class addresses numerous issues with Embeddable annotated classes
 * for issues {@code HHH-9228} and {@code HHH-9229}.
 *
 * @author Chris Cranford
 */
public class EmbeddableTest extends BaseEnversJPAFunctionalTestCase {
	private Integer simpleId;
	private Integer simpleOverrideId;
	private Integer simplePropertyId;
	private Integer fullOverrideId;
	private Integer notAuditedId;
	private Integer overridedId;
	private Integer auditedId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				AuditedEmbeddable.class,
				AuditedEmbeddableOverrideEntity.class,
				FullOverrideEmbeddable.class,
				FullOverrideEmbeddableEntity.class,
				NotAuditedEmbeddableEntity.class,
				OverrideEmbeddable.class,
				OverrideEmbeddableEntity.class,
				SimpleAbstractMappedSuperclass.class,
				SimpleEmbeddable.class,
				SimpleEmbeddableEntity.class,
				SimpleEmbeddableWithOverrideEntity.class,
				SimpleEmbeddableWithPropertyOverrideEntity.class
		};
	}

	@Test
	@Priority( 10 )
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {

			// entity 1
			SimpleEmbeddableEntity simple = new SimpleEmbeddableEntity();
			simple.setEmbeddable( new SimpleEmbeddable() );
			simple.getEmbeddable().setStrValue( "strValueSimple" );
			simple.getEmbeddable().setIntValue( 5 );

			// entity 2
			SimpleEmbeddableWithOverrideEntity simpleOverride = new SimpleEmbeddableWithOverrideEntity();
			simpleOverride.setEmbeddable( new SimpleEmbeddable() );
			simpleOverride.getEmbeddable().setStrValue( "strValueSimpleOverride" );
			simpleOverride.getEmbeddable().setIntValue( 10 );

			// entity 3
			SimpleEmbeddableWithPropertyOverrideEntity simpleProperty = new SimpleEmbeddableWithPropertyOverrideEntity();
			simpleProperty.setEmbeddable( new SimpleEmbeddable() );
			simpleProperty.getEmbeddable().setStrValue( "strValueSimpleMapped" );
			simpleProperty.getEmbeddable().setIntValue( 15 );

			// entity 4
			FullOverrideEmbeddableEntity fullOverride = new FullOverrideEmbeddableEntity();
			fullOverride.setEmbeddable( new FullOverrideEmbeddable() );
			fullOverride.getEmbeddable().setStrValue( "strValueFull" );
			fullOverride.getEmbeddable().setIntValue( 20 );

			// entity 5
			NotAuditedEmbeddableEntity notAudited = new NotAuditedEmbeddableEntity();
			notAudited.setEmbeddable( new FullOverrideEmbeddable() );
			notAudited.getEmbeddable().setStrValue( "strValueNotAudited" );
			notAudited.getEmbeddable().setIntValue( 25 );

			// entity 6
			OverrideEmbeddableEntity overrided = new OverrideEmbeddableEntity();
			overrided.setEmbeddable( new OverrideEmbeddable() );
			overrided.getEmbeddable().setStrValue( "strValueOver" );
			overrided.getEmbeddable().setIntValue( 30 );

			// entity 7
			AuditedEmbeddableOverrideEntity audited = new AuditedEmbeddableOverrideEntity();
			audited.setEmbeddable( new AuditedEmbeddable() );
			audited.getEmbeddable().setStrValue( "strValueAudited" );
			audited.getEmbeddable().setIntValue( 35 );
			audited.getEmbeddable().setValue( 1024 );

			entityManager.getTransaction().begin();
			entityManager.persist( simple );
			entityManager.persist( simpleOverride );
			entityManager.persist( simpleProperty );
			entityManager.persist( fullOverride );
			entityManager.persist( notAudited );
			entityManager.persist( overrided );
			entityManager.persist( audited );
			entityManager.getTransaction().commit();

			this.simpleId = simple.getId();
			this.simpleOverrideId = simpleOverride.getId();
			this.simplePropertyId = simpleProperty.getId();
			this.fullOverrideId = fullOverride.getId();
			this.notAuditedId = notAudited.getId();
			this.overridedId = overrided.getId();
			this.auditedId = audited.getId();
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9228" )
	public void testAuditOverrideOnAuditedEmbeddable() {
		final PersistentClass clazz = getPersistentClass( AuditedEmbeddableOverrideEntity.class, auditedId, 1 );
		assertTrue( clazz.hasProperty( "name" ) );
		// verify non-audited fields are excluded from mappings.
		assertFalse( clazz.hasProperty( "embeddable_value" ) );
		assertFalse( clazz.hasProperty( "embeddable_intValue" ) );
		assertFalse( clazz.hasProperty( "embeddable_strValue" ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9229" )
	public void testEmptyEmbeddableWithFullAudit() {
		final PersistentClass clazz = getPersistentClass( FullOverrideEmbeddableEntity.class, fullOverrideId, 1 );
		assertTrue( clazz.hasProperty( "embeddable_intValue" ) );
		assertTrue( clazz.hasProperty( "embeddable_strValue" ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9229" )
	public void testEmptyEmbeddableWithNoAudited() {
		final PersistentClass clazz = getPersistentClass( NotAuditedEmbeddableEntity.class, notAuditedId, 1 );
		// not mapped because @NotAudited should override any other behavior.
		assertFalse( clazz.hasProperty( "embeddable_intValue" ) );
		assertFalse( clazz.hasProperty( "embeddable_strValue" ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9229" )
	public void testEmptyEmbeddableWithAuditOverride() {
		final PersistentClass clazz = getPersistentClass( OverrideEmbeddableEntity.class, overridedId, 1 );
		assertFalse( clazz.hasProperty( "embeddable_strValue" ) );
		assertTrue( clazz.hasProperty( "embeddable_intValue" ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9229" )
	public void testEmptyEmbeddableNoAuditOverrides() {
		final PersistentClass clazz = getPersistentClass( SimpleEmbeddableEntity.class, simpleId, 1 );
		assertFalse( clazz.hasProperty( "embeddable_strValue" ) );
		assertFalse( clazz.hasProperty( "embeddable_intValue" ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9229" )
	public void testEmptyEmbeddableWithAuditOverrideForMappedSuperclass() {
		final PersistentClass clazz = getPersistentClass(
				SimpleEmbeddableWithOverrideEntity.class,
				simpleOverrideId,
				1
		);
		assertTrue( clazz.hasProperty( "embeddable_strValue" ) );
		assertTrue( clazz.hasProperty( "embeddable_intValue" ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9229" )
	public void testEmptyEmbeddableWithPropertyAuditOverride() {
		final PersistentClass clazz = getPersistentClass(
				SimpleEmbeddableWithPropertyOverrideEntity.class,
				simplePropertyId,
				1
		);
		assertFalse( clazz.hasProperty( "embeddable_strValue" ) );
		assertTrue( clazz.hasProperty( "embeddable_intValue" ) );
	}

	// represents a @MappedSuperclass with no overrides
	@MappedSuperclass
	public static abstract class SimpleAbstractMappedSuperclass {
		private String strValue;
		private Integer intValue;

		public Integer getIntValue() {
			return intValue;
		}

		public void setIntValue(Integer intValue) {
			this.intValue = intValue;
		}

		public String getStrValue() {
			return strValue;
		}

		public void setStrValue(String strValue) {
			this.strValue = strValue;
		}
	}

	// an embeddable that should introduce no audited properties
	@Embeddable
	public static class SimpleEmbeddable extends SimpleAbstractMappedSuperclass {

	}

	// an embeddable that should introduce 'intValue' as audited based on audit overrides locally
	@Embeddable
	@AuditOverride(forClass = SimpleAbstractMappedSuperclass.class, name = "intValue")
	public static class OverrideEmbeddable extends SimpleAbstractMappedSuperclass {

	}

	// an embedddable that introduces all audited values base don audit overrides locally.
	@Embeddable
	@AuditOverride(forClass = SimpleAbstractMappedSuperclass.class)
	public static class FullOverrideEmbeddable extends SimpleAbstractMappedSuperclass {

	}

	@Embeddable
	@Audited
	public static class AuditedEmbeddable extends SimpleAbstractMappedSuperclass {
		private Integer value;

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}
	}

	@Entity
	@Audited
	public static class AuditedEmbeddableOverrideEntity {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;
		@Embedded
		@AuditOverrides({
				@AuditOverride(name = "value", isAudited = false),
				@AuditOverride(forClass = SimpleAbstractMappedSuperclass.class, isAudited = false)
		})
		private AuditedEmbeddable embeddable;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public AuditedEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(AuditedEmbeddable embeddable) {
			this.embeddable = embeddable;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	@Audited
	public static class NotAuditedEmbeddableEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@Embedded
		@NotAudited
		private FullOverrideEmbeddable embeddable;

		public FullOverrideEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(FullOverrideEmbeddable embeddable) {
			this.embeddable = embeddable;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	@Audited
	public static class FullOverrideEmbeddableEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@Embedded
		private FullOverrideEmbeddable embeddable;

		public FullOverrideEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(FullOverrideEmbeddable embeddable) {
			this.embeddable = embeddable;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	@Audited
	public static class OverrideEmbeddableEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@Embedded
		private OverrideEmbeddable embeddable;

		public OverrideEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(OverrideEmbeddable embeddable) {
			this.embeddable = embeddable;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	@Audited
	public static class SimpleEmbeddableWithPropertyOverrideEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@Embedded
		@AuditOverride(name = "intValue", forClass = SimpleAbstractMappedSuperclass.class)
		private SimpleEmbeddable embeddable;

		public SimpleEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(SimpleEmbeddable embeddable) {
			this.embeddable = embeddable;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity
	@Audited
	public static class SimpleEmbeddableEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@Embedded
		private SimpleEmbeddable embeddable;

		public SimpleEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(SimpleEmbeddable embeddable) {
			this.embeddable = embeddable;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}


	@Entity
	@Audited
	public static class SimpleEmbeddableWithOverrideEntity {
		@Id
		@GeneratedValue
		private Integer id;
		@Embedded
		@AuditOverride(forClass = SimpleAbstractMappedSuperclass.class)
		private SimpleEmbeddable embeddable;

		public SimpleEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(SimpleEmbeddable embeddable) {
			this.embeddable = embeddable;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	private PersistentClass getPersistentClass(Class<?> clazz, Object id, Number revision) {
		final Object entity = getAuditReader().find( clazz, id, revision );
		return metadata().getEntityBinding( getAuditReader().getEntityName( id, revision, entity ) + "_AUD" );
	}
}
