//$
package org.hibernate.test.annotations.collectionelement.recreate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Sergey Astakhov
 */
@Entity
@GenericGenerator( name = "increment", strategy = "increment" )
public class Poi
{
  @Id
  @GeneratedValue
  private Integer id;

  private String name;

  public Poi()
  {
  }

  public Poi(String _name)
  {
    name = _name;
  }

  public Integer getId()
  {
    return id;
  }

  public void setId(Integer id)
  {
    this.id = id;
  }

  public String getName()
  {
    return name;
  }

  public void setName(String _name)
  {
    name = _name;
  }
}
