package org.hibernate.test.collection.original;


public class Animal {
    long id;
    String name;
    boolean boolvar;
    Zoo zoo;

    public long getId() {
        return id;
    }
    public void setId( long id ) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName( String name ) {
        this.name = name;
    }
    public boolean isBoolvar() {
		return boolvar;
	}
	public void setBoolvar(boolean boolvar) {
		this.boolvar = boolvar;
	}
	public Zoo getZoo() {
		return zoo;
	}
	public void setZoo(Zoo zoo) {
		this.zoo = zoo;
	}
}
