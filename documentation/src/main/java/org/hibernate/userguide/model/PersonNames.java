/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.model;

/**
 * @author Vlad Mihalcea
 */
//tag::sql-ConstructorResult-dto-example[]
public class PersonNames {

    private final String name;

    private final String nickName;

    public PersonNames(String name, String nickName) {
        this.name = name;
        this.nickName = nickName;
    }

    public String getName() {
        return name;
    }

    public String getNickName() {
        return nickName;
    }
}
//end::sql-ConstructorResult-dto-example[]
