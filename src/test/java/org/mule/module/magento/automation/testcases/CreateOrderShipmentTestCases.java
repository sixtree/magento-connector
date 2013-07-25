/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.md file.
 */

package org.mule.module.magento.automation.testcases;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

import com.magento.api.CatalogProductCreateEntity;
import com.magento.api.OrderItemIdQty;
import com.magento.api.ShoppingCartCustomerAddressEntity;
import com.magento.api.ShoppingCartCustomerEntity;
import com.magento.api.ShoppingCartPaymentMethodEntity;
import com.magento.api.ShoppingCartProductEntity;

public class CreateOrderShipmentTestCases extends MagentoTestParent {


	@SuppressWarnings("unchecked")
	@Before
	public void setUp() {
		try {
			testObjects = (HashMap<String, Object>) context.getBean("createOrderShipment");
			
			ShoppingCartCustomerEntity customer = (ShoppingCartCustomerEntity) testObjects.get("customer");
			List<ShoppingCartCustomerAddressEntity> addresses = (List<ShoppingCartCustomerAddressEntity>) testObjects.get("customerAddresses");
			String shippingMethod = testObjects.get("shippingMethod").toString();
			ShoppingCartPaymentMethodEntity paymentMethod = (ShoppingCartPaymentMethodEntity) testObjects.get("paymentMethod");
			
			List<HashMap<String, Object>> products = (List<HashMap<String, Object>>) testObjects.get("products");
			List<ShoppingCartProductEntity> shoppingCartProducts = new ArrayList<ShoppingCartProductEntity>();
			List<Integer> productIds = new ArrayList<Integer>();
			
			for (HashMap<String, Object> product : products) {
				
				// Get the product data
				String productType = (String) product.get("type");
				int productSet = (Integer) product.get("set");
				String productSKU = (String) product.get("sku");
				CatalogProductCreateEntity attributes = (CatalogProductCreateEntity) product.get("attributesRef");
			
				// Create the product and get the product ID
				int productId = createProduct(productType, productSet, productSKU, attributes);
				
				// Get the quantity to place in the shopping cart
				double qtyToPurchase = (Double) product.get("qtyToPurchase");

				// Create the shopping cart product entity
				ShoppingCartProductEntity shoppingCartProduct = new ShoppingCartProductEntity();
				shoppingCartProduct.setProduct_id(productId + "");
				shoppingCartProduct.setQty(qtyToPurchase);
				
				shoppingCartProducts.add(shoppingCartProduct);
				productIds.add(productId);
			}
			testObjects.put("productIds", productIds);
			testObjects.put("shoppingCartProducts", shoppingCartProducts);

			String storeId = testObjects.get("storeId").toString();
			int quoteId = createShoppingCart(storeId);
			
			String orderId = createShoppingCartOrder(quoteId, customer, addresses, paymentMethod, shippingMethod, shoppingCartProducts);
			testObjects.put("orderId", orderId);
			
		}
		catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}	
	
	@SuppressWarnings("unchecked")
	@Category({SmokeTests.class, RegressionTests.class})
	@Test
	public void testCreateOrderShipment() {
		try {
			List<ShoppingCartProductEntity> shoppingCartProducts = (List<ShoppingCartProductEntity>) testObjects.get("shoppingCartProducts");
			
			// Send empty list so that Magento defaults to shipping everything that was placed in the order
			// This is probably a bug on Magento's end
			List<OrderItemIdQty> quantities = new ArrayList<OrderItemIdQty>();
			
//			for (ShoppingCartProductEntity shoppingCartProduct : shoppingCartProducts) {
//				OrderItemIdQty item = new OrderItemIdQty(Integer.parseInt(shoppingCartProduct.getProduct_id()), 10d);
//				quantities.add(item);
//			}
//			
			testObjects.put("itemsQuantitiesRef", quantities);
			
			MessageProcessor flow = lookupFlowConstruct("create-order-shipment");
			MuleEvent response = flow.process(getTestEvent(testObjects));
			
			String shipmentId = (String) response.getMessage().getPayload();
			assertNotNull(shipmentId);
			assertTrue(shipmentId.length() > 0);
			assertTrue(shipmentId.trim().length() > 0);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@SuppressWarnings("unchecked")
	@After
	public void tearDown() {
		try {
			List<Integer> productIds = (List<Integer>) testObjects.get("productIds");
			for (Integer productId : productIds) {
				deleteProductById(productId);
			}	
			clearSalesTables();
		}
		catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
}