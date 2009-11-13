/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.cache.infinispan.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.infinispan.remoting.transport.Address;

/**
 * AddressAdapterImpl.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class AddressAdapterImpl implements AddressAdapter, Externalizable {

   private Address address;

   private AddressAdapterImpl(Address address) {
      this.address = address;
   }

   static AddressAdapter newInstance(Address address) {
      return new AddressAdapterImpl(address);
   }

   public static List<AddressAdapter> toAddressAdapter(List<Address> ispnAddresses) {
      List<AddressAdapter> addresses = new ArrayList<AddressAdapter>(ispnAddresses.size());
      for (Address address : ispnAddresses) {
         addresses.add(AddressAdapterImpl.newInstance(address));
      }
      return addresses;
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      address = (Address) in.readObject();
   }

   public void writeExternal(ObjectOutput out) throws IOException {
      out.writeObject(address);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this)
         return true;
      if (!(obj instanceof AddressAdapterImpl))
         return false;
      AddressAdapterImpl other = (AddressAdapterImpl) obj;
      return other.address.equals(address);
   }

   @Override
   public int hashCode() {
      int result = 17;
      result = 31 * result + address.hashCode();
      return result;
   }
}
