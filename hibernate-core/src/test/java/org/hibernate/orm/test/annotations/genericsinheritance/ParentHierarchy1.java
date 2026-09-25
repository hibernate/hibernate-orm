package org.hibernate.orm.test.annotations.genericsinheritance;
import jakarta.persistence.Entity;

@Entity
public class ParentHierarchy1 extends Parent<ChildHierarchy1> {

}
