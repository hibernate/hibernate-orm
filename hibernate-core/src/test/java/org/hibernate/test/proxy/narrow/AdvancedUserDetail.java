package org.hibernate.test.proxy.narrow;

/**
 * @author jlandin
 */
public class AdvancedUserDetail
{
  private Long id;
  private AdvancedUser advancedUser;
  private String statusCode;
  
  /**
   * Constructs a new AdvancedUserDetail.
   */
  public AdvancedUserDetail()
  {
    super();
  }

  /**
   * Retrieves the value of the advancedUser property.
   * @return The value of the advancedUser property.
   */
  public AdvancedUser getAdvancedUser()
  {
    return advancedUser;
  }

  /**
   * Sets the value of the advancedUser property. 
   * @param advancedUser The value to set for the property advancedUser.
   */
  public void setAdvancedUser(AdvancedUser advancedUser)
  {
    this.advancedUser = advancedUser;
  }

  /**
   * Retrieves the value of the statusCode property.
   * @return The value of the statusCode property.
   */
  public String getStatusCode()
  {
    return statusCode;
  }

  /**
   * Sets the value of the statusCode property. 
   * @param statusCode The value to set for the property statusCode.
   */
  public void setStatusCode(String statusCode)
  {
    this.statusCode = statusCode;
  }

  /**
   * Retrieves the value of the id property.
   * @return The value of the id property.
   */
  public Long getId()
  {
    return id;
  }

  /**
   * Sets the value of the id property. 
   * @param id The value to set for the property id.
   */
  public void setId(Long id)
  {
    this.id = id;
  }

}
