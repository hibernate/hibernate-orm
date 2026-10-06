package org.hibernate.orm.test.mapping.access;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// An identifier access override must not change the access inherited by an
/// embeddable element collection, whose fields have no accessor methods.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = { ElementCollectionMixedAccessTest.Owner.class,
		ElementCollectionMixedAccessTest.Detail.class } )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-13537" )
class ElementCollectionMixedAccessTest {
	@Test
	@FailureExpected( jiraKey = "HHH-13537" )
	void collectionElementsInheritFieldAccess(SessionFactoryScope scope) {
		var metamodel = scope.getSessionFactory().getJpaMetamodel();
		assertThat( metamodel.entity( Owner.class ).getId( Long.class ).getJavaMember() )
				.isInstanceOf( Method.class );
		assertThat( metamodel.entity( Owner.class ).getAttribute( "details" ).getJavaMember() )
				.isInstanceOf( Field.class );
		assertThat( metamodel.entity( Owner.class ).getAttributes() ).extracting( attribute -> attribute.getName() )
				.containsExactlyInAnyOrder( "id", "details" );
		assertThat( metamodel.embeddable( Detail.class ).getAttribute( "description" ).getJavaMember() )
				.isInstanceOf( Field.class );

		Long id = scope.fromTransaction( session -> {
			Owner owner = new Owner();
			Detail detail = new Detail();
			detail.description = "description without an accessor";
			owner.details.add( detail );
			session.persist( owner );
			return owner.id;
		} );
		scope.inTransaction( session -> {
			Owner owner = session.find( Owner.class, id );
			assertThat( owner.details ).extracting( detail -> detail.description )
					.containsExactly( "description without an accessor" );
		} );
	}

	@Entity( name = "MixedAccessOwner" )
	public static class Owner {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		@Access( AccessType.PROPERTY )
		private Long id;

		@ElementCollection
		private Set<Detail> details = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTest() {
			throw new AssertionError( "This method is not a persistent property" );
		}
	}

	@Embeddable
	public static class Detail {
		private String description;
	}
}
