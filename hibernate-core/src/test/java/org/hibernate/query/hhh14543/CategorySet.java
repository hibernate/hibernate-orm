package org.hibernate.query.hhh14543;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity(name = "CategorySet")
@Table(name = "CategorySet")
public class CategorySet implements Serializable {
  @Id
  private Long id;

  @ManyToMany
  @JoinTable
  private Set<Network> networks;

  @OneToMany(mappedBy = "categorySet")
  private Set<Category> categories;
}
