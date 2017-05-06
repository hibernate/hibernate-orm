/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public interface ValueMapping {
	MappedTable getMappedTable();
	List<MappedColumn> getMappedColumns();

	Type getType() throws MappingException;
	FetchMode getFetchMode();

	// todo (6.0) : others as deemed appropriate - see o.h.mapping.Value
}
