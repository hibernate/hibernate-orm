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
public class ExecutableList extends AbstractList implements Cloneable, Serializable {

    private Set propertySpaces;

    private List executables;

    public ExecutableList() {
        executables = new ArrayList();
        propertySpaces = null;
    }

    public Object set(int index, Object e) {
        Object old = executables.set(index, e);
        // TODO: we don't have to null this field if the old and new elements share the same property spaces
        propertySpaces = null;
        return old;
    }

    public Object get(int index) {
        return executables.get(index);
    }

    public ExecutableList(int initialCapacity) {
        executables = new ArrayList(initialCapacity);
    }

    public Object remove(int index) {
        propertySpaces = null;
        return executables.remove(index);
    }

    public void add(int index, Object e) {
        executables.add(index, e);
        if(propertySpaces != null) {
            Serializable[] spaces = ((Executable) e).getPropertySpaces();
            for(int i = 0; i < spaces.length; i++) {
                propertySpaces.add(spaces[i]);
            }
        }
    }

    public Set getPropertySpaces() {
        if(propertySpaces == null) {
            propertySpaces = new HashSet();
            for(Iterator iter = executables.iterator(); iter.hasNext();) {
                Executable e = (Executable) iter.next();
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
	        ExecutableList v = (ExecutableList) super.clone();
	        v.executables = new ArrayList(executables);
	        return v;
	    } catch (CloneNotSupportedException e) {
	        // this shouldn't happen, since we are Cloneable
	        throw new InternalError();
    	}
    }

}
