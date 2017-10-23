/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria.limitexpression;

import java.util.Set;

public class Person {
    private Long id;
    private Set<UsState> states;
    private Set<Country> countries;
    
    public Set<UsState> getStates() {
        return states;
    }

    public void setStates(Set<UsState> states) {
        this.states = states;
    }

    public Set<Country> getCountries() {
        return countries;
    }

    public void setCountries(Set<Country> countries) {
        this.countries = countries;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    
}
