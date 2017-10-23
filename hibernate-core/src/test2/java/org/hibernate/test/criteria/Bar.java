/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;


public class Bar {
    private Integer id;

    AbstractFoo myFoo;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AbstractFoo getMyFoo() {
        return myFoo;
    }

    public void setMyFoo(AbstractFoo myFoo) {
        this.myFoo = myFoo;
    }
}
