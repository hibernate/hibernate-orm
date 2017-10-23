/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
