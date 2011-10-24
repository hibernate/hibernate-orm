/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tuple.component;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tuple.Tuplizer;

/**
 * Defines further responsibilities regarding tuplization based on
 * a mapped components.
 * </p>
 * ComponentTuplizer implementations should have the following constructor signature:
 *      (org.hibernate.mapping.Component)
 * 
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface ComponentTuplizer extends Tuplizer, Serializable {
	/**
	 * Retreive the current value of the parent property.
	 *
	 * @param component The component instance from which to extract the parent
	 * property value.
	 * @return The current value of the parent property.
	 */
	public Object getParent(Object component);

    /**
     * Set the value of the parent property.
     *
     * @param component The component instance on which to set the parent.
     * @param parent The parent to be set on the comonent.
     * @param factory The current session factory.
     */
	public void setParent(Object component, Object parent, SessionFactoryImplementor factory);

	/**
	 * Does the component managed by this tuuplizer contain a parent property?
	 *
	 * @return True if the component does contain a parent property; false otherwise.
	 */
	public boolean hasParentProperty();

	/**
	 * Is the given method available via the managed component as a property getter?
	 *
	 * @param method The method which to check against the managed component.
	 * @return True if the managed component is available from the managed component; else false.
	 */
	public boolean isMethodOf(Method method);
}
