/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cdi.extended;

import javax.ejb.LocalBean;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

/**
 * @author Vlad Mihalcea
 */
@Stateful
@LocalBean
public class NonConversationalEventManager {

	@PersistenceContext(type = PersistenceContextType.EXTENDED)
	private EntityManager em;

	public Event saveEvent(String eventName) {
		final Event event = new Event();
		event.setName( eventName );
		em.persist( event );
		return event;
	}

	public void endConversation() {
		em.flush();
	}
}
