/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
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
package org.hibernate.tool.orm.jbt.internal.factory;

import org.hibernate.tool.reveng.internal.reveng.strategy.TableFilter;
import org.hibernate.tool.orm.jbt.api.wrp.TableFilterWrapper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class TableFilterWrapperFactory {
	
	public static TableFilterWrapper createTableFilterWrapper() {
		return createTableFilterWrapper(new TableFilter());
	}

	private static TableFilterWrapper createTableFilterWrapper(TableFilter wrappedTableFilter) {
		return new TableFilterWrapperImpl(wrappedTableFilter);
	}
	
	private static class TableFilterWrapperImpl 
			extends AbstractWrapper
			implements TableFilterWrapper {
		
		private TableFilter  tableFilter = null;
		
		private TableFilterWrapperImpl(TableFilter tableFilter) {
			this.tableFilter = tableFilter;
		}
		
		@Override 
		public TableFilter getWrappedObject() { 
			return tableFilter; 
		}
		
		@Override 
		public void setExclude(boolean b) { 
			tableFilter.setExclude(b); 
		}

		@Override 
		public void setMatchCatalog(String s) { 
			tableFilter.setMatchCatalog(s); 
		}

		@Override 
		public void setMatchSchema(String s) { 
			tableFilter.setMatchSchema(s); 
		}

		@Override 
		public void setMatchName(String s) { 
			tableFilter.setMatchName(s); 
		}

		@Override 
		public Boolean getExclude() { 
			return tableFilter.getExclude(); 
		}

		@Override 
		public String getMatchCatalog() { 
			return tableFilter.getMatchCatalog(); 
		}

		@Override 
		public String getMatchSchema() { 
			return tableFilter.getMatchSchema(); 
		}

		@Override 
		public String getMatchName() { 
			return tableFilter.getMatchName(); 
		}

	}

}
