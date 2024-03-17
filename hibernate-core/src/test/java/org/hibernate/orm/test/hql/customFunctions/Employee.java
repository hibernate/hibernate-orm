/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql.customFunctions;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;


//tag::hql-examples-domain-model-example[]
@Entity
public class Employee {
    @Id
    private Long id;
    private Long salary;
    private String name;
    private String surname;

    public Employee(Long id, Long salary, String name, String surname) {
        this.id = id;
        this.salary = salary;
        this.name = name;
        this.surname = surname;
    }

    //Getters and setters are omitted for brevity

//end::hql-examples-domain-model-example[]

    public Employee() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSalary() {
        return salary;
    }

    public void setSalary(Long salary) {
        this.salary = salary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }
    //tag::hql-examples-domain-model-example[]
}
//end::hql-examples-domain-model-example[]