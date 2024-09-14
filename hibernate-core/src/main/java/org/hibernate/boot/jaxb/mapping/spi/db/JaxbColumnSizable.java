/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi.db;

/**
 * @author Steve Ebersole
 */
public interface JaxbColumnSizable extends JaxbColumn {
	Integer getLength();

	default Integer getPrecision() {
		return null;
	}

	default Integer getScale() {
		return null;
	}

	default Integer getSecondPrecision() {
		return null;
	}
}
