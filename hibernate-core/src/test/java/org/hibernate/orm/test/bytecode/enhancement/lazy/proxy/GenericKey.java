package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@Entity(name="GenericKey")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class GenericKey extends AbstractKey implements Serializable {

}
