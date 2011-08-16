package org.hibernate.test.criteria;

/**
 * @author David Mansfield
 */
public class Country {
    String code;
    String name;

    public Country() {}

    public Country(String code, String name) {
	this.code = code;
	this.name = name;
    }

    public String getCode() {
	return code;
    }

    public void setCode(String code) {
	this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
