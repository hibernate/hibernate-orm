/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.embedded;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddableWithManyToOneCircularityTest.EntityTest.class,
				EmbeddableWithManyToOneCircularityTest.EntityTest2.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class EmbeddableWithManyToOneCircularityTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 1 );
					EntityTest2 entityTest2 = new EntityTest2( 2 );

					EmbeddableTest embeddable = new EmbeddableTest();
					embeddable.setEntity( entity );
					embeddable.setStringField( "Fab" );

					entityTest2.setEmbeddedAttribute( embeddable );

					entity.setEntity2( entityTest2 );
					session.save( entity );
					session.save( entityTest2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityTest", EntityTest.class ).getResultList().forEach( entity -> {
						final EntityTest2 entity2 = entity.getEntity2();
						if ( entity2 != null && entity2.getEmbeddedAttribute() != null ) {
							entity2.getEmbeddedAttribute().setEntity( null );
						}
						session.remove( entity );
					} );
					session.flush();
					session.createMutationQuery( "delete from EntityTest2" ).executeUpdate();
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityTest entity = session.get( EntityTest.class, 1 );

					EntityTest2 entity2 = entity.getEntity2();
					assertSame( entity, entity2.getEmbeddedAttribute().getEntity() );

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
				}
		);
	}

	@Entity(name = "EntityTest")
	public static class EntityTest {
		@Id
		private Integer id;

		@ManyToOne
		private EntityTest2 entity2;

		public EntityTest() {
		}

		public EntityTest(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityTest2 getEntity2() {
			return entity2;
		}

		public void setEntity2(EntityTest2 entity2) {
			this.entity2 = entity2;
		}
	}

	@Entity(name = "EntityTest2")
	public static class EntityTest2 {
		@Id
		private Integer id;

		@Embedded
		private EmbeddableTest embeddedAttribute;

		public EntityTest2() {
		}

		public EntityTest2(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EmbeddableTest getEmbeddedAttribute() {
			return embeddedAttribute;
		}

		public void setEmbeddedAttribute(EmbeddableTest embeddedAttribute) {
			this.embeddedAttribute = embeddedAttribute;
		}
	}

	@Embeddable
	public static class EmbeddableTest {
		private String stringField;

		@OneToOne(mappedBy = "entity2")
		private EntityTest entity;

		public String getStringField() {
			return stringField;
		}

		public void setStringField(String stringField) {
			this.stringField = stringField;
		}

		public EntityTest getEntity() {
			return entity;
		}

		public void setEntity(EntityTest entity) {
			this.entity = entity;
		}
	}
}
