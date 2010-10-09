/*
 * Created on 20-Dec-2004
 *
 */
package org.hibernate.test.legacy;

import java.io.Serializable;

/**
 * @author max
 *
 */
public class CompositeIdId implements Serializable {

    String system;
    String id;
    String name;
    CompositeElement composite;
    
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getSystem() {
        return system;
    }
    public void setSystem(String system) {
        this.system = system;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        // not totally NP or type safe equals, but enough for the unittests. 
        CompositeIdId o = (CompositeIdId) obj;
        if(o==null) return false;
        return o.getSystem().equals(getSystem()) && o.getId().equals(getId());
    }
    public CompositeElement getComposite() {
        return composite;
    }
    public void setComposite(CompositeElement composite) {
        this.composite = composite;
    }
}
