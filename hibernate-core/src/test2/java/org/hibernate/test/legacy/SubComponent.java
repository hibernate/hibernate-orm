/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: SubComponent.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;

/**
 * Sub component
 * 
 * @author emmanuel
 */
public class SubComponent {
    private String _subName;
    
    private String _subName1;
    
    /**
     * @return
     */
    public String getSubName() {
        return _subName;
    }

    /**
     * @param string
     */
    public void setSubName(String string) {
        _subName = string;
    }

    /**
     * @return
     */
    public String getSubName1() {
        return _subName1;
    }

    /**
     * @param string
     */
    public void setSubName1(String string) {
        _subName1 = string;
    }

}
