/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.mapping.PersistentClass;

/**
 * Hibernate builds its {@linkplain org.hibernate.mapping build-time model}
 * incrementally, often delaying operations until other pieces of information
 * are available. A second pass represents one of these delayed operations.
 *
 * @author Gavin King
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface SecondPass extends Serializable {
	/**
	 * Perform the operation
	 */
	void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException;
}
