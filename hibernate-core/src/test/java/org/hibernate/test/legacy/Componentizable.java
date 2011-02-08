//$Id: Componentizable.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;


/**
 * contains components
 * 
 * @author emmanuel
 */
public class Componentizable {
	/** surrogate id */
	private Integer _id;
    
    public String _nickName;
	
	/** component */
    private Component _component;

    /**
     * @return
     */
    public Integer getId() {
        return _id;
    }

    /**
     * @param integer
     */
    public void setId(Integer integer) {
        _id = integer;
    }

    /**
     * @return
     */
    public Component getComponent() {
        return _component;
    }

    /**
     * @param component
     */
    public void setComponent(Component component) {
        _component = component;
    }

    /**
     * @return
     */
    public String getNickName() {
        return _nickName;
    }

    /**
     * @param string
     */
    public void setNickName(String string) {
        _nickName = string;
    }

}
