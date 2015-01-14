/**
 * $Id$
 *
 * Copyright (C) 2015 CSBI. All Rights Reserved
 */
package org.hibernate.test.annotations.collectionelement.recreate;

import java.util.Date;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Sergey Astakhov
 * @version $Revision$
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
