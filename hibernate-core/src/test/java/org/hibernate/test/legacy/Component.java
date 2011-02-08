//$Id: Component.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;


/**
 * Component
 * 
 * @author Emmanuel Bernard
 */
public class Component {
    private String _name;
    
    private SubComponent _subComponent;

    /**
     * @return
     */
    public String getName() {
        return _name;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        _name = string;
    }

    /**
     * @return
     */
    public SubComponent getSubComponent() {
        return _subComponent;
    }

    /**
     * @param component
     */
    public void setSubComponent(SubComponent component) {
        _subComponent = component;
    }

}
