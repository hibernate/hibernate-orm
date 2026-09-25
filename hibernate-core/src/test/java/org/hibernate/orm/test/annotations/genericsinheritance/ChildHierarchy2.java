package org.hibernate.orm.test.annotations.genericsinheritance;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class ChildHierarchy2<P extends ParentHierarchy2> extends Child<P> {

}
