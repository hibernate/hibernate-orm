package org.hibernate.test.bytecode.enhancement.association.inherited;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue( "child" )
public class ChildItem extends Item {
}
