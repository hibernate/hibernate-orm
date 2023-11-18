/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.spi.db;

import org.hibernate.boot.jaxb.mapping.spi.JaxbSchemaAware;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbCheckable;
import org.hibernate.boot.jaxb.mapping.spi.db.JaxbDatabaseObject;

/**
 * @author Steve Ebersole
 */
public interface JaxbTableMapping extends JaxbSchemaAware, JaxbCheckable, JaxbDatabaseObject {
	String getComment();
	String getOptions();
}
