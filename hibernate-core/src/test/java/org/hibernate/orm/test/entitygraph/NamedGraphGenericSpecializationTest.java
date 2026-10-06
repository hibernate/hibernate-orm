package org.hibernate.orm.test.entitygraph;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Named subgraphs must resolve an inherited generic association independently
/// for each concrete entity specializing the same mapped superclass.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = { NamedGraphGenericSpecializationTest.CategoryPosition.class,
		NamedGraphGenericSpecializationTest.AttributePosition.class,
		NamedGraphGenericSpecializationTest.Category.class, NamedGraphGenericSpecializationTest.Attribute.class } )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-13320" )
class NamedGraphGenericSpecializationTest {
	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Category category = new Category();
			category.id = 1L;
			category.featured = true;
			Attribute attribute = new Attribute();
			attribute.id = 2L;
			attribute.tags.add( "tag" );
			session.persist( category );
			session.persist( attribute );
			CategoryPosition categoryPosition = new CategoryPosition();
			categoryPosition.id = category.id;
			categoryPosition.entity = category;
			AttributePosition attributePosition = new AttributePosition();
			attributePosition.id = attribute.id;
			attributePosition.entity = attribute;
			session.persist( categoryPosition );
			session.persist( attributePosition );
		} );
	}

	@Test
	void categorySubgraphUsesCategoryType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var graph = session.getEntityGraph( CategoryPosition.class, "category.position" );
			var subgraphs = graph.getAttributeNode( "entity" ).getSubgraphs();
			assertThat( subgraphs ).containsOnlyKeys( Category.class );
			assertThat( subgraphs.get( Category.class ).getAttributeNodes() )
					.extracting( node -> node.getAttributeName() ).containsExactly( "featured" );

			var position = session.find( graph, 1L );
			assertThat( Hibernate.isInitialized( position.entity ) ).isTrue();
			assertThat( position.entity.featured ).isTrue();
		} );
	}

	@Test
	void attributeSubgraphUsesAttributeType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var graph = session.getEntityGraph( AttributePosition.class, "attribute.position" );
			var subgraphs = graph.getAttributeNode( "entity" ).getSubgraphs();
			assertThat( subgraphs ).containsOnlyKeys( Attribute.class );
			assertThat( subgraphs.get( Attribute.class ).getAttributeNodes() )
					.extracting( node -> node.getAttributeName() ).containsExactly( "tags" );

			var position = session.find( graph, 2L );
			assertThat( Hibernate.isInitialized( position.entity ) ).isTrue();
			assertThat( Hibernate.isInitialized( position.entity.tags ) ).isTrue();
			assertThat( position.entity.tags ).containsExactly( "tag" );
		} );
	}

	@MappedSuperclass
	public static class Position<T> {
		@Id
		Long id;
		@OneToOne( optional = false, fetch = FetchType.LAZY )
		@PrimaryKeyJoinColumn
		T entity;
	}

	@Entity( name = "GenericCategoryPosition" )
	@NamedEntityGraph( name = "category.position", includeAllAttributes = true,
			attributeNodes = @NamedAttributeNode( value = "entity", subgraph = "category.details" ),
			subgraphs = @NamedSubgraph( name = "category.details", attributeNodes = @NamedAttributeNode( "featured" ) ) )
	public static class CategoryPosition extends Position<Category> {
	}

	@Entity( name = "GenericAttributePosition" )
	@NamedEntityGraph( name = "attribute.position",
			attributeNodes = @NamedAttributeNode( value = "entity", subgraph = "attribute.tags" ),
			subgraphs = @NamedSubgraph( name = "attribute.tags", attributeNodes = @NamedAttributeNode( "tags" ) ) )
	public static class AttributePosition extends Position<Attribute> {
	}

	@Entity( name = "GraphCategory" )
	public static class Category {
		@Id
		Long id;
		boolean featured;
	}

	@Entity( name = "GraphAttribute" )
	public static class Attribute {
		@Id
		Long id;
		@ElementCollection
		List<String> tags = new ArrayList<>();
	}
}
