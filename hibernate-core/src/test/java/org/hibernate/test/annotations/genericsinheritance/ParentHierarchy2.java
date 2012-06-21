package org.hibernate.test.annotations.genericsinheritance;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class ParentHierarchy2<C extends ChildHierarchy2> extends Parent<C> {

}
