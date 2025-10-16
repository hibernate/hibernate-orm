/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditoverride;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class addresses numerous issues with Embeddable annotated classes
 * for issues {@code HHH-9228} and {@code HHH-9229}.
 *
 * @author Chris Cranford
 */
@EnversTest
@DomainModel(annotatedClasses = {
		EmbeddableTest.AuditedEmbeddable.class,
		EmbeddableTest.AuditedEmbeddableOverrideEntity.class,
		EmbeddableTest.FullOverrideEmbeddable.class,
		EmbeddableTest.FullOverrideEmbeddableEntity.class,
		EmbeddableTest.NotAuditedEmbeddableEntity.class,
		EmbeddableTest.OverrideEmbeddable.class,
		EmbeddableTest.OverrideEmbeddableEntity.class,
		EmbeddableTest.SimpleAbstractMappedSuperclass.class,
		EmbeddableTest.SimpleEmbeddable.class,
		EmbeddableTest.SimpleEmbeddableEntity.class,
		EmbeddableTest.SimpleEmbeddableWithOverrideEntity.class,
		EmbeddableTest.SimpleEmbeddableWithPropertyOverrideEntity.class
})
@SessionFactory
public class EmbeddableTest {
	private Integer simpleId;
	private Integer simpleOverrideId;
	private Integer simplePropertyId;
	private Integer fullOverrideId;
	private Integer notAuditedId;
	private Integer overridedId;
	private Integer auditedId;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
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

			em.persist( simple );
			em.persist( simpleOverride );
			em.persist( simpleProperty );
			em.persist( fullOverride );
			em.persist( notAudited );
			em.persist( overrided );
			em.persist( audited );

			this.simpleId = simple.getId();
			this.simpleOverrideId = simpleOverride.getId();
			this.simplePropertyId = simpleProperty.getId();
			this.fullOverrideId = fullOverride.getId();
			this.notAuditedId = notAudited.getId();
			this.overridedId = overrided.getId();
			this.auditedId = audited.getId();
		} );
	}

	@Test
	@JiraKey(value = "HHH-9228")
	public void testAuditOverrideOnAuditedEmbeddable(DomainModelScope dm, SessionFactoryScope sf) {
		final PersistentClass clazz = getPersistentClass( dm, sf, AuditedEmbeddableOverrideEntity.class, auditedId, 1 );
		assertTrue( clazz.hasProperty( "name" ) );
		// verify non-audited fields are excluded from mappings.
		assertFalse( clazz.hasProperty( "embeddable_value" ) );
		assertFalse( clazz.hasProperty( "embeddable_intValue" ) );
		assertFalse( clazz.hasProperty( "embeddable_strValue" ) );
	}

	@Test
	@JiraKey(value = "HHH-9229")
	public void testEmptyEmbeddableWithFullAudit(DomainModelScope dm, SessionFactoryScope sf) {
		final PersistentClass clazz = getPersistentClass( dm, sf, FullOverrideEmbeddableEntity.class, fullOverrideId,
				1 );
		assertTrue( clazz.hasProperty( "embeddable_intValue" ) );
		assertTrue( clazz.hasProperty( "embeddable_strValue" ) );
	}

	@Test
	@JiraKey(value = "HHH-9229")
	public void testEmptyEmbeddableWithNoAudited(DomainModelScope dm, SessionFactoryScope sf) {
		final PersistentClass clazz = getPersistentClass( dm, sf, NotAuditedEmbeddableEntity.class, notAuditedId, 1 );
		// not mapped because @NotAudited should override any other behavior.
		assertFalse( clazz.hasProperty( "embeddable_intValue" ) );
		assertFalse( clazz.hasProperty( "embeddable_strValue" ) );
	}

	@Test
	@JiraKey(value = "HHH-9229")
	public void testEmptyEmbeddableWithAuditOverride(DomainModelScope dm, SessionFactoryScope sf) {
		final PersistentClass clazz = getPersistentClass( dm, sf, OverrideEmbeddableEntity.class, overridedId, 1 );
		assertFalse( clazz.hasProperty( "embeddable_strValue" ) );
		assertTrue( clazz.hasProperty( "embeddable_intValue" ) );
	}

	@Test
	@JiraKey(value = "HHH-9229")
	public void testEmptyEmbeddableNoAuditOverrides(DomainModelScope dm, SessionFactoryScope sf) {
		final PersistentClass clazz = getPersistentClass( dm, sf, SimpleEmbeddableEntity.class, simpleId, 1 );
		assertFalse( clazz.hasProperty( "embeddable_strValue" ) );
		assertFalse( clazz.hasProperty( "embeddable_intValue" ) );
	}

	@Test
	@JiraKey(value = "HHH-9229")
	public void testEmptyEmbeddableWithAuditOverrideForMappedSuperclass(DomainModelScope dm, SessionFactoryScope sf) {
		final PersistentClass clazz = getPersistentClass(
				dm,
				sf,
				SimpleEmbeddableWithOverrideEntity.class,
				simpleOverrideId,
				1
		);
		assertTrue( clazz.hasProperty( "embeddable_strValue" ) );
		assertTrue( clazz.hasProperty( "embeddable_intValue" ) );
	}

	@Test
	@JiraKey(value = "HHH-9229")
	public void testEmptyEmbeddableWithPropertyAuditOverride(DomainModelScope dm, SessionFactoryScope sf) {
		final PersistentClass clazz = getPersistentClass(
				dm,
				sf,
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
		@Column(name = "val")
		private Integer value;

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}
	}

	@Entity(name = "aeoe")
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

	@Entity(name = "naee")
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

	@Entity(name = "foee")
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

	@Entity(name = "oee")
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

	@Entity(name = "sewpoe")
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

	@Entity(name = "see")
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


	@Entity(name = "sewoe")
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

	private PersistentClass getPersistentClass(
			DomainModelScope dm,
			SessionFactoryScope sf,
			Class<?> clazz,
			Object id,
			Number revision) {
		return sf.fromSession( session -> {
			final var auditReader = AuditReaderFactory.get( session );
			final Object entity = auditReader.find( clazz, id, revision );
			return dm.getDomainModel()
					.getEntityBinding( auditReader.getEntityName( id, revision, entity ) + "_AUD" );
		} );
	}
}
