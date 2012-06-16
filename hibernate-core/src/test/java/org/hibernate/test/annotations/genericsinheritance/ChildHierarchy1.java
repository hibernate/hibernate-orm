package org.hibernate.test.annotations.genericsinheritance;
import javax.persistence.Entity;

@Entity
public class ChildHierarchy1 extends Child<ParentHierarchy1> {

}
