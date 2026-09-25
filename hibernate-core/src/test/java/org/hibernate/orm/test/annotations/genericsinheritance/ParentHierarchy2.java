package org.hibernate.orm.test.annotations.genericsinheritance;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class ParentHierarchy2<C extends ChildHierarchy2> extends Parent<C> {

}
