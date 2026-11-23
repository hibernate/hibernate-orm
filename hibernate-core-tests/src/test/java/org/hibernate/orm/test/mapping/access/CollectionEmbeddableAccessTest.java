package org.hibernate.orm.test.mapping.access;

import java.util.List;
import java.util.Map;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import org.hibernate.boot.MetadataSources;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Collection contents inherit declaring-class access independently of identifier
/// overrides and collection attribute overrides.
///
/// @author Steve Ebersole
@ServiceRegistry
@Jira( "https://hibernate.atlassian.net/browse/HHH-13537" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-21017" )
class CollectionEmbeddableAccessTest {
	@Test
	void propertyAccessWithFieldIdentifier(ServiceRegistryScope scope) {
		verify( scope, PropertyOwner.class, "details", "property", false );
	}

	@Test
	void collectionOverride(ServiceRegistryScope scope) {
		verify( scope, OverrideOwner.class, "details", "field", false );
	}

	@Test
	void fieldCollectionOverride(ServiceRegistryScope scope) {
		verify( scope, FieldOverrideOwner.class, "details", "property", false );
	}

	@Test
	void explicitEmbeddableAccess(ServiceRegistryScope scope) {
		verify( scope, ExplicitOwner.class, "details", "field", false );
	}

	@Test
	void mappedSuperclass(ServiceRegistryScope scope) {
		verify( scope, SubclassOwner.class, "details", "property", false );
	}

	@Test
	void containingEmbeddable(ServiceRegistryScope scope) {
		verify( scope, EmbeddedOwner.class, "container.details", "property", false );
	}

	@Test
	void mapKeyAndNestedComponent(ServiceRegistryScope scope) {
		verify( scope, MapOwner.class, "details", "field", true );
	}

	@Test
	void mapKeyCollectionOverride(ServiceRegistryScope scope) {
		verify( scope, MapOverrideOwner.class, "details", "field", true );
	}

	private void verify(ServiceRegistryScope scope, Class<?> owner, String path, String access, boolean mapKey) {
		var metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( owner )
				.buildMetadata();
		var collection = (Collection) metadata.getEntityBinding( owner.getName() ).getRecursiveProperty( path ).getValue();
		var component = (Component) ( mapKey ? ( (org.hibernate.mapping.Map) collection ).getIndex() : collection.getElement() );
		assertThat( component.getPropertySpan() ).isEqualTo( 1 );
		var nested = component.getProperty( "nested" );
		assertThat( nested.getPropertyAccessorName() ).isEqualTo( access );
		var nestedComponent = (Component) nested.getValue();
		assertThat( nestedComponent.getPropertySpan() ).isEqualTo( 1 );
		assertThat( nestedComponent.getProperty( "description" ).getPropertyAccessorName() ).isEqualTo( access );
	}

	@Entity
	@Access( AccessType.PROPERTY )
	static class PropertyOwner {
		@Id
		@Access( AccessType.FIELD )
		long id;
		List<PropertyDetail> details;

		@ElementCollection
		public List<PropertyDetail> getDetails() {
			return details;
		}
		public void setDetails(List<PropertyDetail> details) {
			this.details = details;
		}
	}

	@Entity
	@Access( AccessType.FIELD )
	static class OverrideOwner {
		@Id
		long id;
		@Transient
		List<FieldDetail> details;

		@ElementCollection
		@Access( AccessType.PROPERTY )
		public List<FieldDetail> getDetails() {
			return details;
		}
		public void setDetails(List<FieldDetail> details) {
			this.details = details;
		}
	}

	@Entity
	@Access( AccessType.PROPERTY )
	static class FieldOverrideOwner {
		long id;
		@ElementCollection
		@Access( AccessType.FIELD )
		List<PropertyDetail> details;

		@Id
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	@Access( AccessType.PROPERTY )
	static class ExplicitOwner {
		@Id
		@Access( AccessType.FIELD )
		long id;
		List<ExplicitFieldDetail> details;

		@ElementCollection
		public List<ExplicitFieldDetail> getDetails() {
			return details;
		}
		public void setDetails(List<ExplicitFieldDetail> details) {
			this.details = details;
		}
	}

	@MappedSuperclass
	@Access( AccessType.PROPERTY )
	static class BaseOwner {
		@Id
		@Access( AccessType.FIELD )
		long id;
		List<PropertyDetail> details;

		@ElementCollection
		public List<PropertyDetail> getDetails() {
			return details;
		}
		public void setDetails(List<PropertyDetail> details) {
			this.details = details;
		}
	}

	@Entity
	@Access( AccessType.FIELD )
	static class SubclassOwner extends BaseOwner {}

	@Entity
	static class EmbeddedOwner {
		@Id
		long id;
		@Embedded
		Container container;
	}

	@Embeddable
	@Access( AccessType.PROPERTY )
	static class Container {
		List<PropertyDetail> details;

		@ElementCollection
		public List<PropertyDetail> getDetails() {
			return details;
		}
		public void setDetails(List<PropertyDetail> details) {
			this.details = details;
		}
	}

	@Entity
	@Access( AccessType.FIELD )
	static class MapOwner {
		@Transient
		long id;
		@ElementCollection
		Map<FieldDetail, String> details;

		@Id
		@Access( AccessType.PROPERTY )
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	@Access( AccessType.FIELD )
	static class MapOverrideOwner {
		@Id
		long id;
		@Transient
		Map<FieldDetail, String> details;

		@ElementCollection
		@Access( AccessType.PROPERTY )
		public Map<FieldDetail, String> getDetails() {
			return details;
		}
		public void setDetails(Map<FieldDetail, String> details) {
			this.details = details;
		}
	}

	@Embeddable
	static class PropertyDetail {
		PropertyNested nested;
		@Embedded
		public PropertyNested getNested() {
			return nested;
		}
		public void setNested(PropertyNested nested) {
			this.nested = nested;
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

	@Embeddable
	static class FieldDetail {
		@Embedded
		FieldNested nested;
	}

	@Embeddable
	@Access( AccessType.FIELD )
	static class ExplicitFieldDetail {
		@Embedded
		FieldNested nested;
	}

	@Embeddable
	static class FieldNested {
		String description;
	}
}
