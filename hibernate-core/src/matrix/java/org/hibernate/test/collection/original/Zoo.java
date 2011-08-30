package org.hibernate.test.collection.original;
import java.util.ArrayList;
import java.util.List;

public class Zoo {
    long id;
    List animals = new ArrayList();

    public long getId() {
        return id;
    }
    public void setId( long id ) {
        this.id = id;
    }
	public List getAnimals() {
		return animals;
	}
	public void setAnimals(List animals) {
		this.animals = animals;
	}
    
    
}
