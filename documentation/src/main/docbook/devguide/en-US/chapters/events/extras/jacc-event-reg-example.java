/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.secure.internal.JACCPreDeleteEventListener;
import org.hibernate.secure.internal.JACCPreInsertEventListener;
import org.hibernate.secure.internal.JACCPreLoadEventListener;
import org.hibernate.secure.internal.JACCPreUpdateEventListener;
import org.hibernate.secure.internal.JACCSecurityListener;

public class JaccEventListenerIntegrator implements Integrator {

	private static final DuplicationStrategy JACC_DUPLICATION_STRATEGY = new DuplicationStrategy() {
		@Override
		public boolean areMatch(Object listener, Object original) {
			return listener.getClass().equals( original.getClass() ) &&
					JACCSecurityListener.class.isInstance( original );
		}

		@Override
		public Action getAction() {
			return Action.KEEP_ORIGINAL;
		}
	};

	@Override
	@SuppressWarnings( {"unchecked"})
	public void integrate(
			Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		boolean isSecurityEnabled = configuration.getProperties().containsKey( AvailableSettings.JACC_ENABLED );
		if ( !isSecurityEnabled ) {
			return;
		}

		final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		eventListenerRegistry.addDuplicationStrategy( JACC_DUPLICATION_STRATEGY );

		final String jaccContextId = configuration.getProperty( Environment.JACC_CONTEXTID );
		eventListenerRegistry.prependListeners( EventType.PRE_DELETE, new JACCPreDeleteEventListener(jaccContextId) );
		eventListenerRegistry.prependListeners( EventType.PRE_INSERT, new JACCPreInsertEventListener(jaccContextId) );
		eventListenerRegistry.prependListeners( EventType.PRE_UPDATE, new JACCPreUpdateEventListener(jaccContextId) );
		eventListenerRegistry.prependListeners( EventType.PRE_LOAD, new JACCPreLoadEventListener(jaccContextId) );
	}
}