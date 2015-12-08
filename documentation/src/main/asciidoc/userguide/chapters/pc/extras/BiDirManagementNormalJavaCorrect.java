Order order = new Order();
LineItem lineItem = new LineItem();
order.getLineItems().add( lineItem );
lineItem.setOrder( order );

// Now this is OK...
lineItem.getOrder.getname();