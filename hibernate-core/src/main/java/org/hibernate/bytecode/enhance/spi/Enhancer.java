/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.enhance.spi;

/**
 * Class responsible for performing enhancement.
 *
 * @author Steve Ebersole
 * @author Jason Greene
 * @author Luis Barreiro
 */
public interface Enhancer {

	/**
	 * Performs the enhancement.
	 *
	 * It is possible to invoke this method concurrently, but when doing so make sure
	 * no two enhancement tasks are invoked on the same class in parallel: the
	 * Enhancer implementations are not required to guard against this.
	 *
	 * @param className The name of the class whose bytecode is being enhanced.
	 * @param originalBytes The class's original (pre-enhancement) byte code
	 *
	 * @return The enhanced bytecode. If the original bytes are not enhanced, null is returned.
	 *
	 * @throws EnhancementException Indicates a problem performing the enhancement
	 */
	byte[] enhance(String className, byte[] originalBytes) throws EnhancementException;
}
