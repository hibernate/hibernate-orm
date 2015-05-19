/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations;
import java.util.Comparator;

/**
 * Ticket comparator ordering longest first
 *
 * @author Emmanuel Bernard
 */
public class TicketComparator implements Comparator<Ticket> {

	public int compare(Ticket ticket, Ticket ticket1) {
		if ( ticket == null || ticket1 == null ) {
			throw new IllegalStateException( "Ticket comparison only available through non null tickets" );
		}
		return ticket1.getNumber().length() - ticket.getNumber().length();

	}
}
