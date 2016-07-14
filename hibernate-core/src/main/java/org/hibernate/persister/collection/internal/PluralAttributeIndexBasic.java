/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.internal;

import org.hibernate.persister.common.spi.AbstractPluralAttributeIndex;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexBasic extends AbstractPluralAttributeIndex<BasicType,org.hibernate.sqm.domain.BasicType> {
	public PluralAttributeIndexBasic(
			BasicType ormType,
			org.hibernate.sqm.domain.BasicType sqmType,
			Column[] columns) {
		super( ormType, sqmType, columns );
	}
}
