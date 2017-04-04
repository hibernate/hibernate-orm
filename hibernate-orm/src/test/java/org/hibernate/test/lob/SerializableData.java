/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: SerializableData.java 4704 2004-11-04 21:59:22Z steveebersole $
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
