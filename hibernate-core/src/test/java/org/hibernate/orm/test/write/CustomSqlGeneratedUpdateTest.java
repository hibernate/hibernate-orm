package org.hibernate.orm.test.write;

import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = CustomSqlGeneratedUpdateTest.Item.class)
@SessionFactory
class CustomSqlGeneratedUpdateTest {
	@Test
	void customUpdateBindsUnchangedAttributes(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new Item();
			item.id = 1L;
			item.description = "original";
			item.label = "unchanged";
			session.persist( item );
		} );
		scope.inTransaction( session -> session.find( Item.class, 1L ).description = "changed" );
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1L );
			assertThat( item.description ).isEqualTo( "changed" );
			assertThat( item.label ).isEqualTo( "unchanged" );
			item.label = "updated";
		} );
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1L );
			assertThat( item.description ).isEqualTo( "changed" );
			assertThat( item.label ).isEqualTo( "updated" );
			session.remove( item );
		} );
	}

	@Entity(name = "CustomDynamicItem")
	@Table(name = "custom_dynamic_item")
	@SQLUpdate(sql = "update custom_dynamic_item set description=?, label=?, updated_at=? where id=?")
	static class Item {
		@Id Long id;
		String description;
		String label;
		@UpdateTimestamp
		@jakarta.persistence.Column(name = "updated_at")
		java.time.LocalDateTime updatedAt;
	}
}
