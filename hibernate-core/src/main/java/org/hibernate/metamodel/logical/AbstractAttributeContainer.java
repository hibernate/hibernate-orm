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
package org.hibernate.metamodel.logical;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Convenient base class for {@link AttributeContainer}.  Because in our model all
 * {@link AttributeContainer AttributeContainers} are also {@link Hierarchical} we also implement that here
 * as well.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeContainer implements AttributeContainer, Hierarchical  {
	private final String name;
	private final Hierarchical superType;
	private LinkedHashSet<Attribute> attributeSet = new LinkedHashSet<Attribute>();
	private HashMap<String,Attribute> attributeMap = new HashMap<String,Attribute>();

	public AbstractAttributeContainer(String name, Hierarchical superType) {
		this.name = name;
		this.superType = superType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Hierarchical getSuperType() {
		return superType;
	}

	@Override
	public Set<Attribute> getAttributes() {
		return Collections.unmodifiableSet( attributeSet );
	}

	@Override
	public Attribute getAttribute(String name) {
		return attributeMap.get( name );
	}

	public void addAttribute(Attribute attribute) {
		// todo : how to best "secure" this?
		if ( attributeMap.put( attribute.getName(), attribute ) != null ) {
			throw new IllegalArgumentException( "Attrtibute with name [" + attribute.getName() + "] already registered" );
		}
		attributeSet.add( attribute );
	}
}
