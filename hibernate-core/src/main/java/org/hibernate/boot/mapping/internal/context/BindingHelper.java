/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.mapping.internal.relational.QuotedIdentifierTarget;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/// Small helpers for identifier conversion and global quoting.
///
/// These helpers centralize the interaction between [BindingOptionsImpl], the
/// JDBC identifier helper, and Hibernate's object-name normalizer.  They are used
/// by table, column, and discriminator binders whenever source text needs to be
/// converted into logical identifiers or SQL fragments. Names processed by the
/// physical naming strategy retain source quoting here; global and automatic
/// quoting are applied afterward. Other identifiers and SQL fragments retain
/// their existing quoting path.
///
/// @since 9.0
/// @author Steve Ebersole
public class BindingHelper {
	public static Identifier toIdentifier(
			String name,
			QuotedIdentifierTarget target,
			BindingOptions options,
			JdbcEnvironment jdbcEnvironment) {
		return toIdentifier( name, target, options, jdbcEnvironment, false );
	}

	public static Identifier toIdentifier(
			String name,
			QuotedIdentifierTarget target,
			BindingOptions options,
			JdbcEnvironment jdbcEnvironment,
			boolean explicit) {
		if ( target == QuotedIdentifierTarget.TABLE_NAME || target == QuotedIdentifierTarget.COLUMN_NAME
				|| target == QuotedIdentifierTarget.CATALOG_NAME || target == QuotedIdentifierTarget.SCHEMA_NAME
				|| target == QuotedIdentifierTarget.SEQUENCE_NAME || target == QuotedIdentifierTarget.TYPE_NAME ) {
			return Identifier.toIdentifier( name, false, false, explicit );
		}
		final boolean globallyQuoted = options.getGloballyQuotedIdentifierTargets().contains( target );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name, globallyQuoted, explicit );
	}

	public static String applyGlobalQuoting(
			String text,
			QuotedIdentifierTarget target,
			BindingOptions options,
			BindingState bindingState) {
		final ObjectNameNormalizer objectNameNormalizer = bindingState
				.getMetadataBuildingContext()
				.getObjectNameNormalizer();
		if ( target == QuotedIdentifierTarget.COLUMN_DEFINITION ) {
			return objectNameNormalizer.applyGlobalQuoting( text );
		}
		final boolean globallyQuoted = options.getGloballyQuotedIdentifierTargets().contains( target );
		if ( !globallyQuoted ) {
			return text;
		}
		return objectNameNormalizer.applyGlobalQuoting( text );
	}
}
