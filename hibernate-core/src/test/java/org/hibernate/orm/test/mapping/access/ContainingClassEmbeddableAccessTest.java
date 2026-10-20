package org.hibernate.orm.test.mapping.access;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Access to an embedding attribute is independent of access to embedded state,
/// including collection elements and map keys in element and relationship maps.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = {
		ContainingClassEmbeddableAccessTest.FieldOwner.class,
		ContainingClassEmbeddableAccessTest.PropertyOwner.class,
		ContainingClassEmbeddableAccessTest.Target.class,
		ContainingClassEmbeddableAccessTest.ExplicitOwner.class,
		ContainingClassEmbeddableAccessTest.InferredOwner.class,
		ContainingClassEmbeddableAccessTest.IdOwner.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-21017" )
@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Because I'm tired of f'n around with Spanner's table naming rules")
@SkipForDialect(dialectClass = SpannerPostgreSQLDialect.class, reason = "Because I'm tired of f'n around with Spanner's table naming rules")
class ContainingClassEmbeddableAccessTest {
	@Test
	void fieldContainer(DomainModelScope model, SessionFactoryScope scope) {
		verifyModel( model, FieldOwner.class, "property", "field", "property" );
		scope.inTransaction( session -> {
			var owner = new FieldOwner();
			owner.id = 1L;
			owner.detail = new FieldDetail( "singular" );
			owner.details.add( new FieldDetail( "element" ) );
			owner.entries.put( new FieldDetail( "key" ), "value" );
			owner.targets.put( new FieldDetail( "relationship key" ), new Target( 1L ) );
			session.persist( owner );
		} );
		scope.inTransaction( session -> {
			var owner = session.find( FieldOwner.class, 1L );
			verifyDetail( owner.detail.description, owner.detail.nested.description, owner.detail.tag, "singular" );
			assertThat( owner.details ).hasSize( 1 );
			var element = owner.details.get( 0 );
			verifyDetail( element.description, element.nested.description, element.tag, "element" );
			assertThat( owner.entries.get( new FieldDetail( "key" ) ) ).isEqualTo( "value" );
			assertThat( owner.targets.get( new FieldDetail( "relationship key" ) ).id ).isEqualTo( 1L );
		} );
	}

	@Test
	void propertyContainer(DomainModelScope model, SessionFactoryScope scope) {
		verifyModel( model, PropertyOwner.class, "field", "property", "field" );
		scope.inTransaction( session -> {
			var owner = new PropertyOwner();
			owner.id = 2L;
			owner.detail = new PropertyDetail( "singular" );
			owner.details.add( new PropertyDetail( "element" ) );
			owner.entries.put( new PropertyDetail( "key" ), "value" );
			owner.targets.put( new PropertyDetail( "relationship key" ), new Target( 2L ) );
			session.persist( owner );
		} );
		scope.inTransaction( session -> {
			var owner = session.find( PropertyOwner.class, 2L );
			verifyDetail( owner.detail.description, owner.detail.nested.description, owner.detail.tag, "singular" );
			assertThat( owner.details ).hasSize( 1 );
			var element = owner.details.get( 0 );
			verifyDetail( element.description, element.nested.description, element.tag, "element" );
			assertThat( owner.entries.get( new PropertyDetail( "key" ) ) ).isEqualTo( "value" );
			assertThat( owner.targets.get( new PropertyDetail( "relationship key" ) ).id ).isEqualTo( 2L );
		} );
	}

	@Test
	void explicitEmbeddableAccessAndNestedOverride(DomainModelScope scope) {
		var property = scope.getDomainModel().getEntityBinding( ExplicitOwner.class.getName() ).getProperty( "detail" );
		assertThat( property.getPropertyAccessorName() ).isEqualTo( "property" );
		var component = (Component) property.getValue();
		// The embeddable explicitly chooses PROPERTY despite its FIELD container.
		assertThat( component.getProperty( "description" ).getPropertyAccessorName() ).isEqualTo( "property" );
		var nested = component.getProperty( "nested" );
		assertThat( nested.getPropertyAccessorName() ).isEqualTo( "field" );
		// Its FIELD embedding attribute must not override the containing embeddable's PROPERTY access.
		assertThat( ((Component) nested.getValue()).getProperty( "description" ).getPropertyAccessorName() )
				.isEqualTo( "property" );
	}

	@Test
	void inferredContainingClassAccess(DomainModelScope scope) {
		var property = scope.getDomainModel().getEntityBinding( InferredOwner.class.getName() ).getProperty( "detail" );
		assertThat( property.getPropertyAccessorName() ).isEqualTo( "property" );
		verifyComponent( property, "field", "property" );
	}

	@Test
	void embeddedIdentifier(DomainModelScope scope) {
		var entity = scope.getDomainModel().getEntityBinding( IdOwner.class.getName() );
		assertThat( entity.getIdentifierProperty().getPropertyAccessorName() ).isEqualTo( "property" );
		var key = (Component) entity.getIdentifier();
		assertThat( key.getPropertySpan() ).isEqualTo( 2 );
		assertThat( key.getProperties() ).allSatisfy( property ->
				assertThat( property.getPropertyAccessorName() ).isEqualTo( "field" ) );
	}

	private void verifyModel(DomainModelScope scope, Class<?> type, String attributeAccess, String stateAccess, String tagAccess) {
		var entity = scope.getDomainModel().getEntityBinding( type.getName() );
		for ( var name : List.of( "detail", "details", "entries", "targets" ) ) {
			var property = entity.getProperty( name );
			assertThat( property.getPropertyAccessorName() ).isEqualTo( attributeAccess );
			if ( name.equals( "detail" ) ) {
				verifyComponent( property, stateAccess, tagAccess );
			}
			else {
				var collection = (Collection) property.getValue();
				var component = (Component) (name.equals( "details" )
						? collection.getElement() : ((org.hibernate.mapping.Map) collection).getIndex());
				verifyComponent( component, stateAccess, tagAccess );
			}
		}
	}

	private void verifyComponent(Property property, String stateAccess, String tagAccess) {
		verifyComponent( (Component) property.getValue(), stateAccess, tagAccess );
	}

	private void verifyComponent(Component component, String stateAccess, String tagAccess) {
		assertThat( component.getProperty( "description" ).getPropertyAccessorName() ).isEqualTo( stateAccess );
		assertThat( component.getProperty( "tag" ).getPropertyAccessorName() ).isEqualTo( tagAccess );
		var nested = component.getProperty( "nested" );
		assertThat( nested.getPropertyAccessorName() ).isEqualTo( stateAccess );
		assertThat( ((Component) nested.getValue()).getProperty( "description" ).getPropertyAccessorName() )
				.isEqualTo( stateAccess );
	}

	private void verifyDetail(String description, String nested, String tag, String expected) {
		assertThat( description ).isEqualTo( expected );
		assertThat( nested ).isEqualTo( expected + " nested" );
		assertThat( tag ).isEqualTo( expected + " tag" );
	}

	@MappedSuperclass
	@Access( AccessType.FIELD )
	static class FieldBase {
		@Id
		long id;
		@Transient
		FieldDetail detail = null;
		@Transient
		List<FieldDetail> details = new ArrayList<>();
		@Transient
		Map<FieldDetail, String> entries = new LinkedHashMap<>();
		@Transient
		Map<FieldDetail, Target> targets = new LinkedHashMap<>();

		@Embedded
		@Access( AccessType.PROPERTY )
		public FieldDetail getDetail() {
			return detail;
		}

		public void setDetail(FieldDetail detail) {
			this.detail = detail;
		}

		@ElementCollection
		@Access( AccessType.PROPERTY )
		public List<FieldDetail> getDetails() {
			return details;
		}

		public void setDetails(List<FieldDetail> details) {
			this.details = details;
		}

		@ElementCollection
		@Access( AccessType.PROPERTY )
		public Map<FieldDetail, String> getEntries() {
			return entries;
		}

		public void setEntries(Map<FieldDetail, String> entries) {
			this.entries = entries;
		}

		@OneToMany( cascade = CascadeType.ALL )
		@Access( AccessType.PROPERTY )
		public Map<FieldDetail, Target> getTargets() {
			return targets;
		}

		public void setTargets(Map<FieldDetail, Target> targets) {
			this.targets = targets;
		}
	}

	@Entity
	@Table(name = "field_owner")
	@Access( AccessType.PROPERTY )
	static class FieldOwner extends FieldBase {}

	@Entity
	@Table(name = "property_owner")
	@Access( AccessType.PROPERTY )
	static class PropertyOwner {
		long id;
		@Embedded
		@Access( AccessType.FIELD )
		PropertyDetail detail = null;
		@ElementCollection
		@Access( AccessType.FIELD )
		List<PropertyDetail> details = new ArrayList<>();
		@ElementCollection
		@Access( AccessType.FIELD )
		Map<PropertyDetail, String> entries = new LinkedHashMap<>();
		@OneToMany( cascade = CascadeType.ALL )
		@Access( AccessType.FIELD )
		Map<PropertyDetail, Target> targets = new LinkedHashMap<>();

		@Id
		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	@Table(name = "target")
	static class Target {
		@Id
		long id;

		Target() {}

		Target(long id) {
			this.id = id;
		}
	}

	@Embeddable
	static class FieldDetail {
		String description;
		@Embedded
		@AttributeOverride( name = "description", column = @Column( name = "nested_description" ) )
		FieldNested nested;
		@Transient
		String tag;

		FieldDetail() {}

		FieldDetail(String description) {
			this.description = description;
			this.nested = new FieldNested();
			this.nested.description = description + " nested";
			this.tag = description + " tag";
		}

		@Access( AccessType.PROPERTY )
		public String getTag() {
			return tag;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof FieldDetail that
					&& Objects.equals( description, that.description )
					&& Objects.equals( nested.description, that.nested.description )
					&& Objects.equals( tag, that.tag );
		}

		@Override
		public int hashCode() {
			return Objects.hash( description, nested.description, tag );
		}
	}

	@Embeddable
	static class FieldNested {
		String description;
	}

	@Embeddable
	static class PropertyDetail {
		String description;
		PropertyNested nested;
		@Access( AccessType.FIELD )
		String tag;

		PropertyDetail() {}

		PropertyDetail(String description) {
			this.description = description;
			this.nested = new PropertyNested();
			this.nested.description = description + " nested";
			this.tag = description + " tag";
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@Embedded
		@AttributeOverride( name = "description", column = @Column( name = "nested_description" ) )
		public PropertyNested getNested() {
			return nested;
		}

		public void setNested(PropertyNested nested) {
			this.nested = nested;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof PropertyDetail that
					&& Objects.equals( description, that.description )
					&& Objects.equals( nested.description, that.nested.description )
					&& Objects.equals( tag, that.tag );
		}

		@Override
		public int hashCode() {
			return Objects.hash( description, nested.description, tag );
		}
	}

	@Embeddable
	static class PropertyNested {
		String description;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	@Entity
	@Access( AccessType.FIELD )
	static class ExplicitOwner {
		@Id
		long id;
		@Transient
		ExplicitDetail detail;

		@Embedded
		@Access( AccessType.PROPERTY )
		public ExplicitDetail getDetail() {
			return detail;
		}

		public void setDetail(ExplicitDetail detail) {
			this.detail = detail;
		}
	}

	@Embeddable
	@Access( AccessType.PROPERTY )
	static class ExplicitDetail {
		String description;
		@Embedded
		@Access( AccessType.FIELD )
		@AttributeOverride( name = "description", column = @Column( name = "explicit_nested_description" ) )
		PropertyNested nested;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	@Entity
	static class InferredOwner {
		@Id
		long id;
		@Transient
		FieldDetail detail;

		@Embedded
		@Access( AccessType.PROPERTY )
		public FieldDetail getDetail() {
			return detail;
		}

		public void setDetail(FieldDetail detail) {
			this.detail = detail;
		}
	}
	@Entity
	@Access( AccessType.FIELD )
	static class IdOwner {
		@Transient
		FieldKey id;

		@EmbeddedId
		@Access( AccessType.PROPERTY )
		public FieldKey getId() {
			return id;
		}

		public void setId(FieldKey id) {
			this.id = id;
		}
	}

	@Embeddable
	static class FieldKey {
		String first;
		String second;

		@Override
		public boolean equals(Object other) {
			return other instanceof FieldKey that
					&& Objects.equals( first, that.first ) && Objects.equals( second, that.second );
		}

		@Override
		public int hashCode() {
			return Objects.hash( first, second );
		}
	}

}
