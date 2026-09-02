/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.schema;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.constraint.spi.CheckConstraintPlacement;
import org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest;
import org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest;
import org.hibernate.dialect.constraint.spi.ForeignKeyDropRequest;
import org.hibernate.dialect.schema.spi.CommentPlacement;
import org.hibernate.dialect.schema.spi.CommentRequest;
import org.hibernate.dialect.schema.spi.CommentTarget;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.SchemaCommentSupports;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentation;
import org.hibernate.dialect.unique.spi.UniqueKeyRepresentationRequest;
import org.hibernate.testing.orm.junit.BaseUnitTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the focused constraint and schema-comment contracts.
///
/// @author Steve Ebersole
@BaseUnitTest
class ConstraintAndSchemaCommentSupportTests {
	private final Dialect dialect = new TestDialect();

	@Test
	void suppliesStableDefaultStrategies() {
		assertSame( dialect, dialect.getForeignKeySupport() );
		assertSame( dialect, dialect.getCheckConstraintSupport() );
		assertSame( dialect.getUniqueDelegate(), dialect.getUniqueDelegate() );
		assertSame( dialect.getSchemaCommentSupport(), dialect.getSchemaCommentSupport() );
	}

	@Test
	void validatesAndDefensivelyCopiesForeignKeyRequests() {
		final var sourceColumns = new ArrayList<>( List.of( "customer_id" ) );
		final var targetColumns = new ArrayList<>( List.of( "id" ) );
		final var request = ForeignKeyConstraintRequest.structured(
				"fk_order_customer",
				sourceColumns,
				"customer",
				targetColumns,
				false
		);
		sourceColumns.add( "mutated" );
		targetColumns.add( "mutated" );
		assertEquals( List.of( "customer_id" ), request.sourceColumnNames() );
		assertEquals( List.of( "id" ), request.targetColumnNames() );
		assertThrows( UnsupportedOperationException.class, () -> request.sourceColumnNames().add( "mutated" ) );

		assertThrows(
				IllegalArgumentException.class,
				() -> ForeignKeyConstraintRequest.structured(
						"fk_order_customer", List.of( "customer_id" ), "customer", List.of(), false
				)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> new ForeignKeyConstraintRequest(
						"fk_order_customer",
						List.of( "customer_id" ),
						"customer",
						List.of( "id" ),
						false,
						"foreign key (customer_id) references customer"
				)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> new ForeignKeyDropRequest( " ", ExistenceCheckPlacement.NONE )
		);
	}

	@Test
	void rendersStandardForeignKeyFormsAndSemantics() {
		assertEquals(
				"add constraint fk_order_customer foreign key (customer_id) references customer",
				dialect.renderAddConstraint( ForeignKeyConstraintRequest.structured(
						"fk_order_customer",
						List.of( "customer_id" ),
						"customer",
						List.of( "id" ),
						true
				) )
		);
		assertEquals(
				"add constraint fk_order_customer foreign key (customer_code) references customer (code)",
				dialect.renderAddConstraint( ForeignKeyConstraintRequest.structured(
						"fk_order_customer",
						List.of( "customer_code" ),
						"customer",
						List.of( "code" ),
						false
				) )
		);
		assertEquals(
				"add constraint fk_order_customer foreign key (customer_id) references customer on delete cascade",
				dialect.renderAddConstraint( ForeignKeyConstraintRequest.explicit(
						"fk_order_customer",
						"foreign key (customer_id) references customer on delete cascade"
				) )
		);
		assertEquals(
				"drop constraint if exists fk_order_customer",
				dialect.renderDropConstraint(
						new ForeignKeyDropRequest( "fk_order_customer", ExistenceCheckPlacement.BEFORE_NAME )
				)
		);
		assertEquals(
				"drop constraint fk_order_customer if exists",
				dialect.renderDropConstraint(
						new ForeignKeyDropRequest( "fk_order_customer", ExistenceCheckPlacement.AFTER_NAME )
				)
		);
		for ( OnDeleteAction action : OnDeleteAction.values() ) {
			assertTrue( dialect.supportsOnDeleteAction( action ) );
		}
		assertFalse( dialect.requiresSelfReferentialForeignKeyNullification() );
	}

	@Test
	void validatesAndRendersCheckConstraints() {
		assertEquals(
				"check (quantity > 0)",
				dialect.render( new CheckConstraintRenderRequest(
						CheckConstraintPlacement.ANONYMOUS_COLUMN,
						null,
						"quantity > 0",
						"ignored"
				) )
		);
		assertEquals(
				"constraint ck_quantity check (quantity > 0)",
				dialect.render( new CheckConstraintRenderRequest(
						CheckConstraintPlacement.TABLE,
						"ck_quantity",
						"quantity > 0",
						null
				) )
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> new CheckConstraintRenderRequest(
						CheckConstraintPlacement.ANONYMOUS_COLUMN, "ck_quantity", "quantity > 0", null
				)
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> new CheckConstraintRenderRequest(
						CheckConstraintPlacement.NAMED_COLUMN, null, "quantity > 0", null
				)
		);
		assertTrue( dialect.supports( CheckConstraintPlacement.ANONYMOUS_COLUMN ) );
		assertTrue( dialect.supports( CheckConstraintPlacement.NAMED_COLUMN ) );
		assertTrue( dialect.supports( CheckConstraintPlacement.TABLE ) );
	}

	@Test
	void rendersEveryStockSchemaCommentProfileAndTarget() {
		for ( CommentTarget target : CommentTarget.values() ) {
			assertEquals( CommentPlacement.NONE, SchemaCommentSupports.none().placement( target ) );
			assertEquals( "", SchemaCommentSupports.none().render( request( target ) ) );
			assertEquals( CommentPlacement.STATEMENT, SchemaCommentSupports.commentOn().placement( target ) );
		}
		assertEquals(
				"comment on table orders is 'owner''s table'",
				SchemaCommentSupports.commentOn().render( request( CommentTarget.TABLE ) )
		);
		assertEquals(
				"comment on column orders.customer_id is 'owner''s table'",
				SchemaCommentSupports.commentOn().render(
						new CommentRequest( CommentTarget.TABLE_COLUMN, "orders.customer_id", "owner's table" )
				)
		);
		assertEquals(
				"comment on type order_type is 'owner''s table'",
				SchemaCommentSupports.commentOn().render( request( CommentTarget.USER_DEFINED_TYPE ) )
		);
		assertEquals(
				"comment on column order_type.customer_id is 'owner''s table'",
				SchemaCommentSupports.commentOn().render(
						new CommentRequest(
								CommentTarget.USER_DEFINED_TYPE_COLUMN,
								"order_type.customer_id",
								"owner's table"
						)
				)
		);
		assertEquals( " comment 'owner''s table'", SchemaCommentSupports.hanaInline().render( request( CommentTarget.TABLE ) ) );
		assertEquals( " comment='owner''s table'", SchemaCommentSupports.mysqlInline().render( request( CommentTarget.TABLE ) ) );
		assertEquals(
				" comment 'owner''s table'",
				SchemaCommentSupports.mysqlInline().render(
						new CommentRequest( CommentTarget.TABLE_COLUMN, "orders.customer_id", "owner's table" )
				)
		);
		assertThrows( IllegalArgumentException.class, () -> new CommentRequest( CommentTarget.TABLE, " ", "comment" ) );
	}

	@Test
	void selectsUniqueRepresentationsThroughTheDelegate() {
		final var simple = new UniqueKeyRepresentationRequest( false, false, false );
		assertEquals( UniqueKeyRepresentation.CONSTRAINT, dialect.getUniqueDelegate().representation( simple ) );
		assertEquals(
				UniqueKeyRepresentation.INDEX,
				dialect.getUniqueDelegate().representation( new UniqueKeyRepresentationRequest( true, false, false ) )
		);
		assertEquals(
				UniqueKeyRepresentation.INDEX,
				UniqueDelegates.alwaysIndex( dialect ).representation( simple )
		);
		assertEquals( UniqueKeyRepresentation.CONSTRAINT, UniqueDelegates.none().representation( simple ) );
		assertSame( UniqueDelegates.none(), UniqueDelegates.none() );
		assertThrows( NullPointerException.class, () -> UniqueDelegates.alterTable( null ) );
	}

	private static CommentRequest request(CommentTarget target) {
		return new CommentRequest( target, target.name().contains( "COLUMN" ) ? "orders.customer_id" :
				target == CommentTarget.USER_DEFINED_TYPE ? "order_type" : "orders", "owner's table" );
	}

	private static final class TestDialect extends Dialect {
		private TestDialect() {
			super( DatabaseVersion.make( 1 ) );
		}
	}
}
