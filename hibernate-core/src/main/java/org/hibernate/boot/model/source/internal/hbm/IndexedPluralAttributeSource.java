/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.PluralAttributeIndexSource;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;

/**
 * Describes a plural attribute that is indexed.  This can mean either a list
 * or a map.
 */
public interface IndexedPluralAttributeSource extends PluralAttributeSource {
	PluralAttributeIndexSource getIndexSource();
}
