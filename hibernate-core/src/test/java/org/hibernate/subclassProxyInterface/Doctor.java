/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.subclassProxyInterface;


/**
 * @author Steve Ebersole
 */
public class Doctor extends Person implements IDoctor {
	public Doctor() {
	}

	public Doctor(String name) {
		super( name );
	}

    public String operate() {
        return "Dr. " + getName() + " is in";
    }
}
