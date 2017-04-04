/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name="employee")
public class Employee implements Serializable  {

    @Id
    @GeneratedValue
    @Column(name="id_emp")
    private Integer id;
    
    private String firstName;
    private String lastName;
    
    @OneToOne
    @JoinColumn(name="id_title")
    private Title title;
    
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="id_depto")
    private Department department;
    
    public Employee() {}
    
    public Employee(Integer _id, String _lastName, Integer _idTitle, String _descriptionTitle, Department _dept, String _fname) {
        setId(_id);
        setLastName(_lastName);
        Title _title = new Title();
        _title.setId(_idTitle);
        _title.setDescription(_descriptionTitle);
        setTitle(_title);
        setDepartment(_dept);
        setFirstName(_fname);
    }
    
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public Title getTitle() {
        return title;
    }
    public void setTitle(Title title) {
        this.title = title;
    }
    public Department getDepartment() {
        return department;
    }
    public void setDepartment(Department department) {
        this.department = department;
    }
    
    
    
    
    
}
