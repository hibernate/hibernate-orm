/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.constraint.spi.CheckConstraintPlacement;
import org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest;
import org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest;
import org.hibernate.dialect.schema.spi.CommentPlacement;
import org.hibernate.dialect.schema.spi.CommentTarget;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentation;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentationRequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies representative community-Dialect constraint profiles.
///
/// @author Steve Ebersole
class ConstraintSupportTest {
	@Test
	void preservesInformixConstraintNamePlacement() {
		final var dialect = new InformixDialect();
		assertEquals(
				"add constraint foreign key (customer_code) references customer (code) constraint fk_order_customer",
				dialect.renderAddConstraint( ForeignKeyConstraintRequest.structured(
						"fk_order_customer",
						List.of( "customer_code" ),
						"customer",
						List.of( "code" ),
						false
				) )
		);
		assertEquals(
				"add constraint foreign key (customer_id) references customer constraint fk_order_customer",
				dialect.renderAddConstraint( ForeignKeyConstraintRequest.explicit(
						"fk_order_customer",
						"foreign key (customer_id) references customer"
				) )
		);
		assertEquals(
				"check (quantity > 0) constraint ck_quantity disabled",
				dialect.render( new CheckConstraintRenderRequest(
						CheckConstraintPlacement.TABLE,
						"ck_quantity",
						"quantity > 0",
						"disabled"
				) )
		);
		assertTrue( dialect.supports( CheckConstraintPlacement.ANONYMOUS_COLUMN ) );
		assertFalse( dialect.supports( CheckConstraintPlacement.NAMED_COLUMN ) );
	}

	@Test
	void suppressesUnsupportedSqliteAndSingleStoreForeignKeys() {
		final var sqlite = new SQLiteDialect();
		final var singleStore = new SingleStoreDialect();
		assertFalse( sqlite.supportsAlterTableConstraints() );
		assertFalse( singleStore.supportsAlterTableConstraints() );
		assertSame( sqlite.getUniqueDelegate(), sqlite.getUniqueDelegate() );
		assertSame( singleStore.getUniqueDelegate(), singleStore.getUniqueDelegate() );
		assertEquals(
				UniqueKeyRepresentation.CONSTRAINT,
				singleStore.getUniqueDelegate().representation(
						new UniqueKeyRepresentationRequest( false, false, false )
				)
		);
		assertEquals( "", singleStore.getUniqueDelegate().getColumnDefinitionUniquenessFragment( null, null ) );
		assertEquals( "", singleStore.getUniqueDelegate().getTableCreationUniqueConstraintsFragment( null, null ) );
	}

	@Test
	void preservesVersionedAndTargetAwareCommentProfiles() {
		final var hsql1 = new HSQLLegacyDialect( DatabaseVersion.make( 1 ) );
		final var hsql2 = new HSQLLegacyDialect( DatabaseVersion.make( 2 ) );
		assertEquals( CommentPlacement.NONE, hsql1.getSchemaCommentSupport().placement( CommentTarget.TABLE ) );
		assertEquals( CommentPlacement.STATEMENT, hsql2.getSchemaCommentSupport().placement( CommentTarget.TABLE ) );
		assertSame( hsql1.getSchemaCommentSupport(), hsql1.getSchemaCommentSupport() );
		assertSame( hsql2.getSchemaCommentSupport(), hsql2.getSchemaCommentSupport() );

		final var singleStore = new SingleStoreDialect();
		assertEquals(
				CommentPlacement.INLINE,
				singleStore.getSchemaCommentSupport().placement( CommentTarget.TABLE_COLUMN )
		);
		assertEquals(
				CommentPlacement.STATEMENT,
				singleStore.getSchemaCommentSupport().placement( CommentTarget.USER_DEFINED_TYPE )
		);
	}

	@Test
	void preservesSqlServerOptionPlacement() {
		final var dialect = new SQLServerLegacyDialect();
		assertEquals(
				"constraint ck_quantity check not for replication (quantity > 0)",
				dialect.render( new CheckConstraintRenderRequest(
						CheckConstraintPlacement.TABLE,
						"ck_quantity",
						"quantity > 0",
						"not for replication"
				) )
		);
	}
}
