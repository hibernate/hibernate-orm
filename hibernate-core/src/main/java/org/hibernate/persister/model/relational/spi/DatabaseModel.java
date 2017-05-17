/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.model.relational.spi;

import java.util.Collection;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface DatabaseModel {
	Collection<Namespace> getNameSpaces();

}
