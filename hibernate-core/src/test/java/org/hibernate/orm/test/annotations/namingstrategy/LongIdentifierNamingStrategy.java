/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import jakarta.annotation.Nonnull;
import org.hibernate.relational.naming.spi.LogicalName;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.spi.ForeignKeyNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.spi.UniqueKeyNamingInput;

public class LongIdentifierNamingStrategy
		extends ImplicitNamingStrategyJpaCompliantImpl {

	@Override
	@Nonnull
	public LogicalName determineForeignKeyName(@Nonnull ForeignKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
		final var name = super.determineForeignKeyName( input, context );
		return context.implicitName( name.getText().substring( 0, Math.min( 30, name.getText().length() ) ), name.isQuoted() );
	}

	@Override
	@Nonnull
	public LogicalName determineUniqueKeyName(@Nonnull UniqueKeyNamingInput input, @Nonnull ImplicitNamingContext context) {
		final var name = super.determineUniqueKeyName( input, context );
		return context.implicitName( name.getText().substring( 0, Math.min( 30, name.getText().length() ) ), name.isQuoted() );
	}

	@Override
	@Nonnull
	public Identifier determineIndexName(@Nonnull ImplicitIndexNameSource source) {
		return limitIdentifierName(super.determineIndexName( source ));
	}

	public Identifier limitIdentifierName(Identifier identifier) {
		String text = identifier.getText();
		if(text.length() > 30) {
			return new Identifier( text.substring( 0, 30 ), identifier.isQuoted() );
		}
		return identifier;
	}
}
