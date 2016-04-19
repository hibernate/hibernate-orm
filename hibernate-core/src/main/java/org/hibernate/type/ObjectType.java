/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;


/**
 * Specific adaptation of the "any" type to the old deprecated "object" type
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ObjectType extends AnyType implements BasicType {
	/**
	 * Singleton access
	 */
	public static final ObjectType INSTANCE = new ObjectType();

	private ObjectType() {
		super( StringType.INSTANCE, SerializableType.INSTANCE );
	}

	@Override
	public String getName() {
		return "object";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), Object.class.getName() };
	}
}
