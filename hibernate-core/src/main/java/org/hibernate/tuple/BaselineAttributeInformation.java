/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.FetchMode;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.engine.spi.CascadeStyle;

/**
 * @deprecated No direct replacement, though see {@link org.hibernate.metamodel.mapping.AttributeMapping}
 * and {@link org.hibernate.metamodel.mapping.AttributeMetadata}
*/
@Deprecated(forRemoval = true)
public class BaselineAttributeInformation {
	private final boolean lazy;
	private final boolean insertable;
	private final boolean updateable;
	private final boolean nullable;
	private final boolean dirtyCheckable;
	private final boolean versionable;
	private final OnDeleteAction onDeleteAction;
	private final CascadeStyle cascadeStyle;
	private final FetchMode fetchMode;

	public BaselineAttributeInformation(
			boolean lazy,
			boolean insertable,
			boolean updateable,
			boolean nullable,
			boolean dirtyCheckable,
			boolean versionable,
			CascadeStyle cascadeStyle,
			OnDeleteAction onDeleteAction,
			FetchMode fetchMode) {
		this.lazy = lazy;
		this.insertable = insertable;
		this.updateable = updateable;
		this.nullable = nullable;
		this.dirtyCheckable = dirtyCheckable;
		this.versionable = versionable;
		this.cascadeStyle = cascadeStyle;
		this.onDeleteAction = onDeleteAction;
		this.fetchMode = fetchMode;
	}

	public boolean isLazy() {
		return lazy;
	}

	public boolean isInsertable() {
		return insertable;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isDirtyCheckable() {
		return dirtyCheckable;
	}

	public boolean isVersionable() {
		return versionable;
	}

	public CascadeStyle getCascadeStyle() {
		return cascadeStyle;
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public OnDeleteAction getOnDeleteAction() {
		return onDeleteAction;
	}

	public static class Builder {
		private boolean lazy;
		private boolean insertable;
		private boolean updateable;
		private boolean nullable;
		private boolean dirtyCheckable;
		private boolean versionable;
		private CascadeStyle cascadeStyle;
		private OnDeleteAction onDeleteAction;
		private FetchMode fetchMode;

		public Builder setLazy(boolean lazy) {
			this.lazy = lazy;
			return this;
		}

		public Builder setInsertable(boolean insertable) {
			this.insertable = insertable;
			return this;
		}

		public Builder setUpdateable(boolean updateable) {
			this.updateable = updateable;
			return this;
		}

		public Builder setNullable(boolean nullable) {
			this.nullable = nullable;
			return this;
		}

		public Builder setDirtyCheckable(boolean dirtyCheckable) {
			this.dirtyCheckable = dirtyCheckable;
			return this;
		}

		public Builder setVersionable(boolean versionable) {
			this.versionable = versionable;
			return this;
		}

		public Builder setCascadeStyle(CascadeStyle cascadeStyle) {
			this.cascadeStyle = cascadeStyle;
			return this;
		}

		public Builder setOnDeleteAction(OnDeleteAction onDeleteAction) {
			this.onDeleteAction = onDeleteAction;
			return this;
		}

		public Builder setFetchMode(FetchMode fetchMode) {
			this.fetchMode = fetchMode;
			return this;
		}

		public BaselineAttributeInformation createInformation() {
			return new BaselineAttributeInformation(
					lazy,
					insertable,
					updateable,
					nullable,
					dirtyCheckable,
					versionable,
					cascadeStyle,
					onDeleteAction,
					fetchMode
			);
		}
	}
}
