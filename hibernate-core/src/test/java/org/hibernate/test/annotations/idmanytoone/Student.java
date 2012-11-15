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
@Table(name = "idmanytoone_student")
public class Student implements Serializable {

    @Id
    @GeneratedValue
    private int id;

    private String name;

    @OneToMany(mappedBy = "student")
    private Set<CourseStudent> courses;

    public Student() {
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

    public Set<CourseStudent> getCourses() {
        return courses;
    }

    public void setCourses(Set<CourseStudent> courses) {
        this.courses = courses;
    }
}
