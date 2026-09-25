package org.hibernate.testing.orm.domain.animal;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue( "P" )
public class PettingZoo extends Zoo {

}
