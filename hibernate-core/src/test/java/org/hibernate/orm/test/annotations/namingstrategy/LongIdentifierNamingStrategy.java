/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.namingstrategy;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;

public class LongIdentifierNamingStrategy
		extends ImplicitNamingStrategyJpaCompliantImpl {

	@Override
	public Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source) {
		return limitIdentifierName(super.determineForeignKeyName( source ));
	}

	@Override
	public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source) {
		return limitIdentifierName(super.determineUniqueKeyName( source ));
	}

	@Override
	public Identifier determineIndexName(ImplicitIndexNameSource source) {
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
