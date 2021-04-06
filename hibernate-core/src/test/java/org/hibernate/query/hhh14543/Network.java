package org.hibernate.query.hhh14543;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Set;

@Entity(name = "Network")
@Table(name = "Network")
public class Network implements Serializable {
  @Id
  private Long id;

  @ManyToMany(mappedBy = "networks")
  private Set<CategorySet> categorySets;
}
