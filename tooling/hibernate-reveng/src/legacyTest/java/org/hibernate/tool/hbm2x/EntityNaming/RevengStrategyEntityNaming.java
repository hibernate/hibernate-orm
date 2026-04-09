/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.hbm2x.EntityNaming;

import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.internal.reveng.strategy.DelegatingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Daren
 */
public class RevengStrategyEntityNaming extends DelegatingStrategy {

  private List<SchemaSelection> schemas;

  public RevengStrategyEntityNaming(RevengStrategy delegate) {
    super(delegate);
    this.schemas = new ArrayList<>();
    schemas.add(new SchemaSelection(){
      @Override
      public String getMatchCatalog() {
        /* no h2 pattern matching on catalog*/
        return "test1";
      }

      @Override
      public String getMatchSchema() {
        return "PUBLIC";
      }

      @Override
      public String getMatchTable() {
        return ".*";
      }
    });
  }

  public List<SchemaSelection> getSchemaSelections() {
    return schemas;
  }

    }
