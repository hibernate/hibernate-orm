package org.hibernate.test.annotations.onetomany;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class D {
    @Id
    Long id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "a_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    List<E> listOfEs;
}