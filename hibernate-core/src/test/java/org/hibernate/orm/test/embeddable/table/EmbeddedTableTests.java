/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable.table;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.EmbeddedTable;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@RequiresDialect(value = H2Dialect.class, comment = "The underlying database has no effect on this, so just run on the default" )
public class EmbeddedTableTests {
	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {EmbeddedTableTests.Tag.class, EmbeddedTableTests.PostCompliant.class})
	void testCompliantApproach(DomainModelScope modelScope) {
		verifyModel( modelScope.getEntityBinding( PostCompliant.class ),
				"posts_compliant",
				"posts_compliant_secondary",
				modelScope.getDomainModel() );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {EmbeddedTableTests.Tag.class, EmbeddedTableTests.Post.class})
	void testTableNaming(DomainModelScope modelScope) {
		verifyModel( modelScope.getEntityBinding( Post.class ),
				"posts",
				"posts_secondary",
				modelScope.getDomainModel() );
	}

	void verifyModel(
			PersistentClass entityBinding,
			String primaryTableName,
			String secondaryTableName,
			MetadataImplementor domainModel) {
		final Property nameProperty = entityBinding.getProperty( "name" );
		assertThat( nameProperty.getValue().getTable().getName() ).isEqualTo( primaryTableName );

		final Property primaryTagProperty = entityBinding.getProperty( "tag" );
		assertThat( primaryTagProperty.getValue().getTable().getName() ).isEqualTo( secondaryTableName );

		final Namespace dbNamespace = domainModel.getDatabase().getDefaultNamespace();

		// id, name
		final org.hibernate.mapping.Table primaryTable = dbNamespace.locateTable(
				Identifier.toIdentifier( primaryTableName ) );
		assertThat( primaryTable.getColumns() ).hasSize( 2 );
		assertThat( primaryTable.getColumns().stream().map( org.hibernate.mapping.Column::getName ) )
				.containsExactlyInAnyOrder( "id", "name" );

		// text, added
		final org.hibernate.mapping.Table secondaryTable = dbNamespace.locateTable(
				Identifier.toIdentifier( secondaryTableName ) );
		assertThat( secondaryTable.getColumns() ).hasSize( 3 );
		assertThat( secondaryTable.getColumns().stream().map( org.hibernate.mapping.Column::getName ) )
				.containsExactlyInAnyOrder( "text", "added", "post_fk" );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {
			EmbeddedTableTests.Nested.class,
			EmbeddedTableTests.Container.class,
			EmbeddedTableTests.TopContainer.class
	})
	void testNestedModel(DomainModelScope modelScope) {
		final PersistentClass entityBinding = modelScope.getEntityBinding( TopContainer.class );

		final Property subContainerProp = entityBinding.getProperty( "subContainer" );
		checkContainerComponent( (Component) subContainerProp.getValue(), "supp" );

		final Property subContainersProp = entityBinding.getProperty( "subContainers" );
		final Collection containersPropValue = (Collection) subContainersProp.getValue();
		checkContainerComponent( (Component) containersPropValue.getElement(), "sub_containers" );

		final Namespace dbNamespace = modelScope.getDomainModel().getDatabase().getDefaultNamespace();

		// id, name
		final org.hibernate.mapping.Table primaryTable = dbNamespace.locateTable(
				Identifier.toIdentifier( "top" ) );
		assertThat( primaryTable.getColumns() ).hasSize( 2 );
		assertThat( primaryTable.getColumns().stream().map( org.hibernate.mapping.Column::getName ) )
				.containsExactlyInAnyOrder( "id", "name" );

		// thing1, thing2, top_fk
		final org.hibernate.mapping.Table secondaryTable = dbNamespace.locateTable(
				Identifier.toIdentifier( "supp" ) );
		assertThat( secondaryTable.getColumns() ).hasSize( 3 );
		assertThat( secondaryTable.getColumns().stream().map( org.hibernate.mapping.Column::getName ) )
				.containsExactlyInAnyOrder( "thing1", "thing2", "top_fk" );

		// thing1, thing2, top_fk
		final org.hibernate.mapping.Table collectionTable = dbNamespace.locateTable(
				Identifier.toIdentifier( "sub_containers" ) );
		assertThat( collectionTable.getColumns() ).hasSize( 3 );
		assertThat( collectionTable.getColumns().stream().map( org.hibernate.mapping.Column::getName ) )
				.containsExactlyInAnyOrder( "thing1", "thing2", "top_fk" );
	}

	private void checkContainerComponent(Component containerComponent, String tableName) {
		assertThat( containerComponent.getTable().getName() ).isEqualTo( tableName );
		assertThat( containerComponent.getPropertySpan() ).isEqualTo( 1 );
		final Property nestedProp = containerComponent.getProperty( "nested" );
		final Component nestedComponent = (Component) nestedProp.getValue();
		nestedComponent.getProperties().forEach( (subProp) -> {
			assertThat( subProp.getValue().getTable().getName() ).isEqualTo( tableName );
		} );
	}

	@Test
	@ServiceRegistry
	@DomainModel(annotatedClasses = {EmbeddedTableTests.Tag.class, EmbeddedTableTests.Post.class})
	@SessionFactory(useCollectingStatementInspector = true)
	void testDatabase(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		sqlCollector.clear();

		factoryScope.inTransaction( (session) -> {
			// NOTE: ... from posts p1_0 left join posts_secondary p1_1 ...
			session.createSelectionQuery( "from Post", Post.class ).list();
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) )
					.contains( "p1_0.id", "p1_0.name", "p1_1.added", "p1_1.text" );
		} );
	}

	@Test
	@ServiceRegistry
	void testBadNestedPlacement(ServiceRegistryScope registryScope) {
		final HibernatePersistenceConfiguration persistenceConfiguration = registryScope
				.createPersistenceConfiguration( "bad-nested" )
				.managedClasses( Bottom.class, BadMiddle.class, BadNesterEntity.class );
		try ( var sf = persistenceConfiguration.createEntityManagerFactory() ) {
			fail( "Should have failed with AnnotationPlacementException" );
		}
		catch (AnnotationPlacementException expected) {
		}
	}

	@Test
	@ServiceRegistry
	void testBadCollectionPlacement(ServiceRegistryScope registryScope) {
		final HibernatePersistenceConfiguration persistenceConfiguration = registryScope
				.createPersistenceConfiguration( "bad-nested" )
				.managedClasses( Bottom.class, Middle.class, BadCollectionEntity.class );
		try ( var sf = persistenceConfiguration.createEntityManagerFactory() ) {
			fail( "Should have failed with AnnotationPlacementException" );
		}
		catch (AnnotationPlacementException expected) {
		}
	}

	@Embeddable
	public static class Tag {
		String text;
		Instant added;
	}

	@Entity(name="Post")
	@Table(name="posts")
	@SecondaryTable(name="posts_secondary", pkJoinColumns = @PrimaryKeyJoinColumn(name = "post_fk"))
	public static class Post {
		@Id
		private Integer id;
		private String name;
		@Embedded
		@EmbeddedTable("posts_secondary")
		private Tag tag;
	}

	@Entity(name="PostCompliant")
	@Table(name="posts_compliant")
	@SecondaryTable(name="posts_compliant_secondary", pkJoinColumns = @PrimaryKeyJoinColumn(name = "post_fk"))
	public static class PostCompliant {
		@Id
		private Integer id;
		private String name;
		@Embedded
		@AttributeOverride(name="text", column = @Column(table = "posts_compliant_secondary") )
		@AttributeOverride(name="added", column = @Column(table = "posts_compliant_secondary") )
		private Tag tag;
	}

	@Embeddable
	public static class Nested {
		String thing1;
		String thing2;
	}

	@Embeddable
	public static class Container {
		@Embedded
		Nested nested;
	}

	@Entity(name="TopContainer")
	@Table(name="top")
	@SecondaryTable(name="supp", pkJoinColumns = @PrimaryKeyJoinColumn(name = "top_fk"))
	public static class TopContainer {
		@Id
		private Integer id;
		private String name;
		@Embedded
		@EmbeddedTable("supp")
		private Container subContainer;

		@ElementCollection
		@CollectionTable(name = "sub_containers", joinColumns = @JoinColumn(name = "top_fk"))
		private Set<Container> subContainers;
	}

	@Embeddable
	public static class Bottom {
		private String kind;
		private Instant whenReached;
	}

	@Embeddable
	public static class BadMiddle {
		@Embedded
		@EmbeddedTable("secondary")
		private Bottom bottom;
	}

	@Embeddable
	public static class Middle {
		@Embedded
		private Bottom bottom;
	}

	@Entity(name="BadNesterEntity")
	@Table(name="primary")
	@SecondaryTable(name="secondary", pkJoinColumns = @PrimaryKeyJoinColumn(name = "primary_fk"))
	public static class BadNesterEntity {
		@Id
		private Integer id;
		private String name;
		@Embedded
		@EmbeddedTable("secondary")
		BadMiddle badMiddle;
	}

	@Entity(name="BadNesterEntity")
	@Table(name="primary")
	@SecondaryTable(name="secondary", pkJoinColumns = @PrimaryKeyJoinColumn(name = "primary_fk"))
	public static class BadCollectionEntity {
		@Id
		private Integer id;
		private String name;
		@ElementCollection
		@EmbeddedTable("secondary")
		Set<Middle> middles;
	}
}
