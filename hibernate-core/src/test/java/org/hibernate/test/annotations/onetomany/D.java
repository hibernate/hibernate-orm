package org.hibernate.test.annotations.onetomany;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.List;

@Entity
public class D {
    @Id
    Long id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "a_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    List<E> listOfEs;
}