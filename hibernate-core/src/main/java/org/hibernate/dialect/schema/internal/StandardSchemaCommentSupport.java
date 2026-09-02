/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.internal;

import org.hibernate.Internal;
import org.hibernate.dialect.schema.spi.CommentPlacement;
import org.hibernate.dialect.schema.spi.CommentRequest;
import org.hibernate.dialect.schema.spi.CommentTarget;
import org.hibernate.dialect.schema.spi.SchemaCommentSupport;

/// Hibernate-owned stock schema-comment rendering profiles.
///
/// @author Steve Ebersole
/// @since 8.0
@Internal
public enum StandardSchemaCommentSupport implements SchemaCommentSupport {
	NONE {
		@Override
		public CommentPlacement placement(CommentTarget target) {
			return CommentPlacement.NONE;
		}
	},
	COMMENT_ON {
		@Override
		public CommentPlacement placement(CommentTarget target) {
			return CommentPlacement.STATEMENT;
		}
	},
	HANA_INLINE {
		@Override
		public CommentPlacement placement(CommentTarget target) {
			return isTableTarget( target ) ? CommentPlacement.INLINE : CommentPlacement.STATEMENT;
		}

		@Override
		protected String inline(CommentRequest request) {
			return " comment '" + escape( request.comment() ) + "'";
		}
	},
	MYSQL_INLINE {
		@Override
		public CommentPlacement placement(CommentTarget target) {
			return isTableTarget( target ) ? CommentPlacement.INLINE : CommentPlacement.STATEMENT;
		}

		@Override
		protected String inline(CommentRequest request) {
			return request.target() == CommentTarget.TABLE
					? " comment='" + escape( request.comment() ) + "'"
					: " comment '" + escape( request.comment() ) + "'";
		}
	};

	@Override
	public String render(CommentRequest request) {
		return switch ( placement( request.target() ) ) {
			case NONE -> "";
			case INLINE -> inline( request );
			case STATEMENT -> statement( request );
		};
	}

	protected String inline(CommentRequest request) {
		throw new IllegalStateException( "Profile does not support inline comments" );
	}

	private static String statement(CommentRequest request) {
		final String target = switch ( request.target() ) {
			case TABLE -> "table ";
			case TABLE_COLUMN, USER_DEFINED_TYPE_COLUMN -> "column ";
			case USER_DEFINED_TYPE -> "type ";
		};
		return "comment on " + target + request.qualifiedName() + " is '" + escape( request.comment() ) + "'";
	}

	private static boolean isTableTarget(CommentTarget target) {
		return switch ( target ) {
			case TABLE, TABLE_COLUMN -> true;
			case USER_DEFINED_TYPE, USER_DEFINED_TYPE_COLUMN -> false;
		};
	}

	private static String escape(String comment) {
		return comment.replace( "'", "''" );
	}
}
