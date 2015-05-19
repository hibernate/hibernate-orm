/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;


/**
 * Models a property which does not actually exist in the model.  It is created by Hibernate during
 * the metamodel binding process. 
 *
 * @author Steve Ebersole
 */
public class SyntheticProperty extends Property {
	@Override
	public boolean isSynthetic() {
		return true;
	}
}
