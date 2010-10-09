package org.hibernate.test.annotations.genericsinheritance;

import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class ChildHierarchy2<P extends ParentHierarchy2> extends Child<P> {

}
