/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DomainModel(
		annotatedClasses = {
				TestCollectionWithOrderInsertAndOrderUpdates.ChildEntity.class,
				TestCollectionWithOrderInsertAndOrderUpdates.ParentEntity.class,
				TestCollectionWithOrderInsertAndOrderUpdates.AnotherEntity.class,
				TestCollectionWithOrderInsertAndOrderUpdates.ThirdEntity.class,
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = BatchSettings.ORDER_INSERTS, value = "true"),
				@Setting(name = BatchSettings.ORDER_UPDATES, value = "true"),
		}
)
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@JiraKey("HHH-19585")
public class TestCollectionWithOrderInsertAndOrderUpdates {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ParentEntity parentEntity = new ParentEntity( 1L, "Parent 1", 0L );
					parentEntity.addChild( new ChildEntity( 1L, "Child 1" ) );
					session.persist( parentEntity );

					ParentEntity parentEntity2 = new ParentEntity( 2l, "Parent 2", 0L );
					parentEntity2.addChild( new ChildEntity( 2L, "Child 2" ) );
					session.persist( parentEntity2 );

					ParentEntity parentEntity3 = new ParentEntity( 3l, "Parent 3", 0L );
					session.persist( parentEntity3 );

					AnotherEntity anotherEntity = new AnotherEntity( 1L, "1", 1L, parentEntity );

					AnotherEntity anotherEntity2 = new AnotherEntity( 2L, "2", 2L, parentEntity2 );
					AnotherEntity anotherEntity3 = new AnotherEntity( 3L, "3", 3L, parentEntity3 );

					ThirdEntity thirdEntity = new ThirdEntity( 1L, "123" );
					thirdEntity.addAnotherEntity( anotherEntity );
					thirdEntity.addAnotherEntity( anotherEntity2 );
					thirdEntity.addAnotherEntity( anotherEntity3 );

					session.persist( thirdEntity );
				}
		);
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					ThirdEntity thirdEntity = session.getReference( ThirdEntity.class, 1L );
					for ( AnotherEntity thirdEntityDetail : thirdEntity.getAnotherEntities() ) {
						thirdEntityDetail.getParentEntity().getName();
					}
				}
		);

		scope.inTransaction(
				session -> {
					ParentEntity parentEntity = session.find( ParentEntity.class, 1L );
					assertThat( parentEntity.getChildren().size() ).isEqualTo( 1 );

					ParentEntity parentEntity2 = session.find( ParentEntity.class, 2L );
					assertThat( parentEntity2.getChildren().size() ).isEqualTo( 1 );

					ParentEntity parentEntity3 = session.find( ParentEntity.class, 3L );
					assertThat( parentEntity3.getChildren().size() ).isEqualTo( 0 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "ChildEntity")
	public static class ChildEntity {
		@Id
		private Long id;

		private String name;

		public ChildEntity() {
		}

		public ChildEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ParentEntity")
	public static class ParentEntity {
		@Id
		private Long id;

		private String name;

		@Column(name = "secondid")
		private Long secondId;

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@JoinColumns({
				@JoinColumn(name = "parentid", referencedColumnName = "id"),
				@JoinColumn(name = "secondid", referencedColumnName = "secondid")
		})
		private List<ChildEntity> children;

		public ParentEntity() {
		}

		public ParentEntity(Long id, String name, Long secondId) {
			this.id = id;
			this.name = name;
			this.secondId = secondId;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<ChildEntity> getChildren() {
			return children;
		}

		public void addChild(ChildEntity childEntity) {
			if ( this.children == null ) {
				this.children = new ArrayList<>();
			}
			this.children.add( childEntity );
		}
	}

	@Entity(name = "AnotherEntity")
	public static class AnotherEntity {
		@Id
		private Long id;

		private String name;

		@Column(name = "parentid")
		private Long parentId;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parentid", insertable = false, updatable = false, referencedColumnName = "id")
		private ParentEntity parentEntity;

		public AnotherEntity() {
		}

		public AnotherEntity(Long id, String name, Long parentId, ParentEntity parentEntity) {
			this.id = id;
			this.name = name;
			this.parentId = parentId;
			this.parentEntity = parentEntity;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Long getParentId() {
			return parentId;
		}

		public ParentEntity getParentEntity() {
			return parentEntity;
		}
	}

	@Entity(name = "ThirdEntity")
	public static class ThirdEntity {
		@Id
		private Long id;

		private String name;

		@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		@JoinColumn(name = "thirdEntityHeader", referencedColumnName = "id")
		private List<AnotherEntity> anotherEntities;

		public ThirdEntity() {
		}

		public ThirdEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<AnotherEntity> getAnotherEntities() {
			return anotherEntities;
		}

		public void addAnotherEntity(AnotherEntity anotherEntity) {
			if ( this.anotherEntities == null ) {
				this.anotherEntities = new ArrayList<>();
			}
			this.anotherEntities.add( anotherEntity );
		}
	}


}
