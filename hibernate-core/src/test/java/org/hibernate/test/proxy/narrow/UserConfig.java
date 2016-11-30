package org.hibernate.test.proxy.narrow;


/**
 * @author jlandin
 */
public class UserConfig
{
  private Long id;
  private User user;
  
  /**
   * Constructs a new UserConfig.
   */
  public UserConfig()
  {
    super();
  }

  /**
   * Retrieves the value of the id property.
   * @return The value of the id property.
   */
  public Long getId()
  {
    return this.id;
  }

  /**
   * Sets the value of the id property. 
   * @param id The value to set for the property id.
   */
  public void setId(Long id)
  {
    this.id = id;
  }

  /**
   * Retrieves the value of the user property.
   * @return The value of the user property.
   */
  public User getUser()
  {
    return user;
  }

  /**
   * Sets the value of the user property. 
   * @param user The value to set for the property user.
   */
  public void setUser(User user)
  {
    this.user = user;
  }

}
