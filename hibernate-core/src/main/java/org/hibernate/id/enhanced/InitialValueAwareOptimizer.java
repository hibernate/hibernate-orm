/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

/**
 * Marker interface for optimizer which wish to know the user-specified initial value.
 * <p/>
 * Used instead of constructor injection since that is already a public understanding and
 * because not all optimizers care.
 *
 * @author Steve Ebersole
 */
public interface InitialValueAwareOptimizer {
	/**
	 * Reports the user specified initial value to the optimizer.
	 * <p/>
	 * <tt>-1</tt> is used to indicate that the user did not specify.
	 *
	 * @param initialValue The initial value specified by the user, or <tt>-1</tt> to indicate that the
	 * user did not specify.
	 */
	public void injectInitialValue(long initialValue);
}
