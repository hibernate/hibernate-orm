package org.hibernate.orm.test.cut;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.CompositeType;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A {@code @Column} annotation on a composite attribute used to be silently ignored.
 */
@Jira("https://hibernate.atlassian.net/browse/HHH-21113")
public class CompositeTypeColumnTest {

	@Test
	public void testColumnOnCompositeTypeAttribute() {
		assertRejected( WalletWithColumn.class, "WalletWithColumn.amount" );
	}

	@Test
	public void testColumnOnEmbeddedAttribute() {
		assertRejected( WalletWithEmbeddedColumn.class, "WalletWithEmbeddedColumn.owner" );
	}

	@Test
	public void testColumnOnCompositeTypeAttributeOfEmbeddable() {
		assertRejected( WalletWithNestedColumn.class, "WalletWithNestedColumn.balance.amount" );
	}

	@Test
	public void testAttributeOverride() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadataSources =
					new MetadataSources( registry )
							.addAnnotatedClass( WalletWithOverride.class );
			assertDoesNotThrow( () -> metadataSources.buildMetadata() );
		}
	}

	private static void assertRejected(Class<?> entityClass, String path) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadataSources =
					new MetadataSources( registry )
							.addAnnotatedClass( entityClass );
			final var thrown = assertThrows( AnnotationException.class, metadataSources::buildMetadata );
			assertThat( thrown.getMessage() )
					.contains( path )
					.contains( "may not be annotated '@Column'" )
					.contains( "@AttributeOverride" );
		}
	}

	@Entity(name = "WalletWithColumn")
	@Table(name = "wallet_c")
	public static class WalletWithColumn {
		@Id
		Integer id;

		@CompositeType(ImmutableMonetoryAmountUserType.class)
		@Column(name = "amount_value")
		ImmutableMonetoryAmount amount;
	}

	@Entity(name = "WalletWithOverride")
	@Table(name = "wallet_o")
	public static class WalletWithOverride {
		@Id
		Integer id;

		@CompositeType(ImmutableMonetoryAmountUserType.class)
		@AttributeOverride(name = "amount", column = @Column(name = "amount_value"))
		@AttributeOverride(name = "currency", column = @Column(name = "amount_currency"))
		ImmutableMonetoryAmount amount;
	}

	@Entity(name = "WalletWithEmbeddedColumn")
	@Table(name = "wallet_e")
	public static class WalletWithEmbeddedColumn {
		@Id
		Integer id;

		@Embedded
		@Column(name = "owner_name")
		Owner owner;
	}

	@Entity(name = "WalletWithNestedColumn")
	@Table(name = "wallet_n")
	public static class WalletWithNestedColumn {
		@Id
		Integer id;

		@Embedded
		Balance balance;
	}

	@Embeddable
	public static class Owner {
		String name;
	}

	@Embeddable
	public static class Balance {
		@CompositeType(ImmutableMonetoryAmountUserType.class)
		@Column(name = "amount_value")
		ImmutableMonetoryAmount amount;
	}
}
