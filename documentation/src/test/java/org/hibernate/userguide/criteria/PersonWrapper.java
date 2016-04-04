/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.criteria;

/**
 * @author Vlad Mihalcea
 */
//tag::criteria-typedquery-wrapper-example[]
public class PersonWrapper {

    private final Long id;

    private final String nickName;

    public PersonWrapper(Long id, String nickName) {
        this.id = id;
        this.nickName = nickName;
    }

    public Long getId() {
        return id;
    }

    public String getNickName() {
        return nickName;
    }
}
//end::criteria-typedquery-wrapper-example[]

