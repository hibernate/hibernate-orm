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

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;
import org.hibernate.tool.reveng.internal.export.hbm.Cfg2HbmTool;
import org.hibernate.tool.reveng.internal.export.hbm.HBMTagForValueVisitor;
import org.hibernate.tool.orm.jbt.api.wrp.Cfg2HbmToolWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.PersistentClassWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.PropertyWrapper;
import org.hibernate.tool.orm.jbt.api.wrp.Wrapper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class Cfg2HbmToolWrapperFactory {

	public static Cfg2HbmToolWrapper createCfg2HbmToolWrapper() {
		return new Cfg2HbmToolWrapperImpl();
	}
	
	private static class Cfg2HbmToolWrapperImpl 
			extends AbstractWrapper
			implements Cfg2HbmToolWrapper {
		
		private Cfg2HbmTool wrappedCfg2HbmTool = new Cfg2HbmTool();
		
		@Override 
		public Cfg2HbmTool getWrappedObject() { 
			return wrappedCfg2HbmTool; 
		}

		public String getTag(PersistentClassWrapper pcw) {
			return wrappedCfg2HbmTool.getTag((PersistentClass)pcw.getWrappedObject());
		}
		
		public String getTag(PropertyWrapper pw) {
			PersistentClassWrapper persistentClassWrapper = pw.getPersistentClass();
			if(persistentClassWrapper!=null) {
				Property v = (Property)persistentClassWrapper.getVersion().getWrappedObject();
				if(v==pw.getWrappedObject()) {
					Value pwv = (Value)pw.getValue().getWrappedObject();
					if (pwv instanceof Wrapper) {
						pwv = (Value)((Wrapper)pwv).getWrappedObject();
					}
					String typeName = ((SimpleValue)pwv).getTypeName();
					if("timestamp".equals(typeName) || "dbtimestamp".equals(typeName)) {
						return "timestamp";
					} else {
						return "version";
					}
				}
			}
			String toolTag = (String)((Value)pw.getValue().getWrappedObject()).accept(HBMTagForValueVisitor.INSTANCE);
			if ("component".equals(toolTag) && "embedded".equals(pw.getPropertyAccessorName())){
				toolTag = "properties";
			}
			return toolTag;
		}
		
	}

}
