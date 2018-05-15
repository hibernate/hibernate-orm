/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.filter.subclass.tableperclass;

import org.hibernate.test.annotations.filter.subclass.SubClassTest;

public class TablePerClassTest extends SubClassTest{
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{Animal.class, Mammal.class, Human.class};
	}
	
	@Override
	protected void persistTestData() {
		createHuman(false, 90);
		createHuman(false, 100);
		createHuman(true, 110);
	}

	private void createHuman(boolean pregnant, int iq){
		Human human = new Human();
		human.setName("Homo Sapiens");
		human.setPregnant(pregnant);
		human.setIq(iq);
		session.persist(human);
	}
	

}
