/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi.db;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbCheckConstraintImpl;

/**
 * Composition of the aspects of column definition most commonly exposed in XSD "column types"
 *
 * @author Steve Ebersole
 */
public interface JaxbColumnCommon
		extends JaxbColumn, JaxbColumnMutable, JaxbCheckable, JaxbColumnNullable, JaxbColumnUniqueable, JaxbColumnDefinable, JaxbCommentable {
	@Override
	default String getTable() {
		return null;
	}

	@Override
	default Boolean isNullable() {
		return null;
	}

	@Override
	default Boolean isInsertable() {
		return null;
	}

	@Override
	default Boolean isUpdatable() {
		return null;
	}

	@Override
	default String getComment() {
		return null;
	}

	@Override
	default Boolean isUnique() {
		return null;
	}

	@Override
	default List<JaxbCheckConstraintImpl> getCheckConstraints() {
		return Collections.emptyList();
	}
}
