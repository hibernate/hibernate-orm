package org.hibernate.action;

import java.io.Serializable;
import java.util.*;

/**
 * Encapsulates the actions for an Executable list, and caches the property spaces for the actions.
 * <p>
 * Executable lists don't usually shrink - they grow and then get cleared. We can take advantage of this by caching
 * the property spaces for these actions.
 * </p>
 */
public class ExecutableList<E extends Executable> extends AbstractList<E> implements Cloneable, Serializable {

    private Set propertySpaces;

    private List<E> executables;

    public ExecutableList() {
        executables = new ArrayList<E>();
        propertySpaces = null;
    }

    public E set(int index, E e) {
        E old = executables.set(index, e);
        // TODO: we don't have to null this field if the old and new elements share the same property spaces
        propertySpaces = null;
        return old;
    }

    @Override
    public E get(int index) {
        return executables.get(index);
    }

    public ExecutableList(int initialCapacity) {
        executables = new ArrayList<E>(initialCapacity);
    }

    public E remove(int index) {
        propertySpaces = null;
        return executables.remove(index);
    }

    public void add(int index, E e) {
        executables.add(index, e);
        if(propertySpaces != null) {
            Serializable[] spaces = e.getPropertySpaces();
            for(int i = 0; i < spaces.length; i++) {
                propertySpaces.add(spaces[i]);
            }
        }
    }

    public Set getPropertySpaces() {
        if(propertySpaces == null) {
            propertySpaces = new HashSet();
            for(Executable e : executables) {
                Serializable[] spaces = e.getPropertySpaces();
                for(int i = 0; i < spaces.length; i++) {
                    propertySpaces.add(spaces[i]);
                }
            }
        }
        return Collections.unmodifiableSet(propertySpaces);
    }

    public int size() {
        return executables.size();
    }

    public void clear() {
        if(propertySpaces != null) {
            propertySpaces.clear();
        }
        executables.clear();
    }

    public Object clone() {
    	try {
	        ExecutableList<E> v = (ExecutableList<E>) super.clone();
	        v.executables = new ArrayList<E>(executables);
	        return v;
	    } catch (CloneNotSupportedException e) {
	        // this shouldn't happen, since we are Cloneable
	        throw new InternalError();
    	}
    }

}
