
package edu.itc.se.shop.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.se.shop.domain.Order;
import edu.itc.se.shop.domain.OrderLine;
import edu.itc.se.shop.domain.OrderStatus;
import edu.itc.se.shop.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ReportService {

    private final OrderRepository orders;

    @PersistenceContext
    private EntityManager entityManager;

    public ReportService(OrderRepository orders) {
        this.orders = orders;
    }

    // 1. Naive approach: disable batch fetching
    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrder(Long customerId) {

        Session session = entityManager.unwrap(Session.class);
        session.setFetchBatchSize(0);

        System.out.println(
                "NAIVE SESSION FETCH BATCH SIZE = "
                + session.getFetchBatchSize());

        return orders.findByStatusAndCustomer_Id(
                OrderStatus.PAID, customerId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        Order::getId,
                        order -> order.getLines().size(),
                        (left, right) -> right,
                        LinkedHashMap::new));
    }

    // 2. JOIN FETCH approach
    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderWithFetchJoin(Long customerId) {

        return orders.findByStatusWithLines(
                OrderStatus.PAID, customerId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        Order::getId,
                        order -> order.getLines().size(),
                        (left, right) -> right,
                        LinkedHashMap::new));
    }

    // 3. EntityGraph approach
    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderWithEntityGraph(Long customerId) {

        return orders.findWithGraphByStatusAndCustomer_Id(
                OrderStatus.PAID, customerId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        Order::getId,
                        order -> order.getLines().size(),
                        (left, right) -> right,
                        LinkedHashMap::new));
    }

    // 4. Batch fetching approach
    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderWithBatchSize(Long customerId) {

        Session session = entityManager.unwrap(Session.class);
        session.setFetchBatchSize(16);

        System.out.println(
                "BATCH SESSION FETCH BATCH SIZE = "
                + session.getFetchBatchSize());

        List<Order> paidOrders = orders.findByStatusAndCustomer_Id(
                OrderStatus.PAID, customerId);

        Map<Long, Integer> result = new LinkedHashMap<>();

        for (Order order : paidOrders) {
            result.put(order.getId(), order.getLines().size());
        }

        return result;
    }

    // 5. Product-name N+1 problem
    @Transactional(readOnly = true)
    public List<String> productNamesPerPaidOrder(Long customerId) {

        Session session = entityManager.unwrap(Session.class);
        session.setFetchBatchSize(0);

        System.out.println(
                "PRODUCT NAIVE SESSION FETCH BATCH SIZE = "
                + session.getFetchBatchSize());

        List<Order> paidOrders = orders.findByStatusWithLinesOnly(
                OrderStatus.PAID, customerId);

        List<String> productNames = new ArrayList<>();

        for (Order order : paidOrders) {
            for (OrderLine line : order.getLines()) {
                productNames.add(line.getProduct().getName());
            }
        }

        return productNames;
    }

    // 6. Product-name N+1 solution using batch fetching
    @Transactional(readOnly = true)
    public List<String> productNamesPerPaidOrderWithBatchSize(
            Long customerId) {

        Session session = entityManager.unwrap(Session.class);
        session.setFetchBatchSize(32);

        System.out.println(
                "PRODUCT BATCH SESSION FETCH BATCH SIZE = "
                + session.getFetchBatchSize());

        List<Order> paidOrders = orders.findByStatusWithLinesOnly(
                OrderStatus.PAID, customerId);

        List<String> productNames = new ArrayList<>();

        for (Order order : paidOrders) {
            for (OrderLine line : order.getLines()) {
                productNames.add(line.getProduct().getName());
            }
        }

        return productNames;
    }
}
