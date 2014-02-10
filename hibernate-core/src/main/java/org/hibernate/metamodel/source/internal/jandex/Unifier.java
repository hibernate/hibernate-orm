/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.jandex;

import java.util.List;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPersistenceUnitMetadata;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.xml.spi.BindResult;

import org.jboss.jandex.IndexView;

/**
 * Responsible for consolidating mapping information supplied by annotations and mapping information
 * supplied by XML into a unified view.
 * <p/>
 * Ultimately we are building a Jandex {@link org.jboss.jandex.IndexView} which is a de-typed (and
 * classloading safe!) representation of annotations.  We add virtual annotation information into the
 * Jandex to represent XML supplied information.
 *
 * @author Steve Ebersole
 * @author Strong Liu
 */
public class Unifier {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( Unifier.class );

	// todo : per Jason, it is bad if we create a CompositeIndex where multiple of the aggregated indexes contain the same classes.

	public static IndexView unify(
			IndexView initialJandex,
			List<BindResult<JaxbEntityMappings>> xmlBindings,
			ServiceRegistry serviceRegistry) {
		if ( xmlBindings == null || xmlBindings.isEmpty() ) {
			// if there is no XML information, just return the original index
			return initialJandex;
		}

		JaxbPersistenceUnitMetadata persistenceUnitMetadata = null;

		for ( BindResult<JaxbEntityMappings> xmlBinding : xmlBindings ) {
			if ( xmlBinding.getRoot().getPersistenceUnitMetadata() != null ) {
				if ( persistenceUnitMetadata == null ) {
					log.debugf( "Using <persistence-unit-metadata/> located in %s", xmlBinding.getOrigin() );
					persistenceUnitMetadata = xmlBinding.getRoot().getPersistenceUnitMetadata();
				}
				else {
					// todo : parameterize duplicateMetadata() to accept the origin
					log.duplicateMetadata();
					log.debugf(
							"Encountered <persistence-unit-metadata/> in %s after previously " +
									"encountered one; keeping original",
							xmlBinding.getOrigin()
					);
				}
			}
		}

		// for now, simply hook into the existing code...
		return new EntityMappingsMocker( xmlBindings, initialJandex, serviceRegistry ).mockNewIndex();
	}
}
