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
public class PersonPhoneCount {

    private final String name;

    private final Number phoneCount;

    public PersonPhoneCount(String name, Number phoneCount) {
        this.name = name;
        this.phoneCount = phoneCount;
    }

    public String getName() {
        return name;
    }

    public Number getPhoneCount() {
        return phoneCount;
    }
}
