package org.hibernate.mapping;

import java.util.*;
import java.util.List;

/**
 * Represents a @MappedSuperclass.
 * A @MappedSuperclass can be a superclass of an @Entity (root or not)
 *
 * This class primary goal is to give a representation to @MappedSuperclass
 * in the metamodel in order to reflect them in the JPA 2 metamodel.
 *
 * Do not use outside this use case.
 *
 * A proper redesign will be evluated in Hibernate 4
 *
 * @author Emmanuel Bernard
 */
public class MappedSuperclass {
	private final MappedSuperclass superMappedSuperclass;
	private final PersistentClass superPersistentClass;
	private final List properties;
	private Class mappedClass;

	public MappedSuperclass(MappedSuperclass superMappedSuperclass, PersistentClass superPersistentClass) {
		this.superMappedSuperclass = superMappedSuperclass;
		this.superPersistentClass = superPersistentClass;
		this.properties = new ArrayList();
	}

	/**
	 * Returns the first superclass marked as @MappedSuperclass or null if:
	 *  - none exists
	 *  - or the first persistent superclass found is an @Entity
	 *
	 * @return the super MappedSuperclass
	 */
	public MappedSuperclass getSuperMappedSuperclass() {
		return superMappedSuperclass;
	}

	/**
	 * Returns the PersistentClass of the first superclass marked as @Entity
	 * or null if none exists
	 *
	 * @return the PersistentClass of the superclass
	 */
	public PersistentClass getSuperPersistentClass() {
		return superPersistentClass;
	}

	public Iterator getPropertyIterator() {
		return properties.iterator();
	}

	public void addProperty(Property p) {
		//Do not add duplicate properties
		//TODO is it efficient enough?
		String name = p.getName();
		Iterator it = properties.iterator();
		while (it.hasNext()) {
			if ( name.equals( ((Property)it.next()).getName() ) ) {
				return;
			}
		}
		properties.add(p);
	}

	public Class getMappedClass() {
		return mappedClass;
	}

	public void setMappedClass(Class mappedClass) {
		this.mappedClass = mappedClass;
	}
}
