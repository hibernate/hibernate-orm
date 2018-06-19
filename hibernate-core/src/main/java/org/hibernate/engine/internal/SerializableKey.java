/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Provides custom serialization for entities and collection keys
 *
 * @author Alvaro Esteban Pedraza - aepedraza3117@gmail.com
 */
public interface SerializableKey {

	/**
	 * Custom serialization routine used during serialization of a Session/PersistenceContext for increased performance.
	 *
	 * @param oos The stream to which we should write the serial data.
	 * 
	 * @throws IOException Thrown by Java I/O
	 */
	void serialize(ObjectOutputStream oos) throws IOException;
}
