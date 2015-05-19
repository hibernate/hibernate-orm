/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.buildtime.spi;

import java.io.File;
import java.util.Set;

/**
 * Basic contract for performing instrumentation.
 *
 * @author Steve Ebersole
 */
public interface Instrumenter {
	/**
	 * Perform the instrumentation.
	 *
	 * @param files The file on which to perform instrumentation
	 */
	public void execute(Set<File> files);

	/**
	 * Instrumentation options.
	 */
	public static interface Options {
		/**
		 * Should we enhance references to class fields outside the class itself?
		 *
		 * @return {@literal true}/{@literal false}
		 */
		public boolean performExtendedInstrumentation();
	}
}
