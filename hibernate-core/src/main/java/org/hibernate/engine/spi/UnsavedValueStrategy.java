/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.spi;

/**
 * The base contract for determining transient status versus detached status.
 *
 * @author Steve Ebersole
 */
public interface UnsavedValueStrategy {
	/**
	 * Make the transient/detached determination
	 *
	 * @param test The value to be tested
	 *
	 * @return {@code true} indicates the value corresponds to unsaved data (aka, transient state; {@code false}
	 * indicates the value does not corresponds to unsaved data (aka, detached state); {@code null} indicates that
	 * this strategy was not able to determine conclusively.
	 */
	public Boolean isUnsaved(Object test);

	/**
	 * Get a default value meant to indicate transience.
	 *
	 * @param currentValue The current state value.
	 *
	 * @return The default transience value.
	 */
	public Object getDefaultValue(Object currentValue);
}
