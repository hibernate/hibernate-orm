/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.idmanytoone;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Alex Kalashnikov
 */
@Entity
@Table(name = "idmanytoone_course")
public class Course implements Serializable {

    @Id
    @GeneratedValue
    private int id;

    private String name;

    @OneToMany(mappedBy = "course")
    private Set<CourseStudent> students;

    public Course() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<CourseStudent> getStudents() {
        return students;
    }

    public void setStudents(Set<CourseStudent> students) {
        this.students = students;
    }
}
