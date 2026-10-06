package org.hibernate.orm.test.mapping.access;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Embeddable collection elements inherit entity access inferred from identifier
/// annotation placement, without any explicit access annotations.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = { ElementCollectionInferredAccessTest.FieldOwner.class,
		ElementCollectionInferredAccessTest.FieldDetail.class, ElementCollectionInferredAccessTest.PropertyOwner.class,
		ElementCollectionInferredAccessTest.PropertyDetail.class } )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-13537" )
class ElementCollectionInferredAccessTest {
	@Test
	void identifierOnFieldDeterminesAccess(SessionFactoryScope scope) {
		var metamodel = scope.getSessionFactory().getJpaMetamodel();
		assertThat( metamodel.entity( FieldOwner.class ).getId( Long.class ).getJavaMember() )
				.isInstanceOf( Field.class );
		assertThat( metamodel.entity( FieldOwner.class ).getAttribute( "name" ).getJavaMember() )
				.isInstanceOf( Field.class );
		assertThat( metamodel.entity( FieldOwner.class ).getAttribute( "details" ).getJavaMember() )
				.isInstanceOf( Field.class );
		assertThat( metamodel.embeddable( FieldDetail.class ).getAttribute( "description" ).getJavaMember() )
				.isInstanceOf( Field.class );
		assertThat( metamodel.entity( FieldOwner.class ).getAttributes() ).extracting( attribute -> attribute.getName() )
				.containsExactlyInAnyOrder( "id", "name", "details" );

		Long id = scope.fromTransaction( session -> {
			FieldOwner owner = new FieldOwner();
			owner.name = "field owner";
			FieldDetail detail = new FieldDetail();
			detail.description = "field access";
			owner.details.add( detail );
			session.persist( owner );
			return owner.id;
		} );
		scope.inTransaction( session -> {
			var owner = session.find( FieldOwner.class, id );
			assertThat( owner.name ).isEqualTo( "field owner" );
			assertThat( owner.details ).extracting( detail -> detail.description ).containsExactly( "field access" );
		} );
	}

	@Test
	void identifierOnGetterDeterminesAccess(SessionFactoryScope scope) {
		var metamodel = scope.getSessionFactory().getJpaMetamodel();
		assertThat( metamodel.entity( PropertyOwner.class ).getId( Long.class ).getJavaMember() )
				.isInstanceOf( Method.class );
		assertThat( metamodel.entity( PropertyOwner.class ).getAttribute( "name" ).getJavaMember() )
				.isInstanceOf( Method.class );
		assertThat( metamodel.entity( PropertyOwner.class ).getAttribute( "details" ).getJavaMember() )
				.isInstanceOf( Method.class );
		assertThat( metamodel.embeddable( PropertyDetail.class ).getAttribute( "description" ).getJavaMember() )
				.isInstanceOf( Method.class );

		Long id = scope.fromTransaction( session -> {
			PropertyOwner owner = new PropertyOwner();
			owner.setName( "property owner" );
			PropertyDetail detail = new PropertyDetail();
			detail.setDescription( "property access" );
			owner.getDetails().add( detail );
			session.persist( owner );
			return owner.getId();
		} );
		scope.inTransaction( session -> {
			var owner = session.find( PropertyOwner.class, id );
			assertThat( owner.getName() ).isEqualTo( "property owner" );
			assertThat( owner.getDetails() ).extracting( PropertyDetail::getDescription )
					.containsExactly( "property access" );
		} );
	}

	@Entity( name = "InferredFieldAccessOwner" )
	public static class FieldOwner {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;
		private String name;
		@ElementCollection
		private Set<FieldDetail> details = new HashSet<>();

		public String getTest() {
			throw new AssertionError( "This method is not a persistent property" );
		}
	}

	@Embeddable
	public static class FieldDetail {
		private String description;
	}

	@Entity( name = "InferredPropertyAccessOwner" )
	public static class PropertyOwner {
		private Long id;
		private String name;
		private Set<PropertyDetail> details = new HashSet<>();

		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
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

		@ElementCollection
		public Set<PropertyDetail> getDetails() {
			return details;
		}

		public void setDetails(Set<PropertyDetail> details) {
			this.details = details;
		}
	}

	@Embeddable
	public static class PropertyDetail {
		private String description;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
