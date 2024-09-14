/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.spi;

import jakarta.persistence.TableGenerator;

/**
 * Global registration of a table generator
 *
 * @author Steve Ebersole
 */
public record TableGeneratorRegistration(String name, TableGenerator configuration) {
}
