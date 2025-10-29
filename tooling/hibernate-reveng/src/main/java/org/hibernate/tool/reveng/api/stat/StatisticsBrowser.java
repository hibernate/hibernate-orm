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
package org.hibernate.tool.reveng.api.stat;

import org.hibernate.stat.Statistics;
import org.hibernate.tool.reveng.internal.stat.StatisticsCellRenderer;
import org.hibernate.tool.reveng.internal.stat.StatisticsTreeModel;
import org.hibernate.tool.reveng.internal.util.BeanTableModel;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.beans.PropertyDescriptor;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Very rudimentary statistics browser.
 * 
 * Usage:
 * new StatisticsBrowser().showStatistics(getSessions().getStatistics(), shouldBlock);
 * 
 * @author max
 *
 */
public class StatisticsBrowser {
	
	/**
	 * 
	 * @param stats a Statistics instance obtained from a SessionFactory
	 * @param shouldBlock decides if the ui will be modal or not.
	 */
	public void showStatistics(Statistics stats, boolean shouldBlock) {
		
		JDialog main = new JDialog((JFrame)null, "Statistics browser");
		
		main.getContentPane().setLayout(new BorderLayout());
		
		final StatisticsTreeModel statisticsTreeModel = new StatisticsTreeModel(stats);
		JTree tree = new JTree(statisticsTreeModel);
		tree.setCellRenderer( new StatisticsCellRenderer() );
		ToolTipManager.sharedInstance().registerComponent(tree);
		
		JScrollPane treePane = new JScrollPane(tree);
		
		final JTable table = new StatisticsBrowserTable();
		
		JScrollPane tablePane = new JScrollPane(table);
		tablePane.getViewport().setBackground( table.getBackground() );
		final BeanTableModel beanTableModel = new BeanTableModel(Collections.<PropertyDescriptor>emptyList(), Object.class);
		table.setModel( beanTableModel );
		
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePane, tablePane);
		pane.setContinuousLayout( true );
		
		main.getContentPane().add(pane, BorderLayout.CENTER);
		
		tree.addTreeSelectionListener( new TreeSelectionListener() {
		
			public void valueChanged(TreeSelectionEvent e) {
				Object lastPathComponent = e.getPath().getLastPathComponent();
				List<PropertyDescriptor> l = new ArrayList<PropertyDescriptor>();
				if(statisticsTreeModel.isContainer( lastPathComponent )) {
					int childCount = statisticsTreeModel.getChildCount( lastPathComponent );
					
					Class<?> cl = Object.class;
					for (int i = 0; i < childCount; i++) {
						Object v = statisticsTreeModel.getChild( lastPathComponent, i );
						if(v!=null) cl = v.getClass();
						l.add((PropertyDescriptor)v);
					}
					table.setModel( new BeanTableModel(l, cl) );	
				} else {
					l.add((PropertyDescriptor) lastPathComponent );
					table.setModel( new BeanTableModel(l, lastPathComponent.getClass()) );
				}
				
				//table.doLayout();
				
			}
		
		} );
		
		
		main.getContentPane().setSize(new Dimension(640,480));
		main.pack();
		main.setModal(shouldBlock);
		main.setVisible(true);
	}
	
	final static class StatisticsBrowserTable extends JTable {
		
		private static final long serialVersionUID = 
				ObjectStreamClass.lookup(StatisticsBrowserTable.class).getSerialVersionUID();		

		public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
			TableCellRenderer defaultRenderer = 
					super.getDefaultRenderer( columnClass );		
			if(defaultRenderer==null) {
				return super.getDefaultRenderer( Object.class );
			} else {
				return defaultRenderer;
			}
		}
	}
	
	

}
