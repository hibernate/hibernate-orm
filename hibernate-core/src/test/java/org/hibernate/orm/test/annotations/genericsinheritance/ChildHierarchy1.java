package org.hibernate.orm.test.annotations.genericsinheritance;
import jakarta.persistence.Entity;

@Entity
public class ChildHierarchy1 extends Child<ParentHierarchy1> {

}
