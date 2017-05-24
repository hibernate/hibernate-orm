/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface ValueMapping<J> {
	MappedTable getMappedTable();
	List<MappedColumn> getMappedColumns();

	FetchMode getFetchMode();

	MetadataBuildingContext getMetadataBuildingContext();

	JavaTypeDescriptor<J> getJavaTypeDescriptor();
}
