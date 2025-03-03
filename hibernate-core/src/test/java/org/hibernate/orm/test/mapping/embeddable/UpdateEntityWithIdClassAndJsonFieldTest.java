/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertTrue;

@DomainModel(
		annotatedClasses = {
				UpdateEntityWithIdClassAndJsonFieldTest.MyEntity.class
		}
)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJsonAggregate.class)
@JiraKey("HHH-17159")
public class UpdateEntityWithIdClassAndJsonFieldTest {
	private static final String NAME = "Name";
	private static final Long PLANT_ID = 1l;
	private static final Long ID_2 = 2l;
	private static final String DESCRIPTION = "first description";
	private static final String UPDATED_DESCRIPTION = "updated";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity entity = new MyEntity( NAME, PLANT_ID, new Description( DESCRIPTION ), ID_2 );
					session.persist( entity );
				}
		);
	}

	@Test
	public void updateTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int updatedRows = session.createQuery(
									"UPDATE MyEntity u SET u.hasPictures = :hasPicture WHERE u.id2 = :entityId" )
							.setParameter( "hasPicture", Boolean.TRUE )
							.setParameter( "entityId", ID_2 )
							.executeUpdate();
					assertTrue( updatedRows > 0 );
				}
		);

		scope.inTransaction(
				session -> {
					MyEntity myEntity = session.createQuery( "from MyEntity m where m.id2 = :id2", MyEntity.class )
							.setParameter( "id2", ID_2 )
							.uniqueResult();
					assertThat( myEntity.getHasPictures() ).isTrue();
					assertThat( myEntity.getDescription().getText() ).isEqualTo( DESCRIPTION );
				}
		);

		scope.inTransaction(
				session -> {
					int updatedRows = session.createQuery(
									"UPDATE MyEntity u SET u.description.text = :description WHERE u.id2 = :entityId" )
							.setParameter( "description",  UPDATED_DESCRIPTION  )
							.setParameter( "entityId", ID_2 )
							.executeUpdate();
					assertTrue( updatedRows > 0 );
				}
		);

		scope.inTransaction(
				session -> {
					MyEntity myEntity = session.createQuery( "from MyEntity m where m.id2 = :id2", MyEntity.class )
							.setParameter( "id2", ID_2 )
							.uniqueResult();
					assertThat( myEntity.getHasPictures() ).isTrue();
					assertThat( myEntity.getDescription().getText() ).isEqualTo( UPDATED_DESCRIPTION );
				}
		);
	}

	public static class MyEntityPK implements Serializable {

		private String name;
		private Long plantId;

		public MyEntityPK() {
		}

		public String getName() {
			return name;
		}

		public Long getPlantId() {
			return plantId;
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 97 * hash + Objects.hashCode( this.name );
			hash = 97 * hash + Objects.hashCode( this.plantId );
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			final MyEntityPK other = (MyEntityPK) obj;
			if ( !Objects.equals( this.name, other.getName() ) ) {
				return false;
			}
			return Objects.equals( this.plantId, other.getPlantId() );
		}
	}

	@IdClass(MyEntityPK.class)
	@Entity(name = "MyEntity")
	public static class MyEntity {

		@Id
		private String name;

		@Id
		private Long plantId;

		@JdbcTypeCode(SqlTypes.JSON)
		private Description description;

		private Long id2;

		private Boolean hasPictures = Boolean.FALSE;

		public MyEntity() {
		}

		public MyEntity(String name, Long plantId, Description description, Long id2) {
			this.name = name;
			this.plantId = plantId;
			this.description = description;
			this.id2 = id2;
		}

		public String getName() {
			return name;
		}

		public MyEntity setName(String name) {
			this.name = name;
			return this;
		}

		public Long getPlantId() {
			return plantId;
		}


		public Long getId2() {
			return id2;
		}


		public Boolean getHasPictures() {
			return hasPictures;
		}


		public Description getDescription() {
			return description;
		}
	}

	@Embeddable
	public static class Description {

		@Column(name = "TEXT_COLUMN")
		private String text;

		public Description() {
		}

		public Description(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}
}
