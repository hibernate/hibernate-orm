/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi;

import javax.persistence.FetchType;

/**
 * Common interface for JAXB bindings that represent attributes with laziness and fetch style.
 *
 * @author Brett Meyer
 */
public interface FetchableAttribute {

	FetchType getFetch();

	void setFetch(FetchType value);

}
