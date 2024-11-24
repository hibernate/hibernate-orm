package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.derived;

import javax.persistence.Entity;

import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.crosspackage.base.BaseEntity;

@Entity
public class TestEntity extends BaseEntity {

}
