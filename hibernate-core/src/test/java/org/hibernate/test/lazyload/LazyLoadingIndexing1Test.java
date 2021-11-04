package org.hibernate.test.lazyload;

import org.hibernate.LazyInitializationException;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.*;

@TestForIssue(jiraKey = "HHH-14839")
public class LazyLoadingIndexing1Test extends BaseEntityManagerFunctionalTestCase {

    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{
                BusinessEntity.class,
                SalesOrder.class,
                SalesOrderDetail.class,
                Item.class,
                ItemVendorInfo.class,
                SerialNumber.class,
                Vendor.class,
                Manufacturer.class,
                ItemText.class
        };
    }

    @Test
    public void testLazyLoadingAfterDetachedPersistOrMerge() {

        // add vendor, manufacturer, and item
        doInJPA(this::entityManagerFactory, entityManager -> {

            Vendor vendor = new Vendor(1L, "Distributor");
            entityManager.persist(vendor);

            Manufacturer manufacturer = new Manufacturer(1L, "Manufacturer");
            entityManager.persist(manufacturer);

            Item item = new Item(1L, "New Item");
            item.setManufacturer(manufacturer);
            entityManager.persist(item);
        });

        // add item vendor info with all ToOne references detached
        doInJPA(this::entityManagerFactory, entityManager -> {

            Manufacturer manufacturer = new Manufacturer(1L);
            Vendor vendor = new Vendor(1L);
            Item item = new Item(1L);
            item.setManufacturer(manufacturer);

            ItemVendorInfo itemVendorInfo = new ItemVendorInfo(1L, item, vendor, new BigDecimal("2000"));
            entityManager.persist(itemVendorInfo);
            entityManager.flush();

            assertThat(itemVendorInfo.getItem().getManufacturer().getName()).matches("Manufacturer");
            assertThat(itemVendorInfo.getItem().getManufacturer().getItems()).hasSize(1);

            verifyReachableByIndexing(itemVendorInfo.getItem(), itemVendorInfo.getVendor(), 1, 1);

        });

        // update detached item
        {
            Item detachedItem;

            detachedItem = doInJPA(this::entityManagerFactory, entityManager -> {
                return entityManager.find(Item.class, 1L);
            });

            assertProxyState(detachedItem);

            doInJPA(this::entityManagerFactory, entityManager -> {

                Manufacturer manufacturer = new Manufacturer(1L);  // simulate detached manufacturer

                Item i = new Item(1L);
                i.setManufacturer(manufacturer);
                i.setName("Item 1 New Name");
                i.setVersion(detachedItem.getVersion());

                int version = i.getVersion();
                i = entityManager.merge(i);
                entityManager.flush();

                assertThat(i.getVendorInfos()).hasSize(1);

                manufacturer = i.getManufacturer();
                assertThat(manufacturer.getName()).matches("Manufacturer");
                assertThat(manufacturer.getItems()).hasSize(1);

                assertThat(i.getVersion()).isEqualTo(version + 1);

                entityManager.refresh(i);

                assertThat(i.getName()).matches("Item 1 New Name");
                assertThat(i.getVersion()).isEqualTo(version + 1);

            });

        }

        // add another item with vendor info, and make sure information from previous transaction is still there
        doInJPA(this::entityManagerFactory, entityManager -> {

            Manufacturer manufacturer1 = entityManager.find(Manufacturer.class, 1L);
            Item item2 = new Item(2L, "New Item 2");
            item2.setManufacturer(manufacturer1);
            item2.setVersion(0);

            entityManager.persist(item2);

            Vendor vendor1 = entityManager.find(Vendor.class, 1L);
            ItemVendorInfo itemVendorInfo2 = new ItemVendorInfo(2L, item2, vendor1, new BigDecimal("2000"));
            entityManager.persist(itemVendorInfo2);
            entityManager.flush();

            verifyReachableByIndexing(item2, itemVendorInfo2.getVendor(), 1, 2);
        });


        // also check manufacturer --> items is accumulating
        doInJPA(this::entityManagerFactory, entityManager -> {

            Manufacturer manufacturer1 = new Manufacturer(1L);
            Item item3 = new Item(3L, "New Item 3");
            item3.setManufacturer(manufacturer1);
            item3.setVersion(0);

            entityManager.persist(item3);

            Vendor vendor1 = new Vendor(1L);
            ItemVendorInfo itemVendorInfo3 = new ItemVendorInfo(3L, item3, vendor1, new BigDecimal("2000"));
            entityManager.persist(itemVendorInfo3);
            entityManager.flush();

            Set<ItemVendorInfo> vi = item3.getVendorInfos();
            assertThat(vi).hasSize(1);

            assertThat(item3.getManufacturer().getName()).matches("Manufacturer");
            assertThat(item3.getManufacturer().getItems()).hasSize(3);

            verifyReachableByIndexing(item3, itemVendorInfo3.getVendor(), 1, 3);

        });

        // test ToMany on the other side of a ToOne
        doInJPA(this::entityManagerFactory, entityManager -> {

            SalesOrder salesOrder = new SalesOrder(1L);
            entityManager.persist(salesOrder);

            Item Item1 = new Item(1L);
            Item1.setVersion(1);

            SalesOrderDetail salesOrderDetail = new SalesOrderDetail(1L, salesOrder, Item1);
            entityManager.persist(salesOrderDetail);
            entityManager.flush();

            assertThat(salesOrderDetail.getItem().getSalesOrderDetails()).hasSize(1);

        });

        // test ToMany --> ToOne from entity persisted
        doInJPA(this::entityManagerFactory, entityManager -> {

            SalesOrderDetail salesOrderDetail = new SalesOrderDetail(1L);

            SerialNumber serialNumber = new SerialNumber(1L, "1", salesOrderDetail);
            entityManager.persist(serialNumber);
            entityManager.flush();

            assertNotNull(serialNumber.getSalesOrderDetail().getSalesOrder());
        });


        // test ToMany --> ToOne from entity removed
        doInJPA(this::entityManagerFactory, entityManager -> {

            SerialNumber serialNumber = entityManager.find(SerialNumber.class, 1L);
            entityManager.remove(serialNumber);
            assertNotNull(serialNumber.getSalesOrderDetail().getSalesOrder());
        });


        // test ToOne --> ToMany from entity persisted
        doInJPA(this::entityManagerFactory, entityManager -> {

            Item Item1 = new Item(1L);
            Item1.setVersion(1);

            Vendor vendor1 = new Vendor(1L);

            ItemVendorInfo itemVendorInfo4 = new ItemVendorInfo(4L, Item1, vendor1, new BigDecimal("2000"));
            entityManager.persist(itemVendorInfo4);
            entityManager.flush();

            Set<ItemVendorInfo> vi = itemVendorInfo4.getItem().getVendorInfos();
            assertThat(vi).hasSize(2);

        });

        // add an entity to remove in the next test, check reachability
        doInJPA(this::entityManagerFactory, entityManager -> {

            Item item10 = new Item(10L);
            entityManager.persist(item10);
            entityManager.flush();

            Vendor vendor1 = new Vendor(1L);

            ItemVendorInfo itemVendorInfo5 = new ItemVendorInfo(5L, item10, vendor1, new BigDecimal("2000"));
            entityManager.persist(itemVendorInfo5);
            entityManager.flush();

            verifyReachableByIndexing(itemVendorInfo5.getItem(), itemVendorInfo5.getVendor(), 1, 5);

        });

        // remove entity and check reachability
        doInJPA(this::entityManagerFactory, entityManager -> {
            ItemVendorInfo itemVendorInfo5 = entityManager.find(ItemVendorInfo.class, 5L);
            entityManager.remove(itemVendorInfo5);
            entityManager.flush();

            verifyReachableByIndexing(itemVendorInfo5.getItem(), itemVendorInfo5.getVendor(), 0, 4);
        });


        // add an item to merge in the next test
        doInJPA(this::entityManagerFactory, entityManager -> {
            Item i = new Item(11L);
            entityManager.persist(i);
        });

        // check merged values after a refresh
        doInJPA(this::entityManagerFactory, entityManager -> {

            Item i = new Item(11L);

            Manufacturer manufacturer = new Manufacturer(1L);
            i.setManufacturer(manufacturer); // simulate detached manufacturer
            i.setName("Item 11 Test update with lazy init collection");
            i = entityManager.merge(i);
            entityManager.flush();

            entityManager.refresh(i);
            assertThat(i.getName()).isEqualTo("Item 11 Test update with lazy init collection");
        });
    }

    private void verifyReachableByIndexing(Item item, Vendor vendor, int infoByItemSize, int infoByVendorSize) {
        assertThat(item.getVendorInfos()).hasSize(infoByItemSize);
        assertThat(vendor.getItemVendorInfos()).hasSize(infoByVendorSize);

        if (item.getVendorInfos().size() > 0) {
            ItemVendorInfo vendorInfo = (ItemVendorInfo) item.getVendorInfos().toArray()[0];
            assertThat(vendorInfo.getVendor().getId()).isEqualTo(vendor.getId());
        }
    }

    protected void assertProxyState(Item item) {
        try {
            item.getManufacturer().getName();
            fail("Should throw LazyInitializationException!");
        } catch (LazyInitializationException expected) {

        }

        try {
            item.getVendorInfos().size();
            fail("Should throw LazyInitializationException!");
        } catch (LazyInitializationException expected) {

        }
    }

    @MappedSuperclass
    public static class BusinessEntity {

        private Long id;
        private int version;

        public BusinessEntity() {
            version = 0;
        }

        public BusinessEntity(Long id) {
            this.id = id;
            version = 0;
        }

        @Id
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Version
        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }
    }

    @Entity
    public static class SalesOrder extends BusinessEntity {

        public SalesOrder() {
        }

        public SalesOrder(Long id) {
            super(id);
        }

        private Set<SalesOrderDetail> salesOrderDetails;

        @OneToMany(mappedBy = "salesOrder")
        public Set<SalesOrderDetail> getSalesOrderDetails() {
            return this.salesOrderDetails;
        }

        public void setSalesOrderDetails(Set<SalesOrderDetail> SalesOrderDetails) {
            this.salesOrderDetails = SalesOrderDetails;
        }

    }

    @Entity
    public static class SalesOrderDetail extends BusinessEntity {

        Item item;
        SalesOrder salesOrder;

        public SalesOrderDetail() {

        }

        public SalesOrderDetail(Long id) {
            super(id);
        }

        public SalesOrderDetail(Long id, SalesOrder salesOrder, Item item) {
            super(id);
            this.salesOrder = salesOrder;
            this.item = item;
        }

        private Set<SerialNumber> serialNumbers;

        @ManyToOne(fetch = FetchType.LAZY)
        public SalesOrder getSalesOrder() {
            return this.salesOrder;
        }

        public void setSalesOrder(SalesOrder salesOrder) {
            this.salesOrder = salesOrder;
        }


        @ManyToOne(fetch = FetchType.LAZY)
        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        @OneToMany(mappedBy = "salesOrderDetail")
        public Set<SerialNumber> getSerialNumbers() {
            return serialNumbers;
        }

        public void setSerialNumbers(Set<SerialNumber> serialNumbers) {
            this.serialNumbers = serialNumbers;
        }

    }

    @Entity
    public static class Item extends BusinessEntity {

        private String name;
        private Manufacturer manufacturer;
        private Set<ItemVendorInfo> vendorInfos;

        protected Item() {
        }

        public Item(Long id, String name) {
            super(id);
            this.name = name;
        }

        public Item(long id) {
            super(id);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @OneToMany(mappedBy = "item", targetEntity = ItemVendorInfo.class)
        public Set<ItemVendorInfo> getVendorInfos() {
            return this.vendorInfos;
        }

        public void setVendorInfos(Set<ItemVendorInfo> vendorInfo) {
            this.vendorInfos = vendorInfo;
        }

        @ManyToOne(fetch = FetchType.LAZY)
        public Manufacturer getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(Manufacturer manufacturer) {
            this.manufacturer = manufacturer;
        }

        private Set<SalesOrderDetail> salesOrderDetails;

        @OneToMany(mappedBy = "item")
        public Set<SalesOrderDetail> getSalesOrderDetails() {
            return salesOrderDetails;
        }

        private Set<ItemText> itemTexts;

        public void setSalesOrderDetails(Set<SalesOrderDetail> salesOrderDetails) {
            this.salesOrderDetails = salesOrderDetails;
        }

        @OneToMany(mappedBy = "item")
        public Set<ItemText> getItemTexts() {
            return this.itemTexts;
        }

        public void setItemTexts(Set<ItemText> itemTexts) {
            this.itemTexts = itemTexts;
        }
    }


    @Entity
    public static class ItemVendorInfo extends BusinessEntity {

        private Item item;
        private Vendor vendor;
        private BigDecimal cost;

        protected ItemVendorInfo() {
        }

        public ItemVendorInfo(Long id, Item item, Vendor vendor, BigDecimal cost) {
            super(id);
            this.item = item;
            this.vendor = vendor;
            this.cost = cost;
        }

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false)
        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false)
        public Vendor getVendor() {
            return this.vendor;
        }

        public void setVendor(Vendor Vendor) {
            this.vendor = Vendor;
        }

        public BigDecimal getCost() {
            return cost;
        }

        public void setCost(BigDecimal cost) {
            this.cost = cost;
        }
    }

    @Entity
    public static class SerialNumber extends BusinessEntity {
        private Item item;
        private String serialNumber;
        private SalesOrderDetail salesOrderDetail;

        public SerialNumber() {

        }

        public SerialNumber(Long id) {
            super(id);
        }

        public SerialNumber(Long id, String serialNumber, SalesOrderDetail salesOrderDetail) {
            super(id);
            this.serialNumber = serialNumber;
            this.salesOrderDetail = salesOrderDetail;
        }

        @ManyToOne(fetch = FetchType.LAZY)
        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }

        @ManyToOne(fetch = FetchType.LAZY)
        public SalesOrderDetail getSalesOrderDetail() {
            return salesOrderDetail;
        }

        public void setSalesOrderDetail(SalesOrderDetail salesOrderDetail) {
            this.salesOrderDetail = salesOrderDetail;
        }

    }

    @Entity
    public static class Vendor extends BusinessEntity {

        private String name;
        private Set<ItemVendorInfo> itemVendorInfos;

        protected Vendor() {
        }

        public Vendor(Long id, String name) {
            super(id);
            this.name = name;
        }

        public Vendor(long id) {
            super(id);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @OneToMany(mappedBy = "vendor")
        public Set<ItemVendorInfo> getItemVendorInfos() {
            return itemVendorInfos;
        }

        public void setItemVendorInfos(Set<ItemVendorInfo> itemVendorInfos) {
            this.itemVendorInfos = itemVendorInfos;
        }

    }

    @Entity
    public static class Manufacturer extends BusinessEntity {

        private String name;

        public Manufacturer() {

        }

        public Manufacturer(Long id, String name) {
            super(id);
            this.name = name;
        }

        public Manufacturer(long id) {
            super(id);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private Collection<Item> items = new ArrayList<>();

        @OneToMany(mappedBy = "manufacturer")
        public Collection<Item> getItems() {
            return items;
        }

        public void setItems(Collection<Item> items) {
            this.items = items;
        }
    }

    @Entity
    public static class ItemText extends BusinessEntity {
        private Item item;

        public ItemText() {
        }

        public ItemText(long id) {
            super(id);
        }

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "PARENT_ID")
        public Item getItem() {
            return this.item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

    }
}
