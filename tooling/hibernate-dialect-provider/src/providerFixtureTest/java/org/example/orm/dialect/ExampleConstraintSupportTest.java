/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.util.List;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.constraint.spi.CheckConstraintPlacement;
import org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest;
import org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest;
import org.hibernate.dialect.constraint.spi.ForeignKeyDropRequest;
import org.hibernate.dialect.schema.spi.CommentPlacement;
import org.hibernate.dialect.schema.spi.CommentRequest;
import org.hibernate.dialect.schema.spi.CommentTarget;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentation;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentationRequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the standalone provider's constraint strategy surface.
///
/// @author Steve Ebersole
class ExampleConstraintSupportTest {
	private final ExampleDialect dialect = new ExampleDialect();

	@Test
	void suppliesStableProviderOwnedStrategies() {
		assertSame( dialect.getForeignKeySupport(), dialect.getForeignKeySupport() );
		assertSame( dialect.getUniqueDelegate(), dialect.getUniqueDelegate() );
		assertSame( dialect.getCheckConstraintSupport(), dialect.getCheckConstraintSupport() );
		assertSame( dialect.getSchemaCommentSupport(), dialect.getSchemaCommentSupport() );
		assertInstanceOf( DelegatingUniqueDelegate.class, dialect.getUniqueDelegate() );
		assertTrue( dialect.addPartitionKeyToPrimaryKey() );
	}

	@Test
	void rendersAndReportsCustomForeignKeyBehavior() {
		assertEquals(
				"add fixture foreign key fk_orders_customer (customer_id) references customer",
				dialect.getForeignKeySupport().renderAddConstraint(
						ForeignKeyConstraintRequest.structured(
								"fk_orders_customer",
								List.of( "customer_id" ),
								"customer",
								List.of( "id" ),
								false
						)
				)
		);
		assertEquals(
				"add fixture foreign key fk_orders_customer foreign key (customer_id) references customer",
				dialect.getForeignKeySupport().renderAddConstraint(
						ForeignKeyConstraintRequest.explicit(
								"fk_orders_customer",
								"foreign key (customer_id) references customer"
						)
				)
		);
		assertEquals(
				"remove fixture foreign key fk_orders_customer",
				dialect.getForeignKeySupport().renderDropConstraint(
						new ForeignKeyDropRequest( "fk_orders_customer", ExistenceCheckPlacement.AFTER_NAME )
				)
		);
		assertTrue( dialect.getForeignKeySupport().supportsOnDeleteAction( OnDeleteAction.CASCADE ) );
		assertTrue( dialect.getForeignKeySupport().supportsOnDeleteAction( OnDeleteAction.NO_ACTION ) );
		assertFalse( dialect.getForeignKeySupport().supportsOnDeleteAction( OnDeleteAction.SET_NULL ) );
		assertTrue( dialect.getForeignKeySupport().requiresSelfReferentialForeignKeyNullification() );
	}

	@Test
	void rendersCustomChecksCommentsAndUniqueRepresentation() {
		assertTrue( dialect.getCheckConstraintSupport().supports( CheckConstraintPlacement.ANONYMOUS_COLUMN ) );
		assertFalse( dialect.getCheckConstraintSupport().supports( CheckConstraintPlacement.NAMED_COLUMN ) );
		assertTrue( dialect.getCheckConstraintSupport().supports( CheckConstraintPlacement.TABLE ) );
		assertEquals(
				"check not enforced (quantity > 0)",
				dialect.getCheckConstraintSupport().render(
						new CheckConstraintRenderRequest(
								CheckConstraintPlacement.TABLE,
								null,
								"quantity > 0",
								"not enforced"
						)
				)
		);

		assertEquals( CommentPlacement.STATEMENT, dialect.getSchemaCommentSupport().placement( CommentTarget.TABLE ) );
		assertEquals(
				"fixture comment on table orders is 'owner''s table'",
				dialect.getSchemaCommentSupport().render(
						new CommentRequest( CommentTarget.TABLE, "orders", "owner's table" )
				)
		);
		assertEquals(
				" fixture comment 'owner''s column'",
				dialect.getSchemaCommentSupport().render(
						new CommentRequest( CommentTarget.TABLE_COLUMN, "orders.customer_id", "owner's column" )
				)
		);
		assertEquals( CommentPlacement.NONE, dialect.getSchemaCommentSupport().placement( CommentTarget.USER_DEFINED_TYPE ) );

		assertEquals(
				UniqueKeyRepresentation.CONSTRAINT,
				dialect.getUniqueDelegate().representation( new UniqueKeyRepresentationRequest( false, false, false ) )
		);
		assertEquals(
				UniqueKeyRepresentation.INDEX,
				dialect.getUniqueDelegate().representation( new UniqueKeyRepresentationRequest( false, true, false ) )
		);
	}
}
