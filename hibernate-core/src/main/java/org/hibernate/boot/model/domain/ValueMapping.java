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
import org.hibernate.service.ServiceRegistry;

/**
 * Corollary to {@link org.hibernate.metamodel.model.domain.spi.Navigable}
 * in the runtime model
 *
 * @author Steve Ebersole
 */
public interface ValueMapping<J> {
	MappedTable getMappedTable();

	List<MappedColumn> getMappedColumns();

	FetchMode getFetchMode();

	/**
	 * todo (6.0) : ? - expose this as LocalMetadataBuildingContext to get access to `#getOrigin`?
	 * 		all ValueMapping should be created relative to a local MetadataBuildingContext.  Using
	 * 		LocalMetadataBuildingContext would give us access to Origin which defines the
	 * 		localized mapping info (attribute, class, etc) which is useful for error messages
	 */
	MetadataBuildingContext getMetadataBuildingContext();

	JavaTypeMapping<J> getJavaTypeMapping();

	ServiceRegistry getServiceRegistry();

	default Boolean resolve(ResolutionContext context) {
		// force resolution of the Value's JavaTypeDescriptor
		getJavaTypeMapping().getJavaTypeDescriptor();
		return true;
	}
}
