/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.internal.annotations;

import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.spi.InLineViewSource;

import org.jboss.jandex.AnnotationInstance;

/**
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class InLineViewSourceImpl implements InLineViewSource {
	private final InlineViewInfo inlineViewInfo;

	public InLineViewSourceImpl(EntityTypeMetadata entityTypeMetadata) {
		this.inlineViewInfo = createInlineViewInfo( entityTypeMetadata );
	}

	@Override
	public String getSelectStatement() {
		return inlineViewInfo.getSelectStatement();
	}

	@Override
	public String getExplicitSchemaName() {
		return null;
	}

	@Override
	public String getExplicitCatalogName() {
		return null;
	}

	@Override
	public String getLogicalName() {
		return inlineViewInfo.getLogicalName();
	}

	private InlineViewInfo createInlineViewInfo(EntityTypeMetadata entityTypeMetadata) {
		final AnnotationInstance subselectAnnotation = entityTypeMetadata.getJavaTypeDescriptor().findTypeAnnotation(
				HibernateDotNames.SUB_SELECT
		);

		return new InlineViewInfo( subselectAnnotation.value().asString(), entityTypeMetadata.getEntityName() );
	}

	private static class InlineViewInfo {
		private final String selectStatement;
		private final String logicalName;

		private InlineViewInfo(String selectStatement, String logicalName) {
			this.selectStatement = selectStatement;
			this.logicalName = logicalName;
		}

		public String getSelectStatement() {
			return selectStatement;
		}

		public String getLogicalName() {
			return logicalName;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			InlineViewInfo that = ( InlineViewInfo ) o;

			if ( logicalName != null ? !logicalName.equals( that.logicalName ) : that.logicalName != null ) {
				return false;
			}
			if ( selectStatement != null ? !selectStatement.equals( that.selectStatement ) : that.selectStatement != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = selectStatement != null ? selectStatement.hashCode() : 0;
			result = 31 * result + ( logicalName != null ? logicalName.hashCode() : 0 );
			return result;
		}
	}
}
