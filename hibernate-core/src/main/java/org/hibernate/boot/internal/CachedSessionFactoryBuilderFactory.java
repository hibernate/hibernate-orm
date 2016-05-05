/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package org.hibernate.boot.internal;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderFactory;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;

/**
 * @author kedzie
 * @see CachedSessionFactoryBuilder
 */
public class CachedSessionFactoryBuilderFactory implements SessionFactoryBuilderFactory {

   @Override
   public SessionFactoryBuilder getSessionFactoryBuilder(MetadataImplementor metadata, SessionFactoryBuilderImplementor defaultBuilder) {
      return new CachedSessionFactoryBuilder( defaultBuilder, metadata.getMetadataBuildingOptions().getServiceRegistry());
   }
}
