package org.hibernate.query.hhh14543;

import javax.persistence.*;
import java.io.Serializable;

@Entity(name = "Category")
@Table(name = "Category")
public class Category implements Serializable {
  @Id
  private Long id;

  @ManyToOne
  @JoinColumn
  private CategorySet categorySet;
}
