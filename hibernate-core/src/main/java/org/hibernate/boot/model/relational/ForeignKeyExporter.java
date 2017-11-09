/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.mapping.ForeignKey;

/**
 * Any ValueMapping which exports a foreign key
 *
 * @author Steve Ebersole
 */
public interface ForeignKeyExporter extends ValueMapping {
}
