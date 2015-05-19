/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import java.util.LinkedHashSet;

/**
 * A specialization of the set type, with (resultset-based) ordering.
 */
public class OrderedSetType extends SetType {

	/**
	 * Constructs a set type capable of creating ordered sets of the given
	 * role.
	 *
	 * @param typeScope The scope for this type instance.
	 * @param role The collection role name.
	 * @param propertyRef The property ref name.
	 * @param isEmbeddedInXML Is this collection to embed itself in xml
	 *
	 * @deprecated Use {@link #OrderedSetType(org.hibernate.type.TypeFactory.TypeScope, String, String)}
	 * instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public OrderedSetType(TypeFactory.TypeScope typeScope, String role, String propertyRef, boolean isEmbeddedInXML) {
		super( typeScope, role, propertyRef, isEmbeddedInXML );
	}

	public OrderedSetType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		super( typeScope, role, propertyRef );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize > 0
				? new LinkedHashSet( anticipatedSize )
				: new LinkedHashSet();
	}

}
