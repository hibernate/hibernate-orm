/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
