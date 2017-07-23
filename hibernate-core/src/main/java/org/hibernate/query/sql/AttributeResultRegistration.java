/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql;

import java.util.List;

/**
 * Describes the mapping of a specific persistent attribute
 * of an entity mapping.  See
 * {@link EntityResultRegistration#getAttributeResultRegistrations}.
 *
 * Defined via any of:
 *
 * 		* `<return-field/>` in `hbm.xml`
 * 	    * {@link javax.persistence.FieldResult}
 * 	    * `<column-result/>` in `orm.xml`
 *
 * @author Steve Ebersole
 */
public interface AttributeResultRegistration extends ResultRegistration {
	List<String> getColumnAliases();
}
