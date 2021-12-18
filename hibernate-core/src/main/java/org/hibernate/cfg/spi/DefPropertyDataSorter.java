package org.hibernate.cfg.spi;

import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.PropertyData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 默认排序规则，id排序第一位其他按字段申明顺序<br>
 * 默认排序规则没有处理继承问题，仅做占位使用<br>
 * 建议自定义排序策略 例如使用 org.springframework.core.annotation.Order<br>
 * Default sort rule, id sort first other by field declaration order<br>
 * The default collation does not handle inheritance and is used only as a placeholder<br>
 * You are advised to customize a sorting policy<br>
 */
public class DefPropertyDataSorter implements  PropertyDataSorter  {

    @Override
    public boolean support(XClass clazz) {
        return true;
    }

    @Override
    public int compareTo(PropertyDataSorter o) {
        return 1;
    }
    static class Item implements Comparable<Item>{
        int val;
        PropertyData data;

        public Item(int val, PropertyData data) {
            super();
            this.val = val;
            this.data = data;
        }
        @Override
        public int compareTo(Item o) {
            if(val==o.val) {
                return data.getPropertyName().compareTo(o.data.getPropertyName());
            }
            return val-o.val;
        }
        @Override
        public String toString() {
            return "[" + val + ":" + data.getPropertyName()+"]";
        }

    }
    @Override
    public List<PropertyData> sort(List<PropertyData> elements) {
        List<Item> items=new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            PropertyData element=elements.get(i);
            XProperty property = element.getProperty();
            int val =300+i;
            if("id".equalsIgnoreCase(property.getName())){
                val=-1000+i;
            }
            items.add(new Item(val, element)) ;
        }
        items.sort(Item::compareTo);
        return  items.stream().map(v->v.data).collect(Collectors.toList());
    }

}
