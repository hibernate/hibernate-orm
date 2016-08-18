package org.hibernate.test.bytecode.enhancement.association.inherited;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "type", discriminatorType = DiscriminatorType.STRING )
public abstract class Item {

    @Id
    @GeneratedValue
    Long id;

    @ManyToOne( fetch = FetchType.LAZY )
    Author author;
}
