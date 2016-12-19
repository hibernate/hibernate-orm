/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.spi;

import org.hibernate.persister.collection.spi.PluralAttributeIndex;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPluralAttributeIndex<O extends Type, S extends org.hibernate.sqm.domain.DomainReference>
		implements PluralAttributeIndex<O,S> {

	private final O ormType;
	private final S sqmType;
	private final Column[] columns;

	public AbstractPluralAttributeIndex(O ormType, S sqmType, Column[] columns) {
		this.ormType = ormType;
		this.sqmType = sqmType;
		this.columns = columns;
	}

	@Override
	public O getOrmType() {
		return ormType;
	}

	@Override
	public S getSqmType() {
		return sqmType;
	}

	@Override
	public Column[] getColumns() {
		return columns;
	}
}