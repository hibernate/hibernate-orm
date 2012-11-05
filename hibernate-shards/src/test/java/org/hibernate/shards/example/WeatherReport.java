/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package org.hibernate.shards.example;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Model object for our example app
 *
 * @author maxr@google.com (Max Ross)
 */
public class WeatherReport {
  private BigInteger reportId;
  private String continent;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private int temperature;
  private Date reportTime;


  public BigInteger getReportId() {
    return reportId;
  }

  public void setReportId(BigInteger reportId) {
    this.reportId = reportId;
  }

  public String getContinent() {
    return continent;
  }

  public void setContinent(String continent) {
    this.continent = continent;
  }

  public BigDecimal getLatitude() {
    return latitude;
  }

  public void setLatitude(BigDecimal latitude) {
    this.latitude = latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
  }

  public void setLongitude(BigDecimal longitude) {
    this.longitude = longitude;
  }

  public int getTemperature() {
    return temperature;
  }

  public void setTemperature(int temperature) {
    this.temperature = temperature;
  }

  public Date getReportTime() {
    return reportTime;
  }

  public void setReportTime(Date reportTime) {
    this.reportTime = reportTime;
  }


  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WeatherReport that = (WeatherReport) o;

    return !(reportId != null ? !reportId.equals(that.reportId)
        : that.reportId != null);

  }

  public int hashCode() {
    return (reportId != null ? reportId.hashCode() : 0);
  }
}
