package org.hibernate.bytecode.util;

/**
 * Used to determine whether a class should be instrumented.
 *
 * @author Steve Ebersole
 */
public interface ClassFilter {
		public boolean shouldInstrumentClass(String className);
}
