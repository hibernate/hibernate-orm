/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Locale;

import org.hibernate.boot.model.source.spi.UniqueKeyConstraintSource;

/**
 * @author Steve Ebersole
 */
class UniqueKeyConstraintSourceImpl extends AbstractConstraintSource implements UniqueKeyConstraintSource {
	UniqueKeyConstraintSourceImpl(String name, String tableName) {
		super( name, tableName );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ENGLISH,
				"UniqueKeyConstraintSource{name='%s', tableName='%s', columnNames='%s', orderings=<not-implemented>}",
				name,
				tableName,
				columnNames
		);
	}

}
