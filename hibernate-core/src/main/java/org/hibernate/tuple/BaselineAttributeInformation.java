/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple;

import org.hibernate.FetchMode;
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
			FetchMode fetchMode) {
		this.lazy = lazy;
		this.insertable = insertable;
		this.updateable = updateable;
		this.nullable = nullable;
		this.dirtyCheckable = dirtyCheckable;
		this.versionable = versionable;
		this.cascadeStyle = cascadeStyle;
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

	public static class Builder {
		private boolean lazy;
		private boolean insertable;
		private boolean updateable;
		private boolean nullable;
		private boolean dirtyCheckable;
		private boolean versionable;
		private CascadeStyle cascadeStyle;
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
					fetchMode
			);
		}
	}
}
