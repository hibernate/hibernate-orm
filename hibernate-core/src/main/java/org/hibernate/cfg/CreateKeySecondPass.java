/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.RootClass;

/**
 * @author Emmanuel Bernard
 */
public class CreateKeySecondPass implements SecondPass {
	private RootClass rootClass;
	private JoinedSubclass joinedSubClass;

	public CreateKeySecondPass(RootClass rootClass) {
		this.rootClass = rootClass;
	}

	public CreateKeySecondPass(JoinedSubclass joinedSubClass) {
		this.joinedSubClass = joinedSubClass;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		if ( rootClass != null ) {
			rootClass.createPrimaryKey();
		}
		else if ( joinedSubClass != null ) {
			joinedSubClass.createPrimaryKey();
			joinedSubClass.createForeignKey();
		}
		else {
			throw new AssertionError( "rootClass and joinedSubClass are null" );
		}
	}
}
