package org.hibernate.test.batchfetch;

import java.sql.Blob;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class BatchFetchBootstrapTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Authority.class, JafSid.class, UserGroup.class, File.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "30");
	}

	@Override
	protected void buildSessionFactory() {
	}

	@Test
	@FailureExpected( jiraKey = "HHH-12594")
	public void test() {
		super.buildSessionFactory();
	}

	@Entity(name = "File")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
	public static class File extends Base {

		private Blob blob;
		private Base parent;

		@Column(name = "filedata", length = 1024 * 1024)
		@Lob
		@Basic(fetch = FetchType.LAZY)
		public Blob getBlob() {
			return blob;
		}

		public void setBlob(Blob blob) {
			this.blob = blob;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public Base getParent() {
			return parent;
		}

		public void setParent(Base parent) {
			this.parent = parent;
		}
	}

	@MappedSuperclass
	public abstract static class DatabaseEntity {
		private int id;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

	}

	@Entity(name = "Base")
	@Polymorphism(type = PolymorphismType.EXPLICIT)
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class Base extends DatabaseEntity {

		private Set<File> files;

		@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
		@Fetch(FetchMode.SUBSELECT)
		public Set<File> getFiles() {
			return files;
		}

		public void setFiles(Set<File> files) {
			this.files = files;
		}
	}

	@Entity(name = "Authority")
	public static class Authority extends SidEntity {
		private String authority;

		public String getAuthority() {
			return authority;
		}

		public void setAuthority(String authority) {
			this.authority = authority;
		}
	}

	@Entity(name = "JafSid")
	public static class JafSid extends Base {

		private Set<UserGroup> groups = new LinkedHashSet<>();
		private SidEntity relatedEntity;
		private String sid;

		@ManyToMany(mappedBy = "members", fetch = FetchType.EAGER)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		public Set<UserGroup> getGroups() {
			return groups;
		}

		public void setGroups(Set<UserGroup> groups) {
			this.groups = groups;
		}

		@OneToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		public SidEntity getRelatedEntity() {
			return relatedEntity;
		}

		public void setRelatedEntity(SidEntity relatedEntity) {
			this.relatedEntity = relatedEntity;
		}

		public String getSid() {
			return sid;
		}

		public void setSid(String sid) {
			this.sid = sid;
		}
	}

	@Entity(name = "SidEntity")
	public static class SidEntity extends Base {

		private JafSid sid;

		@OneToOne(mappedBy = "relatedEntity", optional = false, fetch = FetchType.EAGER, orphanRemoval = true)
		@Cascade(CascadeType.ALL)
		public JafSid getSid() {
			return sid;
		}

		public void setSid(JafSid sid) {
			this.sid = sid;
		}
	}

	@Entity(name = "User")
	public static class User extends SidEntity {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "UserGroup")
	public static class UserGroup extends SidEntity {

		private Set<Authority> authorities = new LinkedHashSet<>();
		private Set<JafSid> members = new LinkedHashSet<>();

		@ManyToMany(targetEntity = Authority.class, fetch = FetchType.LAZY)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		public Set<Authority> getAuthorities() {
			return authorities;
		}

		public void setAuthorities(Set<Authority> authorities) {
			this.authorities = authorities;
		}

		@ManyToMany(fetch = FetchType.LAZY)
		@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
		public Set<JafSid> getMembers() {
			return members;
		}

		public void setMembers(Set<JafSid> members) {
			this.members = members;
		}
	}
}
