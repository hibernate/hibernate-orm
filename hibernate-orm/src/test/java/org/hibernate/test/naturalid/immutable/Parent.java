/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.immutable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alex Burgel
 */
public class Parent {
	
	private Long id;
	private String name;
	private List children = new ArrayList();

	Parent() {}

	public Parent(String name) {
		this.name = name;
	}

    public void setName(String name) {
        this.name = name;
    }

    public List getChildren() {
        return children;
    }

    public void setChildren(List children) {
        this.children = children;
    }

    
}
