/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.ejb3;
import java.util.HashMap;
import java.util.Map;

public class Company {
	int id;
	Map organization = new HashMap();
	Map conferenceRoomExtensions = new HashMap();
}
