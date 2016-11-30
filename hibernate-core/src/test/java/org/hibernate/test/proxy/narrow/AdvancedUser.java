package org.hibernate.test.proxy.narrow;

/**
 * @author jlandin
 */
public class AdvancedUser 
  extends User
{
  
  private AdvancedUserDetail currentDetail;

  /**
   * Constructs a new AdvancedUser.
   */
  public AdvancedUser()
  {
    super();
  }

  /**
   * Retrieves the value of the currentDetail property.
   * @return The value of the currentDetail property.
   */
  public AdvancedUserDetail getCurrentDetail()
  {
    return currentDetail;
  }

  /**
   * Sets the value of the currentDetail property. 
   * @param currentDetail The value to set for the property currentDetail.
   */
  public void setCurrentDetail(AdvancedUserDetail currentDetail)
  {
    this.currentDetail = currentDetail;
  }

}
