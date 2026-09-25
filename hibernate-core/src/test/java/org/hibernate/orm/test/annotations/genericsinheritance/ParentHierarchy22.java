package org.hibernate.orm.test.annotations.genericsinheritance;
import jakarta.persistence.Entity;

@Entity
public class ParentHierarchy22 extends ParentHierarchy2<ChildHierarchy22> {

}
