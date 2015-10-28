/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.filter.Converter;
import org.infinispan.filter.KeyValueFilter;
import org.infinispan.metadata.Metadata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class VersionedEntry {
	public static final ExcludeEmptyFilter EXCLUDE_EMPTY_EXTRACT_VALUE = new ExcludeEmptyFilter();
	private final Object value;
	private final Object version;
	private final long timestamp;

	public VersionedEntry(Object value, Object version, long timestamp) {
		this.value = value;
		this.version = version;
		this.timestamp = timestamp;
	}

	public Object getValue() {
		return value;
	}

	public Object getVersion() {
		return version;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("VersionedEntry{");
		sb.append("value=").append(value);
		sb.append(", version=").append(version);
		sb.append(", timestamp=").append(timestamp);
		sb.append('}');
		return sb.toString();
	}

	private static class ExcludeEmptyFilter implements KeyValueFilter<Object, Object>, Converter<Object, Object, Object> {
		@Override
		public boolean accept(Object key, Object value, Metadata metadata) {
			if (value instanceof VersionedEntry) {
				return ((VersionedEntry) value).getValue() != null;
			}
			return true;
		}

		@Override
		public Object convert(Object key, Object value, Metadata metadata) {
			if (value instanceof VersionedEntry) {
				return ((VersionedEntry) value).getValue();
			}
			return value;
		}
	}

	public static class Externalizer implements AdvancedExternalizer<VersionedEntry> {
		@Override
		public Set<Class<? extends VersionedEntry>> getTypeClasses() {
			return Collections.<Class<? extends VersionedEntry>>singleton(VersionedEntry.class);
		}

		@Override
		public Integer getId() {
			return Externalizers.VERSIONED_ENTRY;
		}

		@Override
		public void writeObject(ObjectOutput output, VersionedEntry object) throws IOException {
			output.writeObject(object.value);
			output.writeObject(object.version);
			output.writeLong(object.timestamp);
		}

		@Override
		public VersionedEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			Object value = input.readObject();
			Object version = input.readObject();
			long timestamp = input.readLong();
			return new VersionedEntry(value, version, timestamp);
		}
	}

	public static class ExcludeEmptyExtractValueExternalizer implements AdvancedExternalizer<ExcludeEmptyFilter> {
		@Override
		public Set<Class<? extends ExcludeEmptyFilter>> getTypeClasses() {
			return Collections.<Class<? extends ExcludeEmptyFilter>>singleton(ExcludeEmptyFilter.class);
		}

		@Override
		public Integer getId() {
			return Externalizers.EXCLUDE_EMPTY_EXTRACT_VALUE;
		}

		@Override
		public void writeObject(ObjectOutput output, ExcludeEmptyFilter object) throws IOException {
		}

		@Override
		public ExcludeEmptyFilter readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			return EXCLUDE_EMPTY_EXTRACT_VALUE;
		}
	}
}
