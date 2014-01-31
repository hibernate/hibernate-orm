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
package org.hibernate.cfg.beanvalidation;

import java.util.Map;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Defines the context needed to call the {@link TypeSafeActivator}
 *
 * @author Steve Ebersole
 */
public interface ActivationContext {
	/**
	 * Access the requested validation mode(s).
	 * <p/>
	 * IMPL NOTE : the legacy code allowed multiple mode values to be specified, so that is why it is multi-valued here.
	 * However, I cannot find any good reasoning why it was defined that way and even JPA states it should be a single
	 * value.  For 4.1 (in maintenance) I think it makes the most sense to not mess with it.  Discuss for
	 * 4.2 and beyond.
	 *
	 * @return The requested validation modes
	 */
	public Set<ValidationMode> getValidationModes();

	/**
	 * Access the Metadata object (processed mapping information)
	 *
	 * @return The Hibernate Metadata object
	 */
	public MetadataImplementor getMetadata();

	/**
	 * Access to all settings
	 *
	 * @return The settings
	 */
	public Map getSettings();

	/**
	 * Access the SessionFactory being built to trigger this BV activation
	 *
	 * @return The SessionFactory being built
	 */
	public SessionFactoryImplementor getSessionFactory();

	/**
	 * Access the ServiceRegistry specific to the SessionFactory being built.
	 *
	 * @return The SessionFactoryServiceRegistry
	 */
	public SessionFactoryServiceRegistry getServiceRegistry();
}
