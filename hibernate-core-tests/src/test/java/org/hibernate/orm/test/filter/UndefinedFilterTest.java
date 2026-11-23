package org.hibernate.orm.test.filter;

import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.annotations.Filter;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ServiceRegistry
@JiraKey("HHH-21135")
class UndefinedFilterTest {

	@ParameterizedTest
	@ValueSource(classes = { FilteredEntity.class, OneToManyOwner.class, ManyToManyOwner.class })
	void undefinedFilter(Class<?> filteredType, ServiceRegistryScope scope) {
		final var metadata = new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( filteredType )
				.addAnnotatedClass( Item.class )
				.buildMetadata();
		final var failure = assertThrows( MappingException.class, () -> {
			try ( var factory = metadata.buildSessionFactory() ) {
				// An explicit condition does not replace the filter definition.
			}
		} );
		assertThat( failure ).hasMessageContaining( filteredType.getName() )
				.hasMessageContaining( "undefined filter named 'noSuchFilter'" );
	}

	@Entity(name = "FilteredEntity")
	@Filter(name = "noSuchFilter", condition = "id > 0")
	static class FilteredEntity {
		@Id
		Long id;
	}

	@Entity(name = "OneToManyOwner")
	static class OneToManyOwner {
		@Id
		Long id;
		@OneToMany
		@JoinColumn(name = "owner_id")
		@Filter(name = "noSuchFilter", condition = "id > 0")
		Set<Item> items;
	}

	@Entity(name = "ManyToManyOwner")
	static class ManyToManyOwner {
		@Id
		Long id;
		@ManyToMany
		@Filter(name = "noSuchFilter", condition = "id > 0")
		Set<Item> items;
	}

	@Entity(name = "FilterItem")
	static class Item {
		@Id
		Long id;
	}
}
