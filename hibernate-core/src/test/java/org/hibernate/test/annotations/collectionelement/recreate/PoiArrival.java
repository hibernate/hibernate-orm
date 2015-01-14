package org.hibernate.test.annotations.collectionelement.recreate;

import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * @author Sergey Astakhov
 */
@Embeddable
public class PoiArrival
{
  @Temporal( TemporalType.TIMESTAMP )
  private Date expectedTime;

  @Temporal( TemporalType.TIMESTAMP )
  private Date arriveTime;

  public Date getExpectedTime()
  {
    return expectedTime;
  }

  public void setExpectedTime(Date _expectedTime)
  {
    expectedTime = _expectedTime;
  }

  public Date getArriveTime()
  {
    return arriveTime;
  }

  public void setArriveTime(Date _arriveTime)
  {
    arriveTime = _arriveTime;
  }
}
