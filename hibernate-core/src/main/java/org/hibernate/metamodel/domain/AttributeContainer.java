/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.domain;

import java.util.Set;

/**
 * Basic contract for any container holding attributes. This allows polymorphic handling of both
 * components and entities in terms of the attributes they hold.
 *
 * @author Steve Ebersole
 */
public interface AttributeContainer extends Type {
	/**
	 * Obtain the name of this container in terms of creating attribute role names.
	 * <p/>
	 * NOTE : A role uniquely names each attribute.  The role name is the name of the attribute prefixed by the "path"
	 * to its container.
	 *
	 * @return The container base name for role construction.
	 */
	public String getRoleBaseName();

	/**
	 * Retrieve an attribute by name.
	 *
	 * @param name The name of the attribute to retrieve.
	 *
	 * @return The attribute matching the given name, or null.
	 */
	public Attribute locateAttribute(String name);

	/**
	 * Retrieve the attributes contained in this container.
	 *
	 * @return The contained attributes
	 */
	public Set<Attribute> attributes();

	public SingularAttribute locateSingularAttribute(String name);
	public SingularAttribute createSingularAttribute(String name);
	public SingularAttribute createVirtualSingularAttribute(String name);

	public SingularAttribute locateComponentAttribute(String name);
	public SingularAttribute createComponentAttribute(String name, Component component);

	public PluralAttribute locatePluralAttribute(String name);

	public PluralAttribute locateBag(String name);
	public PluralAttribute createBag(String name);

	public PluralAttribute locateSet(String name);
	public PluralAttribute createSet(String name);

	public IndexedPluralAttribute locateList(String name);
	public IndexedPluralAttribute createList(String name);

	public IndexedPluralAttribute locateMap(String name);
	public IndexedPluralAttribute createMap(String name);

}
