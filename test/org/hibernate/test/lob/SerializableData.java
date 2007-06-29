// $Id$
package org.hibernate.test.lob;

import java.io.Serializable;

/**
 * Implementation of SerializableData.
 *
 * @author Steve
 */
public class SerializableData implements Serializable
{
   private String payload;

   public SerializableData(String payload)
   {
      this.payload = payload;
   }

   public String getPayload()
   {
      return payload;
   }

   public void setPayload(String payload)
   {
      this.payload = payload;
   }
}
