/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.stat;

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.hibernate.stat.CollectionStatistics;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;

@SuppressWarnings("serial")
public class StatisticsCellRenderer extends DefaultTreeCellRenderer {

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		
		JLabel treeCellRendererComponent = (JLabel) super.getTreeCellRendererComponent( tree, value, selected, expanded, leaf, row, hasFocus );
		
		String text = treeCellRendererComponent.getText();
		String tooltip = null;
		if(value instanceof Statistics) {
			Statistics stats = (Statistics) value;
			text = "Statistics " + formatter.format( new Date(stats.getStart().toEpochMilli()) );
			tooltip = stats.toString();
		}
		
		if(value instanceof EntityStatistics) {
			//EntityStatistics stats = (EntityStatistics) value;
			
		}
		
		if(value instanceof CollectionStatistics) {
			//CollectionStatistics stats = (CollectionStatistics) value;
			
		}
		
		if(value instanceof QueryStatistics) {
			//QueryStatistics stats = (QueryStatistics) value;
		
		}
		
		treeCellRendererComponent.setText( text );
		treeCellRendererComponent.setToolTipText( tooltip );
		return treeCellRendererComponent;
	}

	
}
