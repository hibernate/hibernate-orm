/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
