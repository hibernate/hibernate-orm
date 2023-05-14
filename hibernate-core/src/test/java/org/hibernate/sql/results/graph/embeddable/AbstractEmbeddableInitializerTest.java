/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Storey
 */
@DomainModel(annotatedClasses = { AbstractEmbeddableInitializerTest.DomainEntity.class, })
@SessionFactory
public class AbstractEmbeddableInitializerTest {

	@Test
	@TestForIssue(jiraKey = "HHH-16560")
	public void testCompositeEmbeddedId(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			DomainEntity entity = new DomainEntity( new SimpleEmbeddedId(1L), "key" );
			session.persist( entity );
			session.flush();
			Object loaded = session.createQuery( "select id from DomainEntity" ).getSingleResult();
			assertEquals(loaded, entity.getId());
		});
	}

	@Entity(name = "DomainEntity")
	@Table(name = "domain_entity")
	public static class DomainEntity {
		@EmbeddedId
		private CompositeEmbeddedId id;
		private String name;

		private DomainEntity() {
		}

		public DomainEntity(SimpleEmbeddedId simpleId, String name) {
			this();
			this.id = new CompositeEmbeddedId( simpleId );
			this.name = name;
		}

		public CompositeEmbeddedId getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	public static class CompositeEmbeddedId {
		private SimpleEmbeddedId simpleId;
		private String idType;

		private CompositeEmbeddedId() {
		}

		public CompositeEmbeddedId(SimpleEmbeddedId simpleId) {
			this();
			this.simpleId = simpleId;
			this.idType = simpleId.getClass().getSimpleName();
		}

		public String getIdType() {
			return idType;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !( obj instanceof CompositeEmbeddedId ) ) {
				return false;
			}
			CompositeEmbeddedId other = ( CompositeEmbeddedId ) obj;
			return other.simpleId.equals( simpleId ) && other.idType.equals( idType );
		}
	}

	@Embeddable
	public static class SimpleEmbeddedId {
		private Long id;

		protected SimpleEmbeddedId() {
		}

		protected SimpleEmbeddedId(Long id) {
			this();
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		@Override
		public boolean equals(Object obj) {
			if ( !( obj instanceof SimpleEmbeddedId ) ) {
				return false;
			}
			SimpleEmbeddedId other = ( SimpleEmbeddedId ) obj;
			return other.id.equals( id );
		}
	}
}