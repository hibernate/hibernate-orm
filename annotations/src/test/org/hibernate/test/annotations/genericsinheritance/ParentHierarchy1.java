package org.hibernate.test.annotations.genericsinheritance;

import javax.persistence.Entity;

@Entity
public class ParentHierarchy1 extends Parent<ChildHierarchy1> {

}
