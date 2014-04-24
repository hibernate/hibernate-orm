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
package org.hibernate.metamodel.internal.binder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.HierarchyDetails;

import org.jboss.logging.Logger;

/**
 * Acts as the central message hub for 2 types of events in the Binder ecosystem:<ul>
 *     <li>when an identifier is fully resolved</li>
 *     <li>when an attribute is fully resolved</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class BinderEventBus implements HibernateTypeDescriptor.ResolutionListener {
	private static final Logger log = Logger.getLogger( BinderEventBus.class );

	private List<IdentifierBindingListener> identifierBindingListeners;
	private List<AttributeBindingListener> attributeBindingListeners;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IdentifierBindingListener

	public void addIdentifierBindingListener(IdentifierBindingListener listener) {
		log.debugf( "Adding IdentifierBindingListener : %s ", listener );

		if ( identifierBindingListeners == null ) {
			identifierBindingListeners = new ArrayList<IdentifierBindingListener>();
		}
		identifierBindingListeners.add( listener );
	}

	public void removeIdentifierBindingListener(IdentifierBindingListener listener) {
		log.debugf( "Removing IdentifierBindingListener : %s ", listener );

		if ( identifierBindingListeners == null ) {
			throw new IllegalStateException( "No listeners defined" );
		}

		identifierBindingListeners.remove( listener );
	}

	private int identifierResolutionDepth = 0;

	private List<IdentifierBindingListener> completedIdentifierBindingListeners;

	public void fireIdentifierResolved(HierarchyDetails hierarchyDetails) {
		final String entityName = hierarchyDetails.getRootEntityBinding().getEntityName();
		log.debugf( "Starting 'identifier resolved' notifications : %s ", entityName );

		if ( identifierBindingListeners == null ) {
			return;
		}

		identifierResolutionDepth++;
		try {
			for ( IdentifierBindingListener identifierBindingListener : identifierBindingListeners ) {
				log.tracef(
						"   - sending 'identifier resolved' notification [%s] to IdentifierBindingListener : %s",
						entityName,
						identifierBindingListener
				);
				final boolean done = identifierBindingListener.identifierResolved( hierarchyDetails );
				if ( done ) {
					if ( completedIdentifierBindingListeners == null ) {
						completedIdentifierBindingListeners = new ArrayList<IdentifierBindingListener>();
					}
					completedIdentifierBindingListeners.add( identifierBindingListener );
				}
			}
		}
		finally {
			identifierResolutionDepth--;
		}

		if ( identifierResolutionDepth == 0 ) {
			if ( completedIdentifierBindingListeners != null
					&& !completedIdentifierBindingListeners.isEmpty() ) {
				identifierBindingListeners.removeAll( completedIdentifierBindingListeners );
			}
		}
	}

	public void finishUpIdentifiers() {
		// todo : how to best allow all listeners to report any un-resolved state and still throw an excetpion here?

		// for now, we throw an error if there are any remaining listeners
		if ( identifierBindingListeners != null && !identifierBindingListeners.isEmpty() ) {
			throw new IllegalStateException( "Could not resolve all identifiers" );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeBindingListener

	public void addAttributeBindingListener(AttributeBindingListener listener) {
		log.debugf( "Adding AttributeBindingListener : %s ", listener );

		if ( attributeBindingListeners == null ) {
			attributeBindingListeners = new ArrayList<AttributeBindingListener>();
		}
		attributeBindingListeners.add( listener );
	}

	public void removeAttributeBindingListener(AttributeBindingListener listener) {
		log.debugf( "Removing AttributeBindingListener : %s ", listener );

		if ( attributeBindingListeners == null ) {
			throw new IllegalStateException( "No listeners defined" );
		}

		attributeBindingListeners.remove( listener );
	}

	public void finishUpAttributes() {
		// todo : how to best allow all listeners to report any un-resolved state and still throw an excetpion here?
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// HibernateTypeDescriptor.ResolutionListener

	@Override
	public void typeResolved(HibernateTypeDescriptor typeDescriptor) {

	}


	public void fireEmbeddableResolved(EmbeddableBinding binding) {

	}
}
