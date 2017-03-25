/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.namingstrategy;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;

@SuppressWarnings("serial")
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
