/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 * 
 * Copyright 2004-2020 Red Hat, Inc.
 *
 * Licensed under the GNU Lesser General Public License (LGPL), 
 * version 2.1 or later (the "License").
 * You may not use this file except in compliance with the License.
 * You may read the licence in the 'lgpl.txt' file in the root folder of 
 * project or obtain a copy at
 *
 *     http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;

public class Hbm2CfgXmlExporterTask extends ExporterTask {

	private boolean ejb3;

	public Hbm2CfgXmlExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public Exporter createExporter() {
		return ExporterFactory.createExporter(ExporterType.CFG);
	}

	public void setEjb3(boolean ejb3) {
		this.ejb3 = ejb3;
	}
	
	public String getName() {
		return "hbm2cfgxml (Generates hibernate.cfg.xml)";
	}
	
	protected Exporter configureExporter(Exporter exporter) {
		super.configureExporter( exporter );
        exporter.getProperties().setProperty("ejb3", ""+ejb3);
		return exporter;
	}

}
