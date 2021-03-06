package com.cfckata.sample.sales;

import com.cfckata.sample.sales.domain.SalesOrder;
import com.cfckata.sample.sales.domain.OrderItem;
import com.cfckata.sample.sales.domain.OrderStatus;
import com.cfckata.sample.sales.proxy.InsufficientBalanceException;
import com.cfckata.sample.sales.proxy.PayProxy;
import com.cfckata.sample.sales.proxy.TimeoutException;
import com.cfckata.sample.sales.request.CheckoutRequest;
import com.cfckata.sample.product.Product;
import com.github.meixuesong.aggregatepersistence.AggregateFactory;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SalesOrderServiceTest {
    private OrderService service;
    private OrderRepository orderRepository;
    private PayProxy payProxy;

    private SalesOrder testOrder;
    private String orderId;
    private BigDecimal totalPrice;

    @Before
    public void setUp() throws Exception {
        orderRepository = mock(OrderRepository.class);
        payProxy = mock(PayProxy.class);
        orderId = "orderid";
        totalPrice = new BigDecimal("1000");
        testOrder = createNormalTestOrder(orderId, totalPrice);

        service = new OrderService(orderRepository, null, payProxy);

        when(orderRepository.findById(same(orderId))).thenReturn(AggregateFactory.createAggregate(testOrder));
        doNothing().when(orderRepository).save(any());
    }

    @Test
    public void should_pay_success_when_checkout_a_normal_order() {
        //Given
        doNothing().when(payProxy).pay(anyString(), any());

        //When
        service.checkout(orderId, new CheckoutRequest("CASH", totalPrice));

        //THEN
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test(expected = InsufficientBalanceException.class)
    public void should_failed_to_pay_when_balance_insufficient() {
        when(orderRepository.findById(same(orderId))).thenReturn(AggregateFactory.createAggregate(testOrder));
        doThrow(new InsufficientBalanceException()).when(payProxy).pay(anyString(), any());

        //When
        service.checkout(orderId, new CheckoutRequest("CASH", totalPrice));
    }

    @Test(expected = TimeoutException.class)
    public void should_failed_to_pay_when_external_pay_server_timeout() {
        when(orderRepository.findById(same(orderId))).thenReturn(AggregateFactory.createAggregate(testOrder));
        doThrow(new TimeoutException()).when(payProxy).pay(anyString(), any());

        //When
        service.checkout(orderId, new CheckoutRequest("CASH", totalPrice));
    }

    private SalesOrder createNormalTestOrder(String orderid, BigDecimal totalPrice) {
        BigDecimal amount = new BigDecimal("1");
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(orderid);
        salesOrder.setCreateTime(new Date());
        salesOrder.setCustomerId("customerId");
        salesOrder.setStatus(OrderStatus.NEW);
        salesOrder.setTotalPayment(totalPrice);
        ArrayList<OrderItem> items = new ArrayList<>();
        final Product product = new Product("product1", "product1", totalPrice);
        items.add(new OrderItem(1L, amount, product.getPrice(), product.getId(), product.getName()));
        salesOrder.setItems(items);

        return salesOrder;
    }

    @Test
    public void name() {
        assertThat(String.format("product %s not exists", "AA")).isEqualTo("product AA not exists");
    }
}
