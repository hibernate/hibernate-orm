package org.hibernate.orm.test.caching;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

@Jpa(
		annotatedClasses = {
				CachingWithBatchAndFetchModeSelectAndSecondaryTableTest.CategoryReport.class,
				CachingWithBatchAndFetchModeSelectAndSecondaryTableTest.Category.class,
				CachingWithBatchAndFetchModeSelectAndSecondaryTableTest.CompositeCategory.class,
				CachingWithBatchAndFetchModeSelectAndSecondaryTableTest.GroupCategory.class,
				CachingWithBatchAndFetchModeSelectAndSecondaryTableTest.ClassCategory.class,
				CachingWithBatchAndFetchModeSelectAndSecondaryTableTest.SimpleCategory.class,
				CachingWithBatchAndFetchModeSelectAndSecondaryTableTest.DerivativeCategory.class
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "false"),
				@Setting(name = AvailableSettings.ISOLATION, value = "TRANSACTION_READ_COMMITTED")
		}
)
@JiraKey("HHH-17918")
public class CachingWithBatchAndFetchModeSelectAndSecondaryTableTest {

	@BeforeAll
	public void setupCacheEviction(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					// Create a bunch of entities of the various classes
					List<Category> categories = persist(entityManager, Category::new, 1111);
					List<GroupCategory> groupCategories = persist(entityManager, GroupCategory::new, 23);
					List<ClassCategory> classCategories = persist(entityManager, ClassCategory::new, groupCategories.size() * 10);
					List<SimpleCategory> simpleCategories = persist(entityManager, SimpleCategory::new, 2257);
					List<DerivativeCategory> derivativeCategories = persist(entityManager, DerivativeCategory::new, 147);

					// Create some circular links between these entities
					for (int i=0; i<classCategories.size(); i++) {
						GroupCategory groupCategory = groupCategories.get((i * 11) % groupCategories.size());

						classCategories.get(i).setGroupCategory(groupCategory);
						classCategories.get(i).setLeftCategory(simpleCategories.get((i * 19) % simpleCategories.size()));
						classCategories.get(i).setAncestorCategory(classCategories.get((i * 43) % classCategories.size()));

						// set a main category, last update wins
						groupCategory.setMainClassCategory(classCategories.get(i));
					}

					for (int i=0; i<groupCategories.size(); i++) {
						groupCategories.get(i).setLeftCategory(simpleCategories.get((i * 17) % simpleCategories.size()));
						groupCategories.get(i).setAncestorCategory(groupCategories.get((i * 57) % groupCategories.size()));
					}

					for (int i=0; i<derivativeCategories.size(); i++) {
						derivativeCategories.get(i).setSimpleCategory(simpleCategories.get((i * 13) % simpleCategories.size()));
					}

					// Create other Report entities linking to some of our Category entities
					List<Category> allCategories = new ArrayList<>(categories);
					allCategories.addAll(groupCategories);
					allCategories.addAll(classCategories);
					allCategories.addAll(simpleCategories);
					allCategories.addAll(derivativeCategories);

					Random random = new Random(4152);
					Collections.shuffle(allCategories, random);

					for (int i=0; i<allCategories.size(); i++) {
						if (random.nextFloat() > 0.85) {
							Category category = allCategories.get(i);
							CategoryReport report = new CategoryReport();

							report.setCategory(category);
							report.setClassCategory(classCategories.get((i * 113) % classCategories.size()));

							entityManager.persist(report);
						}
					}
				}
		);
	}

	private <C extends Category> List<C> persist(EntityManager entityManager, Supplier<C> supplier, int count) {
		List<C> values = new ArrayList<>();

		for (int i=0; i<count; i++) {
			C value = supplier.get();

			entityManager.persist(value);

			values.add(value);
		}

		return values;
	}

	@Test
	public void testCacheEviction(EntityManagerFactoryScope scope) {
		// Get all the categories in the 2nd level cache
		scope.inEntityManager(
				entityManager -> {
					List<Category> categories =
							entityManager.createQuery( "Select o from Category o", Category.class )
									.getResultList();
				}
		);

		// Remove "some" categories from the 2nd level cache, this does not fail for all random seeds
		Random random = new Random(50215);

		for (long i=0; i<3768; i++) {
			if (random.nextFloat() > 0.9) {
				scope.getEntityManagerFactory().getCache().evict(Category.class, i);
			}
		}

		// This fails with jakarta.persistence.EntityNotFoundException: Unable to find ...
		scope.inEntityManager(
				entityManager -> {
					List<CategoryReport> categories =
							entityManager.createQuery( "Select o from CategoryReport o", CategoryReport.class )
									.getResultList();
				}
		);
	}

	@Entity(name = "CategoryReport")
	public static class CategoryReport {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private GroupCategory groupCategory;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private ClassCategory classCategory;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private Category category;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public GroupCategory getGroupCategory() {
			return groupCategory;
		}

		public void setGroupCategory(GroupCategory groupCategory) {
			this.groupCategory = groupCategory;
		}

		public ClassCategory getClassCategory() {
			return classCategory;
		}

		public void setClassCategory(ClassCategory classCategory) {
			this.classCategory = classCategory;
		}

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category category) {
			this.category = category;
		}
	}

	@Entity(name = "Category")
	@Proxy(lazy = false)
	@BatchSize(size = 10)
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue("CATEGORY")
	public static class Category {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private Category parentCategory;

		public Category() {
		}

		public Category(String name) {
			this.name = name;
		}

		public Category(String name, Category parentCategory) {
			this.name = name;
			this.parentCategory = parentCategory;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Category getParentCategory() {
			return parentCategory;
		}
	}

	@Entity
	@BatchSize(size = 5)
	@Proxy(lazy = false)
	@DiscriminatorValue("COMPOSITE")
	public static class CompositeCategory extends Category {

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private Category leftCategory;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private CompositeCategory rightCategory;

		public CompositeCategory() {
		}

		public CompositeCategory(String name, Category parentCategory) {
			super(name, parentCategory);
		}

		public Category getLeftCategory() {
			return leftCategory;
		}

		public void setLeftCategory(Category leftCategory) {
			this.leftCategory = leftCategory;
		}

		public CompositeCategory getRightCategory() {
			return rightCategory;
		}

		public void setRightCategory(CompositeCategory rightCategory) {
			this.rightCategory = rightCategory;
		}
	}

	@Entity
	@BatchSize(size = 5)
	@Proxy(lazy = false)
	@DiscriminatorValue("GROUP")
	@SecondaryTable(name = "group_category", pkJoinColumns = { @PrimaryKeyJoinColumn(name = "id") })
	public static class GroupCategory extends CompositeCategory {
		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private ClassCategory mainClassCategory;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private GroupCategory ancestorCategory;

		@Column(table = "group_category")
		private String groupValue;

		public ClassCategory getMainClassCategory() {
			return mainClassCategory;
		}

		public void setMainClassCategory(ClassCategory mainClassCategory) {
			this.mainClassCategory = mainClassCategory;
		}

		public GroupCategory getAncestorCategory() {
			return ancestorCategory;
		}

		public void setAncestorCategory(GroupCategory ancestorCategory) {
			this.ancestorCategory = ancestorCategory;
		}

		public String getGroupValue() {
			return groupValue;
		}

		public void setGroupValue(String groupValue) {
			this.groupValue = groupValue;
		}
	}

	@Entity
	@BatchSize(size = 5)
	@Proxy(lazy = false)
	@DiscriminatorValue("CLASS")
	@SecondaryTable(name = "class_category", pkJoinColumns = { @PrimaryKeyJoinColumn(name = "id") })
	public static class ClassCategory extends CompositeCategory {

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private GroupCategory groupCategory;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private ClassCategory ancestorCategory;

		@Column(table = "class_category")
		private String classValue;

		public GroupCategory getGroupCategory() {
			return groupCategory;
		}

		public void setGroupCategory(GroupCategory groupCategory) {
			this.groupCategory = groupCategory;
		}

		public ClassCategory getAncestorCategory() {
			return ancestorCategory;
		}

		public void setAncestorCategory(ClassCategory ancestorCategory) {
			this.ancestorCategory = ancestorCategory;
		}

		public String getClassValue() {
			return classValue;
		}

		public void setClassValue(String classValue) {
			this.classValue = classValue;
		}
	}

	@Entity
	@BatchSize(size = 5)
	@Proxy(lazy = false)
	@DiscriminatorValue("SIMPLE")
	public static class SimpleCategory extends Category {
	}

	@Entity
	@BatchSize(size = 25)
	@Proxy(lazy = false)
	@DiscriminatorValue("DERIVATIVE")
	public static class DerivativeCategory extends Category {

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(value = FetchMode.SELECT)
		private SimpleCategory simpleCategory;

		public SimpleCategory getSimpleCategory() {
			return simpleCategory;
		}

		public void setSimpleCategory(SimpleCategory simpleCategory) {
			this.simpleCategory = simpleCategory;
		}
	}
}
